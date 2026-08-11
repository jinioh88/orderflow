package com.orderflow.infra.catalog;

import com.orderflow.domain.catalog.ProductStatus;

/**
 * 상품 목록 검색 조건 (api-spec 3.2.2) — 모두 선택.
 * keyword는 품목코드·품명·바코드 부분 일치, category는 정확 일치.
 */
public record ProductSearchCondition(
        String keyword,
        String category,
        ProductStatus status,
        Boolean limited) {
}
