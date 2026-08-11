package com.orderflow.api.catalog.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 테스트용 .xlsx 생성 헬퍼 — 단위·통합 테스트가 공유한다.
 * 셀 값은 문자열/숫자 혼용 (Number는 숫자 셀로 기록 — 실사용 입력 형태 재현).
 */
public final class ExcelTestFiles {

    private ExcelTestFiles() {
    }

    public static byte[] xlsx(List<String> headers, List<List<Object>> dataRows) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            for (int c = 0; c < headers.size(); c++) {
                header.createCell(c).setCellValue(headers.get(c));
            }
            for (int r = 0; r < dataRows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<Object> cells = dataRows.get(r);
                for (int c = 0; c < cells.size(); c++) {
                    Object value = cells.get(c);
                    if (value instanceof Number number) {
                        row.createCell(c).setCellValue(number.doubleValue());
                    } else if (value != null) {
                        row.createCell(c).setCellValue(value.toString());
                    }
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
