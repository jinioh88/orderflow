package com.orderflow.domain.common;

import com.orderflow.common.error.BusinessException;
import com.orderflow.common.error.CommonErrorCode;

/**
 * 애그리거트 상태 전이 가드 위반 — 409 CONFLICT (api-spec 1.4).
 * 도메인별 전용 에러 코드가 필요해지면 해당 컨텍스트의 ErrorCode로 세분화한다.
 */
public class InvalidStateTransitionException extends BusinessException {

    public InvalidStateTransitionException(String message) {
        super(CommonErrorCode.CONFLICT, message);
    }
}
