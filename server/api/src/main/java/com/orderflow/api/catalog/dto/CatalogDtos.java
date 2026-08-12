package com.orderflow.api.catalog.dto;

import com.orderflow.api.common.response.KstTimes;
import com.orderflow.domain.catalog.Product;
import com.orderflow.domain.catalog.ProductStatus;
import com.orderflow.infra.catalog.ProductSummary;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * CAT API DTO (api-spec 3.1·3.2) — 단가는 KRW 정수 JSON number (공통 규약 1.1),
 * 서버 내부는 BigDecimal (NFR-3.5).
 */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record ProductCreateRequest(
            @NotBlank @Size(max = 30) String productCode,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 30) String barcode,
            @NotBlank @Size(max = 50) String category,
            @NotBlank @Size(max = 20) String orderUnit,
            @NotNull @Min(0) @Digits(integer = 13, fraction = 0) Long unitPrice) {
    }

    /** 전체 교체 수정 — productCode는 수정 불가라 본문에 없다 (api-spec 3.2.4) */
    public record ProductUpdateRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 30) String barcode,
            @NotBlank @Size(max = 50) String category,
            @NotBlank @Size(max = 20) String orderUnit,
            @NotNull @Min(0) @Digits(integer = 13, fraction = 0) Long unitPrice) {
    }

    /** 한정 품목 지정(3.2.6)·가용 재고 설정(3.2.8) 공용 본문 */
    public record AvailableQtyRequest(@NotNull @Min(0) Integer availableQty) {
    }

    /** 상품 객체 (api-spec 3.1) — availableQty는 한정 품목만 값, 아니면 null */
    public record ProductResponse(
            Long id, String productCode, String name, String barcode, String category,
            String orderUnit, long unitPrice, boolean limited, Integer availableQty,
            ProductStatus status, OffsetDateTime createdAt) {

        public static ProductResponse from(Product product, Integer availableQty) {
            return new ProductResponse(product.getId(), product.getProductCode(), product.getName(),
                    product.getBarcode(), product.getCategory(), product.getOrderUnit(),
                    toKrw(product.getUnitPrice()), product.isLimited(), availableQty,
                    product.getStatus(), KstTimes.toOffset(product.getCreatedAt()));
        }

        public static ProductResponse from(ProductSummary summary) {
            return new ProductResponse(summary.id(), summary.productCode(), summary.name(),
                    summary.barcode(), summary.category(), summary.orderUnit(),
                    toKrw(summary.unitPrice()), summary.limited(), summary.availableQty(),
                    summary.status(), KstTimes.toOffset(summary.createdAt()));
        }

        /** 쓰기 경로가 정수만 받으므로 소수부 없음이 보장된다 — 어긋나면 데이터 이상, 조용히 자르지 않는다 */
        private static long toKrw(BigDecimal unitPrice) {
            return unitPrice.longValueExact();
        }
    }

    public record HqStockResponse(Long productId, int availableQty) {
    }

    /** 엑셀 업로드 전량 반영 성공 응답 (api-spec 3.3.2) */
    public record ExcelUploadResponse(int totalRows, long created, long updated) {
    }
}
