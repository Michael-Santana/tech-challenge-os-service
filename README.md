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
│   └── service/                # OrderServiceImpl, SagaOrchestrator
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
- `k8s/` - Manifests Kubernetes (namespace, deployment, service, configmap, secret, pvc)
- `.github/workflows/` - Pipelines CI/CD (build, test, deploy)
