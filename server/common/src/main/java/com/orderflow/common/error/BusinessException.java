package com.orderflow.common.error;

import java.util.List;
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

    /**
     * 항목 단위 오류 목록 — 있으면 에러 응답 details로 직렬화된다 (api-spec 1.3).
     * 구조화된 오류를 가진 예외(엑셀 행 오류 등)가 재정의한다 — 공통 핸들러에
     * 기능별 분기를 두지 않기 위한 확장점.
     */
    public List<ErrorDetail> details() {
        return List.of();
    }
}
