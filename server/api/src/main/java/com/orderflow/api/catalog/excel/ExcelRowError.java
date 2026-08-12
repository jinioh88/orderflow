package com.orderflow.api.catalog.excel;

/**
 * 행 번호별 오류 리포트 원소 (api-spec 3.3.2) — details[{row, field, reason}]의 서버측 모델.
 * row는 엑셀 실제 행 번호(헤더=1행, 데이터=2행~).
 */
public record ExcelRowError(int row, String field, String reason) {
}
