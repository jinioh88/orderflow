/**
 * 도메인 공통 — TenantContext, 공통 엔티티 기반, 테넌트 필터 정의.
 */
@FilterDef(
        name = TenantFilter.NAME,
        parameters = @ParamDef(name = TenantFilter.PARAM, type = Long.class)
)
package com.orderflow.domain.common;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
