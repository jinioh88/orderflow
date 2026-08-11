package com.orderflow.api.catalog.excel;

import java.util.List;

/**
 * 엑셀 파일 레이아웃 규약 (api-spec 3.3.1) — 업로드는 A~F만 읽고 G 이후 열은 무시한다
 * (다운로드 파일 재업로드 왕복 호환). 행 번호는 엑셀 실제 행 번호(헤더=1, 데이터=2~).
 */
public final class ProductExcelLayout {

    public static final List<String> UPLOAD_HEADERS =
            List.of("품목코드", "품명", "바코드", "카테고리", "발주단위", "단가");

    /** 다운로드는 업로드 6열 + 읽기 전용 3열 (api-spec 3.3.1 — 업로드 시 G 이후 무시) */
    public static final List<String> DOWNLOAD_HEADERS =
            List.of("품목코드", "품명", "바코드", "카테고리", "발주단위", "단가", "한정품목", "가용재고", "상태");

    public static final int COL_PRODUCT_CODE = 0;
    public static final int COL_NAME = 1;
    public static final int COL_BARCODE = 2;
    public static final int COL_CATEGORY = 3;
    public static final int COL_ORDER_UNIT = 4;
    public static final int COL_UNIT_PRICE = 5;

    /** 최대 데이터 행 수 — 초과 시 EXCEL_FILE_INVALID (api-spec 3.3.1) */
    public static final int MAX_DATA_ROWS = 10_000;

    private ProductExcelLayout() {
    }
}
