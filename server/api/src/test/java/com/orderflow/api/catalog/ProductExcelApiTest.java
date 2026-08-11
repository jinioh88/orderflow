package com.orderflow.api.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderflow.api.catalog.excel.ExcelTestFiles;
import com.orderflow.api.catalog.excel.ProductExcelLayout;
import com.orderflow.api.support.ApiIntegrationTest;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 엑셀 업로드/다운로드 API 통합 테스트 (US-CAT-02·03, api-spec 3.3).
 * 핵심 시연: all-or-nothing — 오류 1행이면 정상 행도 반영되지 않는다.
 */
class ProductExcelApiTest extends ApiIntegrationTest {

    private static final String XLSX_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private TenantSetup bonjuk;

    @BeforeEach
    void setUpTenant() {
        bonjuk = createTenantSetup("bonjuk");
    }

    private List<Object> row(String code, String name, String barcode, Object price) {
        return List.of(code, name, barcode, "냉동식품", "BOX", price);
    }

    private MockMultipartFile file(List<List<Object>> rows) {
        return new MockMultipartFile("file", "products.xlsx", XLSX_TYPE,
                ExcelTestFiles.xlsx(ProductExcelLayout.UPLOAD_HEADERS, rows));
    }

    private void createProduct(String code, String barcode) throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productCode", code, "name", "기존 상품", "barcode", barcode,
                                "category", "냉동식품", "orderUnit", "BOX", "unitPrice", 10000))))
                .andExpect(status().isCreated());
    }

    private long countByKeyword(String keyword) throws Exception {
        String response = mockMvc.perform(get("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .param("keyword", keyword))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("page").path("totalElements").asLong();
    }

    @Test
    @DisplayName("등록/수정 혼합 업로드 — 품목코드 매칭으로 구분해 전량 반영한다 (3.3.2)")
    void uploadsMixedCreateAndUpdate() throws Exception {
        createProduct("P-0001", "8801111111111");

        mockMvc.perform(multipart("/api/v1/products/excel")
                        .file(file(List.of(
                                row("P-0001", "이름 변경됨", "8801111111111", 15000),
                                row("P-0002", "신규 상품", "8802222222222", 9000))))
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRows").value(2))
                .andExpect(jsonPath("$.data.created").value(1))
                .andExpect(jsonPath("$.data.updated").value(1));

        mockMvc.perform(get("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .param("keyword", "P-0001"))
                .andExpect(jsonPath("$.data.items[0].name").value("이름 변경됨"))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(15000));
        assertThat(countByKeyword("P-0002")).isEqualTo(1);
    }

    @Test
    @DisplayName("all-or-nothing — 오류 1행이면 400 + 행 번호 리포트, 정상 행도 반영되지 않는다 (US-CAT-02 인수 조건)")
    void rejectsWholeFileOnAnyRowError() throws Exception {
        mockMvc.perform(multipart("/api/v1/products/excel")
                        .file(file(List.of(
                                row("P-0003", "정상 행", "8803333333333", 12000),
                                row("P-0004", "단가 오류 행", "8804444444444", "만원"))))
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("EXCEL_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details[0].row").value(3))
                .andExpect(jsonPath("$.error.details[0].field").value("unitPrice"))
                .andExpect(jsonPath("$.error.details[0].reason").isNotEmpty());

        // 정상 행(P-0003)도 반영되지 않았다
        assertThat(countByKeyword("P-0003")).isZero();
    }

    @Test
    @DisplayName("다운로드 — 왕복 호환 레이아웃(9열)·품목코드 오름차순, 재업로드하면 전량 수정 처리된다 (3.3.3)")
    void downloadRoundTrip() throws Exception {
        createProduct("P-0002", "8802222222222");
        createProduct("P-0001", "8801111111111");
        byte[] bytes = download();

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();
            assertThat(fmt.formatCellValue(sheet.getRow(0).getCell(8))).isEqualTo("상태");
            // 품목코드 오름차순 고정
            assertThat(fmt.formatCellValue(sheet.getRow(1).getCell(0))).isEqualTo("P-0001");
            assertThat(fmt.formatCellValue(sheet.getRow(2).getCell(0))).isEqualTo("P-0002");
            assertThat(fmt.formatCellValue(sheet.getRow(1).getCell(6))).isEqualTo("N");
            assertThat(fmt.formatCellValue(sheet.getRow(1).getCell(8))).isEqualTo("판매중");
        }

        // 다운로드 파일을 그대로 재업로드 — 전량 수정 처리 (읽기 전용 열은 무시)
        mockMvc.perform(multipart("/api/v1/products/excel")
                        .file(new MockMultipartFile("file", "download.xlsx", XLSX_TYPE, bytes))
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.created").value(0))
                .andExpect(jsonPath("$.data.updated").value(2));
    }

    @Test
    @DisplayName("점주는 업로드·다운로드 모두 403 (3.2 접근 매트릭스 — excel은 HQ 전용)")
    void storeOwnerForbidden() throws Exception {
        mockMvc.perform(multipart("/api/v1/products/excel")
                        .file(file(List.of(row("P-0001", "상품", "8801111111111", 1000))))
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.owner())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/products/excel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.owner())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("1,000행 업로드 처리 시간 측정 (US-CAT-02 — 수치는 백로그에 기록)")
    void uploadsThousandRowsWithinBudget() throws Exception {
        List<List<Object>> rows = new ArrayList<>();
        for (int i = 1; i <= 1_000; i++) {
            rows.add(row("P-%04d".formatted(i), "상품 " + i, "880%010d".formatted(i), 1000 + i));
        }

        long start = System.nanoTime();
        mockMvc.perform(multipart("/api/v1/products/excel")
                        .file(file(rows))
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.created").value(1000));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        System.out.printf("[성능] 엑셀 1,000행 업로드(신규 insert): %d ms%n", elapsedMs);
        assertThat(elapsedMs).as("1,000행 동기 처리 예산(10초)").isLessThan(10_000);
    }

    private byte[] download() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/products/excel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin())))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentType()).isEqualTo(XLSX_TYPE);
        assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment").contains(".xlsx");
        return result.getResponse().getContentAsByteArray();
    }
}
