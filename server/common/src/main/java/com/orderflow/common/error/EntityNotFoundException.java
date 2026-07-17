package com.orderflow.common.error;

/**
 * 존재하지 않는 리소스 접근 — 교차 테넌트 접근도 이 예외로 동일하게 404를 낸다 (NFR-2.1, 존재 여부 비노출).
 */
public class EntityNotFoundException extends BusinessException {

    public EntityNotFoundException() {
        super(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    public EntityNotFoundException(String message) {
        super(CommonErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
