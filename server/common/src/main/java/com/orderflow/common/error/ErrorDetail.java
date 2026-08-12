package com.orderflow.common.error;

/**
 * 에러 응답 details 원소의 공통 모델 — row는 엑셀 행 오류 등 행 단위 오류만 사용 (api-spec 1.3·3.3.2).
 * BusinessException.details()로 노출되면 API 계층이 그대로 직렬화한다.
 */
public record ErrorDetail(Integer row, String field, String reason) {

    public static ErrorDetail of(String field, String reason) {
        return new ErrorDetail(null, field, reason);
    }

    public static ErrorDetail of(int row, String field, String reason) {
        return new ErrorDetail(row, field, reason);
    }
}
