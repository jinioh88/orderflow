package com.orderflow.api.catalog.excel;

import java.util.List;

/**
 * 엑셀 검증 결과 (US-CAT-02) — 오류가 1건이라도 있으면 전체 반영이 거부된다 (all-or-nothing).
 */
public record ProductExcelValidation(
        int totalRows,
        List<ProductUpsertCommand> commands,
        List<ExcelRowError> errors) {

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public long createdCount() {
        return commands.stream().filter(ProductUpsertCommand::isNew).count();
    }

    public long updatedCount() {
        return commands.stream().filter(command -> !command.isNew()).count();
    }
}
