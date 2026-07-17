package com.orderflow.api.common.response;

/**
 * 성공 응답 래퍼 — 최상위 키는 data 하나 (api-spec.md 1.3).
 * 본문 없는 성공은 이 래퍼 대신 204 No Content를 쓴다.
 */
public record ApiResponse<T>(T data) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }
}
