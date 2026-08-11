package com.orderflow.api.catalog.excel;

import static com.orderflow.api.catalog.excel.ProductExcelLayout.MAX_DATA_ROWS;
import static com.orderflow.api.catalog.excel.ProductExcelLayout.UPLOAD_HEADERS;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * 상품 엑셀 파싱 (US-CAT-02, api-spec 3.3.1~3.3.2) — 첫 시트만 읽는다.
 * 파일 자체 불량(.xlsx 아님·헤더 불일치·0행·행 수 초과)은 ExcelParseException,
 * 셀 값의 의미 검증은 ProductExcelValidator가 담당한다.
 * 라이브러리 선정 근거(POI XSSF, 스트리밍 미채택)는 study/excel-library.md 참조.
 */
@Component
public class ProductExcelParser {

    private final DataFormatter formatter = new DataFormatter();

    public List<ProductExcelRow> parse(InputStream inputStream) {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            validateHeader(sheet.getRow(0));
            return readDataRows(sheet);
        } catch (IOException | RuntimeException e) {
            if (e instanceof ExcelParseException parseException) {
                throw parseException;
            }
            throw new ExcelParseException("엑셀 파일을 읽을 수 없습니다. .xlsx 형식인지 확인해 주세요.");
        }
    }

    private void validateHeader(Row headerRow) {
        if (headerRow == null) {
            throw new ExcelParseException("헤더 행(1행)이 없습니다.");
        }
        for (int col = 0; col < UPLOAD_HEADERS.size(); col++) {
            String actual = formatter.formatCellValue(headerRow.getCell(col)).trim();
            if (!UPLOAD_HEADERS.get(col).equals(actual)) {
                throw new ExcelParseException("헤더가 규격과 다릅니다. %d열은 '%s'여야 합니다 (현재: '%s')."
                        .formatted(col + 1, UPLOAD_HEADERS.get(col), actual.isEmpty() ? "빈칸" : actual));
            }
        }
    }

    private List<ProductExcelRow> readDataRows(Sheet sheet) {
        List<ProductExcelRow> rows = new ArrayList<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            ProductExcelRow row = toRow(sheet.getRow(rowIndex), rowIndex + 1);
            if (row.isBlank()) {
                continue; // 꼬리 빈 행 등은 오류가 아니라 무시
            }
            rows.add(row);
            if (rows.size() > MAX_DATA_ROWS) {
                throw new ExcelParseException("데이터가 최대 %,d행을 초과했습니다.".formatted(MAX_DATA_ROWS));
            }
        }
        if (rows.isEmpty()) {
            throw new ExcelParseException("데이터 행이 없습니다 (2행부터 입력).");
        }
        return rows;
    }

    /** 업로드는 A~F만 읽는다 — G 이후 열 무시 (왕복 호환, api-spec 3.3.1) */
    private ProductExcelRow toRow(Row row, int excelRowNum) {
        if (row == null) {
            return new ProductExcelRow(excelRowNum, "", "", "", "", "", "");
        }
        return new ProductExcelRow(excelRowNum,
                cell(row, ProductExcelLayout.COL_PRODUCT_CODE),
                cell(row, ProductExcelLayout.COL_NAME),
                cell(row, ProductExcelLayout.COL_BARCODE),
                cell(row, ProductExcelLayout.COL_CATEGORY),
                cell(row, ProductExcelLayout.COL_ORDER_UNIT),
                cell(row, ProductExcelLayout.COL_UNIT_PRICE));
    }

    private String cell(Row row, int col) {
        return formatter.formatCellValue(row.getCell(col)).trim();
    }
}
