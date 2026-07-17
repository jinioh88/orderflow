package com.orderflow.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orderflow.common.error.ErrorCode;

import java.util.List;

/**
 * 에러 응답 래퍼 — 최상위 키는 error 하나 (api-spec.md 1.3).
 * details는 필드 단위 오류가 있을 때만 직렬화된다.
 */
public record ErrorResponse(ErrorBody error) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(String code, String message, List<FieldError> details) {
    }

    public record FieldError(String field, String reason) {
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.message());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(new ErrorBody(errorCode.code(), message, null));
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> details) {
        return new ErrorResponse(new ErrorBody(errorCode.code(), errorCode.message(), details));
    }
}
