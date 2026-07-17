package com.orderflow.api.common.response;

import java.util.List;

/**
 * 페이징 응답 — ApiResponse의 data 자리에 들어간다 (api-spec.md 1.5).
 */
public record PageResponse<T>(List<T> items, PageMeta page) {

    public record PageMeta(int number, int size, long totalElements, int totalPages) {
    }

    public static <T> PageResponse<T> of(List<T> items, int number, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new PageResponse<>(items, new PageMeta(number, size, totalElements, totalPages));
    }
}
