package com.orderflow.api.catalog.excel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 행 단위 검증 (US-CAT-02, api-spec 3.3.2 검증 규칙 표).
 * 파일 내 중복(먼저 나온 행 번호 명시)과 DB 충돌(다른 상품의 바코드)을 구분한다 —
 * DB의 기존 품목코드는 충돌이 아니라 수정 대상이다.
 * DB 조회는 호출자가 벌크로 끝내서 넘긴다 (existingCodes·existingBarcodeOwners) — 행당 쿼리 금지.
 * 품목코드·바코드 비교는 {@link #caseKey}로 정규화한다 — DB 유니크가 대소문자 무시(ai_ci)라
 * Java 비교가 대소문자를 구분하면 "p-0001"이 신규로 분류돼 유니크 충돌 500이 난다 (CAT-3 리뷰).
 */
@Component
public class ProductExcelValidator {

    /** 품목코드·바코드 비교 키 — DB 콜레이션(utf8mb4_0900_ai_ci)의 대소문자 무시와 일치시킨다 */
    public static String caseKey(String value) {
        return value.toUpperCase(Locale.ROOT);
    }

    /**
     * @param existingCodes         테넌트에 이미 존재하는 품목코드 (수정 대상 판별) — {@link #caseKey} 정규화 키
     * @param existingBarcodeOwners 테넌트 기존 바코드 → 소유 상품 품목코드 — 키·값 모두 {@link #caseKey} 정규화
     */
    public ProductExcelValidation validate(List<ProductExcelRow> rows,
                                           Set<String> existingCodes,
                                           Map<String, String> existingBarcodeOwners) {
        List<ProductUpsertCommand> commands = new ArrayList<>();
        List<ExcelRowError> errors = new ArrayList<>();
        Map<String, Integer> seenCodes = new HashMap<>();
        Map<String, Integer> seenBarcodes = new HashMap<>();

        for (ProductExcelRow row : rows) {
            int before = errors.size();
            validateFields(row, errors);
            validateDuplicates(row, seenCodes, seenBarcodes, existingBarcodeOwners, errors);
            if (errors.size() == before) {
                commands.add(new ProductUpsertCommand(row.rowNum(),
                        !existingCodes.contains(caseKey(row.productCode())), row.productCode(), row.name(),
                        row.barcode(), row.category(), row.orderUnit(), new BigDecimal(row.unitPrice())));
            }
        }
        return new ProductExcelValidation(rows.size(), List.copyOf(commands), List.copyOf(errors));
    }

    /** 필수값·길이·단가 형식 (제약은 api-spec 3.1과 동일) */
    private void validateFields(ProductExcelRow row, List<ExcelRowError> errors) {
        requireText(row, "productCode", row.productCode(), 30, errors);
        requireText(row, "name", row.name(), 100, errors);
        requireText(row, "barcode", row.barcode(), 30, errors);
        requireText(row, "category", row.category(), 50, errors);
        requireText(row, "orderUnit", row.orderUnit(), 20, errors);
        validateUnitPrice(row, errors);
    }

    private void requireText(ProductExcelRow row, String field, String value, int maxLength,
                             List<ExcelRowError> errors) {
        if (value == null || value.isBlank()) {
            errors.add(new ExcelRowError(row.rowNum(), field, "필수 입력입니다."));
        } else if (value.length() > maxLength) {
            errors.add(new ExcelRowError(row.rowNum(), field, "최대 %d자를 초과했습니다.".formatted(maxLength)));
        }
    }

    private void validateUnitPrice(ProductExcelRow row, List<ExcelRowError> errors) {
        if (row.unitPrice() == null || row.unitPrice().isBlank()) {
            errors.add(new ExcelRowError(row.rowNum(), "unitPrice", "필수 입력입니다."));
            return;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(row.unitPrice());
        } catch (NumberFormatException e) {
            errors.add(new ExcelRowError(row.rowNum(), "unitPrice", "단가는 0 이상의 정수여야 합니다."));
            return;
        }
        if (price.signum() < 0 || price.stripTrailingZeros().scale() > 0) {
            errors.add(new ExcelRowError(row.rowNum(), "unitPrice", "단가는 0 이상의 정수여야 합니다."));
        } else if (price.precision() - price.scale() > 13) {
            errors.add(new ExcelRowError(row.rowNum(), "unitPrice", "단가가 허용 범위를 초과했습니다."));
        }
    }

    /** 파일 내 중복(먼저 나온 행 번호 명시) + DB 바코드 충돌 (api-spec 3.3.2) */
    private void validateDuplicates(ProductExcelRow row,
                                    Map<String, Integer> seenCodes,
                                    Map<String, Integer> seenBarcodes,
                                    Map<String, String> existingBarcodeOwners,
                                    List<ExcelRowError> errors) {
        if (!row.productCode().isBlank()) {
            Integer firstRow = seenCodes.putIfAbsent(caseKey(row.productCode()), row.rowNum());
            if (firstRow != null) {
                errors.add(new ExcelRowError(row.rowNum(), "productCode",
                        "파일 내 품목코드가 중복됩니다 (%d행과 동일).".formatted(firstRow)));
            }
        }
        if (!row.barcode().isBlank()) {
            Integer firstRow = seenBarcodes.putIfAbsent(caseKey(row.barcode()), row.rowNum());
            if (firstRow != null) {
                errors.add(new ExcelRowError(row.rowNum(), "barcode",
                        "파일 내 바코드가 중복됩니다 (%d행과 동일).".formatted(firstRow)));
            }
            String owner = existingBarcodeOwners.get(caseKey(row.barcode()));
            if (owner != null && !owner.equals(caseKey(row.productCode()))) {
                errors.add(new ExcelRowError(row.rowNum(), "barcode", "다른 상품이 사용 중인 바코드입니다."));
            }
        }
    }
}
