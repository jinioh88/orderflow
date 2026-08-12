package com.orderflow.infra.catalog;

import com.orderflow.domain.catalog.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 상품 목록 프로젝션 DTO (api-spec 3.2.2) — 도메인 모델을 거치지 않는 CQRS-lite 조회 결과.
 * availableQty는 한정 품목만 값 존재 (hq_stock left join), 아니면 null.
 */
public record ProductSummary(
        Long id,
        String productCode,
        String name,
        String barcode,
        String category,
        String orderUnit,
        BigDecimal unitPrice,
        boolean limited,
        Integer availableQty,
        ProductStatus status,
        LocalDateTime createdAt) {
}
