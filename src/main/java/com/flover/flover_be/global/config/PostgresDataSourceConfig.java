package com.flover.flover_be.global.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(prefix = "postgres.datasource", name = "enabled", havingValue = "true")
public class PostgresDataSourceConfig {

    private HikariDataSource postgresDataSource;

    @Bean
    public JdbcTemplate postgresJdbcTemplate(
            @Value("${postgres.datasource.url}") String url,
            @Value("${postgres.datasource.username}") String username,
            @Value("${postgres.datasource.password}") String password,
            @Value("${postgres.datasource.driver-class-name}") String driverClassName
    ) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setMinimumIdle(1);
        dataSource.setMaximumPoolSize(5);
        this.postgresDataSource = dataSource;
        return new JdbcTemplate(dataSource);
    }

    @PreDestroy
    public void closePostgresDataSource() {
        if (postgresDataSource != null) {
            postgresDataSource.close();
        }
    }
}
