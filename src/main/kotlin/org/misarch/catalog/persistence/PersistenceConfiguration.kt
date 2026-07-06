package org.misarch.catalog.persistence

import com.querydsl.sql.PostgreSQLTemplates
import com.querydsl.sql.SQLTemplates
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.flyway.FlywayProperties
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.r2dbc.connection.R2dbcTransactionManager


/**
 * Database / persistence configuration
 * Configures Flyway migrations and reactive transactions
 */
@Configuration
@EnableConfigurationProperties(R2dbcProperties::class, FlywayProperties::class)
internal class PersistenceConfiguration {

    /**
     * Configures Flyway migrations
     *
     * @param flywayProperties the provided Flyway properties
     * @param r2dbcProperties the provided R2DBC properties
     * @return the configured Flyway instance
     */
    @Bean(initMethod = "migrate")
    @ConditionalOnProperty(name = ["spring.flyway.enabled"], matchIfMissing = true, havingValue = "true")
    fun flyway(flywayProperties: FlywayProperties, r2dbcProperties: R2dbcProperties): Flyway {
        return Flyway.configure()
            .dataSource(
                flywayProperties.url,
                r2dbcProperties.username,
                r2dbcProperties.password
            )
            .locations(*flywayProperties.locations.toTypedArray())
            .baselineOnMigrate(true)
            .load()
    }

    /**
     * Configures the R2DBC connection factory with explicit pool settings.
     * Bypasses Spring Boot auto-configuration to ensure maxSize is properly applied.
     *
     * @param properties the bound R2DBC properties
     * @return the configured pooled connection factory
     */
    @Bean(destroyMethod = "dispose")
    @Primary
    fun connectionFactory(properties: R2dbcProperties): ConnectionFactory {
        val factory = ConnectionFactories.get(
            ConnectionFactoryOptions.parse(properties.url)
                .mutate()
                .option(ConnectionFactoryOptions.USER, properties.username)
                .option(ConnectionFactoryOptions.PASSWORD, properties.password)
                .build()
        )
        val poolConfig = ConnectionPoolConfiguration.builder(factory)
            .initialSize(properties.pool.initialSize)
            .maxSize(properties.pool.maxSize)
            .maxIdleTime(properties.pool.maxIdleTime)
            .maxLifeTime(properties.pool.maxLifeTime)
            .build()
        return ConnectionPool(poolConfig)
    }

    /**
     * Configures reactive transactions via a [R2dbcTransactionManager]
     *
     * @param connectionFactory the provided connection factory
     * @return the configured transaction manager
     */
    @Bean
    fun transactionManager(connectionFactory: ConnectionFactory): R2dbcTransactionManager {
        return R2dbcTransactionManager(connectionFactory)
    }

    /**
     * Configures Querydsl SQL templates
     * Overwrites the default configuration which depends on flyway
     *
     * @return the configured SQL templates
     */
    @Bean
    fun querydslSqlConfiguration(): com.querydsl.sql.Configuration {
        val configuration = com.querydsl.sql.Configuration(PostgreSQLTemplates.DEFAULT)
        return configuration
    }
}