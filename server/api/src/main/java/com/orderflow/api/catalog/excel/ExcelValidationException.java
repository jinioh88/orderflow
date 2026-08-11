package com.orderflow.api.catalog.excel;

import com.orderflow.common.error.BusinessException;
import com.orderflow.common.error.CatalogErrorCode;
import java.util.List;
import lombok.Getter;

/**
 * 행 단위 검증 실패 — 400 EXCEL_VALIDATION_FAILED, details[{row, field, reason}] (api-spec 3.3.2).
 * 오류가 1건이라도 있으면 전체 반영이 취소된다 (all-or-nothing, US-CAT-02 인수 조건).
 */
@Getter
public class ExcelValidationException extends BusinessException {

    private final transient List<ExcelRowError> errors;

    public ExcelValidationException(List<ExcelRowError> errors) {
        super(CatalogErrorCode.EXCEL_VALIDATION_FAILED,
                "엑셀 검증에 실패했습니다. 오류 %d건 — 전체 반영이 취소되었습니다.".formatted(errors.size()));
        this.errors = List.copyOf(errors);
    }
}
