# Tech Challenge - OS Service (Ordem de Servico)

Microsservico responsavel pelo gerenciamento de Ordens de Servico (OS) - Fase 2 do Tech Challenge.

## Responsabilidades

- Criar/abrir OS
- Atualizar status da OS
- Consultar historico (auditoria)

## Stack

- **Linguagem:** Java 21 (Spring Boot 3.4)
- **Banco SQL:** PostgreSQL (dados transacionais da OS)
- **Banco NoSQL:** MongoDB (historico/auditoria de eventos)
- **Message Broker:** RabbitMQ (publicacao de eventos da OS)
- **Migracoes:** Flyway
- **Documentacao:** SpringDoc OpenAPI (Swagger)

## Arquitetura

O projeto segue **Arquitetura Hexagonal (Ports & Adapters)**:

```
src/main/java/com/tech/challenger/execution/service/os/
├── domain/                     # Nucleo do dominio
│   ├── model/                  # Entidades (Order, OrderStatus, OrderEvent)
│   └── repository/             # Portas de saida (interfaces de repositorio)
├── application/                # Casos de uso
│   ├── dto/                    # DTOs de entrada/saida
│   ├── event/                  # Publicador de eventos (RabbitMQ)
│   ├── exception/              # Excecoes de negocio
│   └── service/                # OrderServiceImpl
├── infrastructure/             # Adaptadores de infraestrutura
│   └── config/                 # DatabaseConfig, RabbitMQConfig
└── presentation/               # Adaptadores de entrada
    ├── controller/             # OrderServiceController (REST)
    └── exception/              # GlobalExceptionHandler
```

## Endpoints da API

Base URL: `http://localhost:8081/api/v1`

| Metodo | Endpoint                  | Descricao                    |
|--------|---------------------------|------------------------------|
| POST   | `/orders`                 | Criar OS                     |
| GET    | `/orders/{id}`            | Consultar OS                 |
| PUT    | `/orders/{id}/status`     | Atualizar status da OS       |
| GET    | `/orders/{id}/history`    | Historico/auditoria da OS    |

### Exemplos

**Criar OS**
```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": 1, "vehicleId": 2, "description": "Troca de oleo"}'
```

**Atualizar status**
```bash
curl -X PUT http://localhost:8081/api/v1/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "EM_EXECUCAO", "description": "Servico iniciado"}'
```

**Consultar historico**
```bash
curl http://localhost:8081/api/v1/orders/1/history
```

## Status da OS

`RECEBIDA` → `EM_DIAGNOSTICO` → `AGUARDANDO_APROVACAO` → `EM_EXECUCAO` → `FINALIZADA` → `ENTREGUE`

(`CANCELADA` a qualquer momento; OS `ENTREGUE` ou `CANCELADA` nao permite mais alteracoes)

## Eventos e Saga

A estrategia escolhida para a integracao e Saga coreografada: cada servico reage
a eventos e executa sua propria etapa e compensacao, sem coordenador central.
Essa escolha reaproveita o RabbitMQ e reduz o acoplamento entre os servicos.
A integracao completa com Billing e Execution e as compensacoes ainda precisam
ser implementadas; a publicacao de eventos por si so nao constitui uma Saga.

O OS consome eventos de execucao pela chave `execution.status` do exchange
`os.exchange` e rejeicoes de orcamento pela chave `billing.budget_rejected`
do exchange `billing.exchange`. Cada evento precisa de `eventId`, `eventType`
e `orderId`; se houver `status`, ele deve corresponder ao estado da OS. A
inbox e a atualizacao de status compartilham uma transacao PostgreSQL, evitando
reaplicar mensagens duplicadas. Transicoes invalidas falham explicitamente;
apos cinco tentativas a mensagem vai para `os.saga.events.dlq` e requer analise.
O Billing que produz `BUDGET_REJECTED` ainda precisa ser implementado.

As mudancas da OS e seus eventos sao gravados na mesma transacao PostgreSQL.
Um dispatcher grava o historico no MongoDB e publica o evento no RabbitMQ,
aguardando confirmacao e verificando mensagens devolvidas. Em caso de falha,
o evento permanece pendente para nova tentativa. O historico e eventualmente
consistente e pode demorar a aparecer. Uma queda apos a publicacao pode causar
reenvio; consumidores devem tratar o `id` do evento de maneira idempotente.
A confirmacao do broker nao garante que todos os consumidores tenham uma fila
vinculada. As filas dos consumidores devem estar declaradas antes da publicacao;
o provisionamento compartilhado dessas filas ainda precisa ser concluido.

## Executando localmente

### Pre-requisitos
- Java 21
- Docker (para PostgreSQL, MongoDB e RabbitMQ)

### Subir dependencias + aplicacao com Docker Compose
```bash
docker compose up -d
```

### Ou executar apenas as dependencias e rodar a app via Maven
```bash
docker compose up -d postgres mongodb rabbitmq
./mvnw spring-boot:run
```

### Executar testes
```bash
./mvnw clean verify
```

## Documentacao da API

Swagger UI disponivel em: `http://localhost:8081/api/v1/swagger-ui.html`

## Infraestrutura

- `docker/Dockerfile` - Imagem Docker multi-stage da aplicacao
- `docker-compose.yml` - Orquestracao local (app + PostgreSQL + MongoDB + RabbitMQ)
- `k8s/` - Manifests Kubernetes (namespace, deployment, service e configmap)
- `.github/workflows/` - Pipelines CI/CD (build, test, deploy)

O deploy Kubernetes requer a infraestrutura compartilhada provisionada previamente:
PostgreSQL `os-postgresql`, MongoDB `os-mongodb` para historico e o Secret
`rabbitmq-credentials`. Os bancos e suas credenciais sao gerenciados pela
infraestrutura, nao pelos manifests de aplicacao.

## CI/CD e protecao da main

O workflow `OS CI/CD` executa `sh ./mvnw -B clean verify` em PRs e pushes
para `main` ou `feature/**`, exige 80% de cobertura de linhas no JaCoCo e
aguarda o quality gate do Sonar. Configure o secret de repositorio
`SONAR_TOKEN` e as variables `SONAR_HOST_URL` e `SONAR_PROJECT_KEY`;
para SonarCloud configure tambem `SONAR_ORGANIZATION`. O projeto Sonar deve
estar configurado para analisar as branches e PRs utilizados.

Apos push validado em `main`, o workflow publica a imagem GHCR com a tag do
commit e implanta no namespace `os-service`, aguardando o rollout. Configure no
environment `production` o secret `KUBE_CONFIG` com o kubeconfig completo em
texto, de um cluster acessivel ao runner, com permissoes para os recursos do
deploy. Provisione antes os bancos e secrets mencionados acima e o secret
Kubernetes `ghcr-pull` no namespace `os-service`, com acesso de leitura a imagem
GHCR. Nao grave credenciais no repositorio.

Configure no GitHub a protecao de `main` para exigir pull request e o check
`Build, testes e qualidade` apos o workflow estar disponivel no PR. PRs de
forks nao recebem o secret do Sonar e, por isso, o check falha explicitamente;
contribuicoes externas devem ser aplicadas em uma branch confiavel do
repositorio por um mantenedor, sem expor secrets ao codigo nao revisado.
