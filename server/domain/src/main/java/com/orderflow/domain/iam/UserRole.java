package com.orderflow.domain.iam;

/**
 * 역할 5종 (api-spec 2.1, NFR-2.3). STORE_STAFF는 v1.1(US-AUTH-05) — MVP에는 발급 경로가 없다.
 */
public enum UserRole {
    SYSTEM, HQ_ADMIN, HQ_MANAGER, STORE_OWNER, STORE_STAFF;

    public boolean isStoreRole() {
        return this == STORE_OWNER || this == STORE_STAFF;
    }

    public boolean isHqRole() {
        return this == HQ_ADMIN || this == HQ_MANAGER;
    }
}
