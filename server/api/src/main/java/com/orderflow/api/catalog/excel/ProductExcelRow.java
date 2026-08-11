package com.orderflow.api.catalog.excel;

/**
 * 파싱된 데이터 행 — 셀 값은 전부 문자열로 보존한다 (형식 검증은 Validator의 몫).
 * rowNum은 엑셀 실제 행 번호(2~).
 */
public record ProductExcelRow(
        int rowNum,
        String productCode,
        String name,
        String barcode,
        String category,
        String orderUnit,
        String unitPrice) {

    public boolean isBlank() {
        return isBlank(productCode) && isBlank(name) && isBlank(barcode)
                && isBlank(category) && isBlank(orderUnit) && isBlank(unitPrice);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
