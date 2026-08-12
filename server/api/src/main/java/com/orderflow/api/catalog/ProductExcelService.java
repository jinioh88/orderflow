package com.orderflow.api.catalog;

import com.orderflow.api.auth.AuthenticatedUser;
import com.orderflow.api.catalog.dto.CatalogDtos.ExcelUploadResponse;
import com.orderflow.api.catalog.excel.ExcelValidationException;
import com.orderflow.api.catalog.excel.ProductExcelParser;
import com.orderflow.api.catalog.excel.ProductExcelRow;
import com.orderflow.api.catalog.excel.ProductExcelValidation;
import com.orderflow.api.catalog.excel.ProductExcelValidator;
import com.orderflow.api.catalog.excel.ProductUpsertCommand;
import com.orderflow.domain.catalog.Product;
import com.orderflow.domain.catalog.ProductRepository;
import com.orderflow.infra.catalog.ProductQueryRepository;
import com.orderflow.infra.catalog.ProductSearchCondition;
import com.orderflow.infra.catalog.ProductSummary;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 엑셀 업로드/다운로드 유스케이스 (US-CAT-02·03, api-spec 3.3).
 * 업로드는 단일 트랜잭션 all-or-nothing — 오류 1건이라도 있으면 예외로 전체 롤백된다.
 */
@Service
@RequiredArgsConstructor
public class ProductExcelService {

    private final ProductExcelParser parser;
    private final ProductExcelValidator validator;
    private final ProductRepository productRepository;
    private final ProductQueryRepository productQueryRepository;

    @Transactional
    public ExcelUploadResponse upload(AuthenticatedUser principal, InputStream file) {
        List<ProductExcelRow> rows = parser.parse(file);
        Map<String, Product> existingByCode = existingByProductCode(rows);
        ProductExcelValidation validation = validator.validate(
                rows, existingByCode.keySet(), existingBarcodeOwners(rows));
        if (validation.hasErrors()) {
            throw new ExcelValidationException(validation.errors());
        }
        for (ProductUpsertCommand command : validation.commands()) {
            if (command.isNew()) {
                productRepository.save(Product.register(principal.tenantId(), command.productCode(),
                        command.name(), command.barcode(), command.category(),
                        command.orderUnit(), command.unitPrice()));
            } else {
                // 기존 상품은 더티 체킹으로 갱신 — 상태·한정 지정은 업로드로 바뀌지 않는다 (api-spec 3.3.2)
                existingByCode.get(ProductExcelValidator.caseKey(command.productCode()))
                        .update(command.name(), command.barcode(),
                                command.category(), command.orderUnit(), command.unitPrice());
            }
        }
        return new ExcelUploadResponse(
                validation.totalRows(), validation.createdCount(), validation.updatedCount());
    }

    /** 다운로드 데이터 — 목록과 같은 조건, 품목코드 오름차순 전체 (api-spec 3.3.3) */
    @Transactional(readOnly = true)
    public List<ProductSummary> findAllForDownload(ProductSearchCondition condition) {
        return productQueryRepository.findAllForExcel(condition);
    }

    /**
     * 행당 쿼리 금지 — 파일의 품목코드·바코드를 벌크로 한 번에 조회한다.
     * 맵 키는 caseKey 정규화 — DB 조회(ai_ci)는 대소문자 무시로 매칭되므로 Java 측 분류도 맞춘다.
     */
    private Map<String, Product> existingByProductCode(List<ProductExcelRow> rows) {
        Set<String> codes = nonBlank(rows, ProductExcelRow::productCode);
        return codes.isEmpty() ? Map.of() : productRepository.findAllByProductCodeIn(codes).stream()
                .collect(Collectors.toMap(
                        product -> ProductExcelValidator.caseKey(product.getProductCode()),
                        Function.identity()));
    }

    private Map<String, String> existingBarcodeOwners(List<ProductExcelRow> rows) {
        Set<String> barcodes = nonBlank(rows, ProductExcelRow::barcode);
        return barcodes.isEmpty() ? Map.of() : productRepository.findAllByBarcodeIn(barcodes).stream()
                .collect(Collectors.toMap(
                        product -> ProductExcelValidator.caseKey(product.getBarcode()),
                        product -> ProductExcelValidator.caseKey(product.getProductCode())));
    }

    private Set<String> nonBlank(List<ProductExcelRow> rows, Function<ProductExcelRow, String> extractor) {
        return rows.stream().map(extractor).filter(value -> !value.isBlank()).collect(Collectors.toSet());
    }
}
