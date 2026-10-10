package com.tech.challenger.execution.service.os.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuracao dos bancos de dados:
 * - PostgreSQL (JPA) para dados transacionais da OS
 * - MongoDB para historico/auditoria (eventos da OS)
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.tech.challenger.execution.service.os.domain.repository")
@EnableMongoRepositories(basePackages = "com.tech.challenger.execution.service.os.domain.repository")
public class DatabaseConfig {
}

