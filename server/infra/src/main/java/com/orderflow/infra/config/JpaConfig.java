package com.orderflow.infra.config;

import com.orderflow.infra.tenant.TenantFilterTransactionListener;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

@Configuration
@EntityScan(basePackages = "com.orderflow")
@EnableJpaRepositories(basePackages = "com.orderflow")
@EnableJpaAuditing
public class JpaConfig {

    /** 모든 트랜잭션 시작 시 테넌트 필터를 세션에 활성화한다 (설계 노트 §2 — 승인된 활성화 지점). */
    @Bean
    InitializingBean tenantFilterRegistration(PlatformTransactionManager transactionManager,
                                              EntityManagerFactory entityManagerFactory) {
        return () -> {
            if (!(transactionManager instanceof AbstractPlatformTransactionManager aptm)) {
                throw new IllegalStateException(
                        "테넌트 필터 훅을 등록할 수 없는 TransactionManager: " + transactionManager.getClass());
            }
            aptm.addListener(new TenantFilterTransactionListener(entityManagerFactory));
        };
    }
}
