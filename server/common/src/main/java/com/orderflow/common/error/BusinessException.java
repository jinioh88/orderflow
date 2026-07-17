package com.orderflow.common.error;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반의 공통 부모 — 도메인 예외는 이 클래스를 상속한다.
 * GlobalExceptionHandler가 errorCode의 status/code로 에러 응답을 만든다 (api-spec.md 1.3).
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.message());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
