package com.orderflow.infra.tenant;

import com.orderflow.domain.common.TenantContext;
import com.orderflow.domain.common.TenantFilter;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.jspecify.annotations.Nullable;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.TransactionExecution;
import org.springframework.transaction.TransactionExecutionListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 테넌트 필터 활성화 지점 (US-AUTH-04, 설계 노트 §2).
 *
 * OSIV off라 세션 수명 = 트랜잭션 수명이므로, 트랜잭션 시작 훅 하나가 세션을 얻는
 * 모든 경로(리포지토리·EntityManager 직접 사용)를 커버한다.
 * TenantContext 상태별: TENANT → 해당 ID로 활성화 / UNFILTERED → 미활성화 /
 * 미설정 → 불가능 값(-1)으로 활성화 (fail-closed).
 */
public class TenantFilterTransactionListener implements TransactionExecutionListener {

    private final EntityManagerFactory entityManagerFactory;

    public TenantFilterTransactionListener(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void afterBegin(TransactionExecution transaction, @Nullable Throwable beginFailure) {
        if (beginFailure != null) {
            return;
        }
        EntityManagerHolder holder =
                (EntityManagerHolder) TransactionSynchronizationManager.getResource(entityManagerFactory);
        if (holder == null) {
            return;
        }
        if (TenantContext.isUnfiltered()) {
            return;
        }
        holder.getEntityManager().unwrap(Session.class)
                .enableFilter(TenantFilter.NAME)
                .setParameter(TenantFilter.PARAM, TenantContext.tenantIdOrSentinel());
    }
}
