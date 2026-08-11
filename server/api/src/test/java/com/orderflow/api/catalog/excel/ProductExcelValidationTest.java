package com.orderflow.api.catalog.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 엑셀 파싱·행별 검증 단위 테스트 (US-CAT-02, api-spec 3.3) — 오류 유형별 케이스.
 * DB 반영(all-or-nothing)은 엑셀 업로드 2/2에서 통합 테스트로 검증한다.
 */
class ProductExcelValidationTest {

    private final ProductExcelParser parser = new ProductExcelParser();
    private final ProductExcelValidator validator = new ProductExcelValidator();

    /** 헤더 + 데이터 행들로 .xlsx 생성. 각 행은 셀 값 배열(문자열/숫자 혼용) */
    private InputStream workbook(List<String> headers, List<List<Object>> dataRows) {
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
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<Object> normalRow(String code, String barcode) {
        return List.of(code, "김치만두 1kg", barcode, "냉동식품", "BOX", 12000);
    }

    @Nested
    @DisplayName("파싱 — 파일 자체 불량은 EXCEL_FILE_INVALID (3.3.2)")
    class Parsing {

        @Test
        @DisplayName("정상 파일 — 숫자 셀 정규화, G열 이후·빈 행 무시, 행 번호는 엑셀 기준")
        void parsesNormalFile() {
            var rows = parser.parse(workbook(ProductExcelLayout.UPLOAD_HEADERS, List.of(
                    List.of("P-0001", " 김치만두 ", "8801111111111", "냉동식품", "BOX", 12000, "Y", 100, "판매중"),
                    List.of("", "", "", "", "", ""), // 빈 행 — 무시
                    normalRow("P-0002", "8802222222222"))));

            assertThat(rows).hasSize(2);
            assertThat(rows.get(0).rowNum()).isEqualTo(2);
            assertThat(rows.get(0).name()).isEqualTo("김치만두"); // trim
            assertThat(rows.get(0).unitPrice()).isEqualTo("12000"); // 숫자 셀 → 문자열
            assertThat(rows.get(1).rowNum()).isEqualTo(4); // 빈 행 건너뛰어도 엑셀 행 번호 유지
        }

        @Test
        @DisplayName("13자리 바코드 숫자 셀이 지수 표기로 깨지지 않는다 (CAT-2 셀프 리뷰 회귀)")
        void preservesLongNumericCells() {
            var rows = parser.parse(workbook(ProductExcelLayout.UPLOAD_HEADERS, List.of(
                    List.of("P-0001", "만두", 8801111111111L, "냉동식품", "BOX", 12000))));

            assertThat(rows.get(0).barcode()).isEqualTo("8801111111111");
        }

        @Test
        @DisplayName("헤더 불일치 — 열 위치와 기대 헤더를 사유에 명시")
        void rejectsWrongHeader() {
            assertThatThrownBy(() -> parser.parse(workbook(
                    List.of("품목코드", "상품명", "바코드", "카테고리", "발주단위", "단가"),
                    List.of(normalRow("P-0001", "8801111111111")))))
                    .isInstanceOf(ExcelParseException.class)
                    .hasMessageContaining("품명");
        }

        @Test
        @DisplayName("데이터 0행 거부")
        void rejectsEmptyData() {
            assertThatThrownBy(() -> parser.parse(workbook(ProductExcelLayout.UPLOAD_HEADERS, List.of())))
                    .isInstanceOf(ExcelParseException.class);
        }

        @Test
        @DisplayName(".xlsx가 아닌 파일 거부")
        void rejectsNonXlsx() {
            assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream("not an excel".getBytes())))
                    .isInstanceOf(ExcelParseException.class);
        }
    }

    @Nested
    @DisplayName("행별 검증 — 오류 유형별 (3.3.2 검증 규칙 표)")
    class RowValidation {

        private ProductExcelValidation validate(List<List<Object>> dataRows,
                                                Set<String> existingCodes,
                                                Map<String, String> existingBarcodeOwners) {
            var rows = parser.parse(workbook(ProductExcelLayout.UPLOAD_HEADERS, dataRows));
            return validator.validate(rows, existingCodes, existingBarcodeOwners);
        }

        @Test
        @DisplayName("정상 파일 — DB 기존 품목코드는 수정, 신규는 등록으로 구분된다")
        void classifiesCreateAndUpdate() {
            var result = validate(List.of(
                            normalRow("P-0001", "8801111111111"),
                            normalRow("P-0002", "8802222222222")),
                    Set.of("P-0001"), Map.of("8801111111111", "P-0001"));

            assertThat(result.hasErrors()).isFalse();
            assertThat(result.totalRows()).isEqualTo(2);
            assertThat(result.updatedCount()).isEqualTo(1);
            assertThat(result.createdCount()).isEqualTo(1);
            assertThat(result.commands().get(0).isNew()).isFalse();
        }

        @Test
        @DisplayName("필수값 누락·형식 오류·길이 초과를 행 번호와 필드로 리포트한다")
        void reportsFieldErrors() {
            var result = validate(List.of(
                            List.of("P-0001", "", "8801111111111", "냉동식품", "BOX", 12000),   // 품명 누락
                            List.of("P-0002", "만두", "8802222222222", "냉동식품", "BOX", "만원"), // 단가 형식
                            List.of("P-0003", "만두", "8803333333333", "냉동식품", "BOX", -1),    // 단가 음수
                            List.of("P-0004", "만두", "8804444444444", "냉동식품", "BOX", 100.5), // 단가 소수
                            List.of("P-0005", "만두", "8805555555555", "냉동식품", "박스단위초과함박스단위초과함박스단위초과함박스단위초과함", 100)), // 발주단위 길이
                    Set.of(), Map.of());

            assertThat(result.errors()).containsExactly(
                    new ExcelRowError(2, "name", "필수 입력입니다."),
                    new ExcelRowError(3, "unitPrice", "단가는 0 이상의 정수여야 합니다."),
                    new ExcelRowError(4, "unitPrice", "단가는 0 이상의 정수여야 합니다."),
                    new ExcelRowError(5, "unitPrice", "단가는 0 이상의 정수여야 합니다."),
                    new ExcelRowError(6, "orderUnit", "최대 20자를 초과했습니다."));
            assertThat(result.commands()).isEmpty(); // 오류 행은 명령에서 제외
        }

        @Test
        @DisplayName("파일 내 중복은 먼저 나온 행 번호를 명시하고, DB 기존 코드와 구분된다")
        void reportsInFileDuplicates() {
            var result = validate(List.of(
                            normalRow("P-0001", "8801111111111"),
                            normalRow("P-0001", "8802222222222"),   // 코드 중복 (2행과)
                            normalRow("P-0003", "8801111111111")),  // 바코드 중복 (2행과)
                    Set.of("P-0001"), Map.of("8801111111111", "P-0001")); // DB 기존 코드는 오류 아님

            assertThat(result.errors()).containsExactly(
                    new ExcelRowError(3, "productCode", "파일 내 품목코드가 중복됩니다 (2행과 동일)."),
                    new ExcelRowError(4, "barcode", "파일 내 바코드가 중복됩니다 (2행과 동일)."),
                    new ExcelRowError(4, "barcode", "다른 상품이 사용 중인 바코드입니다."));
            assertThat(result.updatedCount()).isEqualTo(1); // 2행은 정상 수정 대상
        }

        @Test
        @DisplayName("DB의 다른 상품 바코드와 충돌하면 오류, 자기(같은 품목코드) 바코드는 정상")
        void reportsDbBarcodeConflict() {
            var result = validate(List.of(
                            normalRow("P-0001", "8801111111111"),  // 자기 바코드 유지 — 정상 수정
                            normalRow("P-0002", "8809999999999")), // P-0009의 바코드 탈취 시도
                    Set.of("P-0001", "P-0009"),
                    Map.of("8801111111111", "P-0001", "8809999999999", "P-0009"));

            assertThat(result.errors()).containsExactly(
                    new ExcelRowError(3, "barcode", "다른 상품이 사용 중인 바코드입니다."));
        }
    }
}
