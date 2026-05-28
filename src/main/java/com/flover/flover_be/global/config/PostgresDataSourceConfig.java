package com.flover.flover_be.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
@ConditionalOnProperty(prefix = "postgres.datasource", name = "enabled", havingValue = "true")
public class PostgresDataSourceConfig {

    @Bean
    public JdbcTemplate postgresJdbcTemplate(
            @Value("${postgres.datasource.url}") String url,
            @Value("${postgres.datasource.username}") String username,
            @Value("${postgres.datasource.password}") String password,
            @Value("${postgres.datasource.driver-class-name}") String driverClassName
    ) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        return new JdbcTemplate(dataSource);
    }
}
