package com.orderflow.api.support;

import com.orderflow.domain.common.TenantContext;
import com.orderflow.domain.common.TenantFilter;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;

/**
 * 테스트 전용 테넌트 스코프 전환 유틸.
 *
 * 프로덕션에서는 트랜잭션 시작 훅이 필터를 켜지만(설계 노트 §2), @Transactional 테스트는
 * 트랜잭션이 테스트 시작 시 이미 열려 있어 훅 시점에 컨텍스트가 없다. 그래서 테스트는
 * 이 유틸로 TenantContext와 현재 세션의 필터를 함께 전환한다 — SQL 레벨 동작은 동일하다.
 * 전환 시 flush + clear로 영속성 컨텍스트를 비워 1차 캐시를 통한 교차 노출을 배제한다.
 */
public final class TenantScope {

    private TenantScope() {
    }

    /** 특정 테넌트 스코프로 전환 */
    public static void tenant(EntityManager em, Long tenantId) {
        switchContext(em);
        TenantContext.setTenant(tenantId);
        em.unwrap(Session.class)
                .enableFilter(TenantFilter.NAME)
                .setParameter(TenantFilter.PARAM, tenantId);
    }

    /** 전역(무필터) 스코프로 전환 — SYSTEM 유스케이스·픽스처 검증용 */
    public static void unfiltered(EntityManager em) {
        switchContext(em);
        TenantContext.setUnfiltered();
        em.unwrap(Session.class).disableFilter(TenantFilter.NAME);
    }

    /** 컨텍스트 미설정 상태 재현 — fail-closed 센티널 필터 */
    public static void none(EntityManager em) {
        switchContext(em);
        em.unwrap(Session.class)
                .enableFilter(TenantFilter.NAME)
                .setParameter(TenantFilter.PARAM, TenantFilter.NO_TENANT);
    }

    private static void switchContext(EntityManager em) {
        em.flush();
        em.clear();
        TenantContext.clear();
    }
}
