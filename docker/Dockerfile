# -- Estagio 1: Build --
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

LABEL maintainer="FIAP Tech Challenge"
LABEL description="OS Service - Microsservico de Ordem de Servico"

# Copia o pom.xml e baixa as dependencias (cache de camadas Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o codigo-fonte e gera o JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# -- Estagio 2: Execucao --
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario nao-root por seguranca
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copia apenas o JAR gerado no estagio anterior
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

# JVM tuning para containers: respeita limites de CPU/memoria do container
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]

# Health check nativo do Docker
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8081/api/v1/actuator/health || exit 1

