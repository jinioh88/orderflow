package com.orderflow.infra.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.orderflow")
@EnableJpaRepositories(basePackages = "com.orderflow")
public class JpaConfig {
}
