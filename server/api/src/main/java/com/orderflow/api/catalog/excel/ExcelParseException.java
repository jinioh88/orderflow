package com.orderflow.api.catalog.excel;

import com.orderflow.common.error.BusinessException;
import com.orderflow.common.error.CatalogErrorCode;

/**
 * 파일 자체 불량 — 400 EXCEL_FILE_INVALID, details 없음 (api-spec 3.3.2).
 * 행 단위 오류는 예외가 아니라 검증 결과(ExcelRowError)로 수집한다.
 */
public class ExcelParseException extends BusinessException {

    public ExcelParseException(String message) {
        super(CatalogErrorCode.EXCEL_FILE_INVALID, message);
    }

    public ExcelParseException(String message, Throwable cause) {
        super(CatalogErrorCode.EXCEL_FILE_INVALID, message);
        initCause(cause);
    }
}
