package com.orderflow.api.catalog.excel;

import java.math.BigDecimal;

/**
 * 검증을 통과한 행의 반영 명령 — 품목코드 매칭 결과로 등록/수정이 갈린다 (api-spec 3.3.2).
 * 실제 DB 반영(all-or-nothing 트랜잭션)은 엑셀 업로드 2/2 태스크.
 */
public record ProductUpsertCommand(
        int rowNum,
        boolean isNew,
        String productCode,
        String name,
        String barcode,
        String category,
        String orderUnit,
        BigDecimal unitPrice) {
}
