package com.orderflow.domain.common;

/**
 * 테넌트 필터 상수 (US-AUTH-04) — @FilterDef 선언은 이 패키지의 package-info.java.
 * 테넌트 스코프 엔티티는 각자 다음을 직접 선언한다 (@MappedSuperclass 상속 불가 — Hibernate 제약):
 * {@code @Filter(name = TenantFilter.NAME, condition = TenantFilter.CONDITION)}
 */
public final class TenantFilter {

    public static final String NAME = "tenantFilter";
    public static final String PARAM = "tenantId";
    public static final String CONDITION = "tenant_id = :tenantId";

    /**
     * fail-closed 센티널 — TenantContext 미설정 상태에서 필터를 이 값으로 켠다.
     * 컨텍스트 설정을 잊으면 전체 노출이 아니라 빈 결과가 되도록 실패 방향을 뒤집는다 (설계 노트 §1).
     */
    public static final long NO_TENANT = -1L;

    private TenantFilter() {
    }
}
