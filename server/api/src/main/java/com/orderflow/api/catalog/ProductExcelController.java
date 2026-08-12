package com.orderflow.api.catalog;

import com.orderflow.api.auth.AuthenticatedUser;
import com.orderflow.api.catalog.dto.CatalogDtos.ExcelUploadResponse;
import com.orderflow.api.catalog.excel.ExcelParseException;
import com.orderflow.api.catalog.excel.ProductExcelWriter;
import com.orderflow.api.common.response.ApiResponse;
import com.orderflow.api.common.response.KstTimes;
import com.orderflow.infra.catalog.ProductSearchCondition;
import com.orderflow.infra.catalog.ProductSummary;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 상품 엑셀 업로드/다운로드 (US-CAT-02·03, api-spec 3.3).
 * 다운로드는 JSON 래퍼를 쓰지 않는 스펙의 명시적 예외 — xlsx 바이너리 스트림.
 */
@RestController
@RequiredArgsConstructor
public class ProductExcelController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ProductExcelService productExcelService;
    private final ProductExcelWriter productExcelWriter;

    @PostMapping("/api/v1/products/excel")
    public ApiResponse<ExcelUploadResponse> upload(@AuthenticationPrincipal AuthenticatedUser principal,
                                                   @RequestParam("file") MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return ApiResponse.of(productExcelService.upload(principal, in));
        } catch (IOException e) {
            throw new ExcelParseException("업로드 파일을 읽을 수 없습니다.", e);
        }
    }

    /**
     * 응답을 byte[]로 확정해 내보낸다 — StreamingResponseBody는 비동기 재디스패치가
     * stateless 보안 체인(SecurityContext 미보존)과 충돌한다. 데이터가 10,000행 상한(3.3.1)으로
     * 캡되어 있어 메모리 확정이 안전하고, 워크북 생성은 SXSSF 스트리밍으로 낮은 메모리를 유지한다.
     */
    @GetMapping("/api/v1/products/excel")
    public ResponseEntity<byte[]> download(ProductSearchCondition condition) {
        List<ProductSummary> products = productExcelService.findAllForDownload(condition);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        productExcelWriter.write(products, out);
        String filename = "products-%s.xlsx".formatted(
                KstTimes.today().format(DateTimeFormatter.BASIC_ISO_DATE));
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(filename))
                .body(out.toByteArray());
    }
}
