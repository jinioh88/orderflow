package com.orderflow.api.auth;

import com.orderflow.domain.iam.UserRole;

/**
 * 인증된 요청의 주체 — JWT 클레임의 역직렬화 결과 (NFR-2.2).
 * SecurityContext의 principal로 들어간다. SYSTEM은 tenantId/storeId가 null.
 */
public record AuthenticatedUser(Long userId, Long tenantId, Long storeId, UserRole role) {
}
