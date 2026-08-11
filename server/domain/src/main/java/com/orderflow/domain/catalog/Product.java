package com.orderflow.domain.catalog;

import com.orderflow.common.error.BusinessException;
import com.orderflow.common.error.CatalogErrorCode;
import com.orderflow.domain.common.BaseEntity;
import com.orderflow.domain.common.InvalidStateTransitionException;
import com.orderflow.domain.common.TenantFilter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.springframework.util.Assert;

/**
 * 상품 — 원자재 카탈로그 애그리거트 (04 §2.2, US-CAT-01·04).
 * 불변식: 품목코드·바코드는 테넌트 내 유일(DB 유니크가 안전망), 품목코드는 등록 후 수정 불가,
 * 단가는 0 이상. 판매중지 품목은 신규 발주 라인에 담을 수 없다 (강제는 ORD 에픽에서).
 */
@Entity
@Table(name = "product", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_tenant_code", columnNames = {"tenant_id", "product_code"}),
        @UniqueConstraint(name = "uk_product_tenant_barcode", columnNames = {"tenant_id", "barcode"})
})
@Filter(name = TenantFilter.NAME, condition = TenantFilter.CONDITION)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /** 품목코드 — 엑셀 업로드 매칭 키·발주 라인 스냅샷 기준. 등록 후 수정 불가 (api-spec 3.1) */
    @Column(name = "product_code", nullable = false, length = 30)
    private String productCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String barcode;

    /** MVP는 단순 문자열 — 계층 카테고리 미도입 (erd 3.2) */
    @Column(nullable = false, length = 50)
    private String category;

    /** 발주 단위 (BOX, EA, KG …) — 자유 문자열 */
    @Column(name = "order_unit", nullable = false, length = 20)
    private String orderUnit;

    /** 현재 단가 — 주문 시 발주 라인에 스냅샷된다 (04 §2.3) */
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    /** 한정 품목 여부 — 지정 시 HqStock 레코드가 함께 존재해야 한다 (04 §2.2) */
    @Column(name = "is_limited", nullable = false)
    private boolean limited;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    private Product(Long tenantId, String productCode, String name, String barcode,
                    String category, String orderUnit, BigDecimal unitPrice) {
        Assert.notNull(tenantId, "상품은 테넌트 소속이어야 한다");
        Assert.hasText(productCode, "품목코드는 필수다");
        requireCatalogAttributes(name, barcode, category, orderUnit, unitPrice);
        this.tenantId = tenantId;
        this.productCode = productCode;
        this.name = name;
        this.barcode = barcode;
        this.category = category;
        this.orderUnit = orderUnit;
        this.unitPrice = unitPrice;
        this.limited = false;
        this.status = ProductStatus.ON_SALE;
    }

    /** 상품 등록 (US-CAT-01, api-spec 3.2.1) — 등록 직후 판매중, 한정 품목 아님. */
    public static Product register(Long tenantId, String productCode, String name, String barcode,
                                   String category, String orderUnit, BigDecimal unitPrice) {
        return new Product(tenantId, productCode, name, barcode, category, orderUnit, unitPrice);
    }

    /** 상품 수정 (US-CAT-01, api-spec 3.2.4) — 전체 교체. 품목코드는 바꾸지 않는다. */
    public void update(String name, String barcode, String category, String orderUnit, BigDecimal unitPrice) {
        requireCatalogAttributes(name, barcode, category, orderUnit, unitPrice);
        this.name = name;
        this.barcode = barcode;
        this.category = category;
        this.orderUnit = orderUnit;
        this.unitPrice = unitPrice;
    }

    /** 판매중지 (US-CAT-01, api-spec 3.2.5) — 한정 품목 지정·가용 재고는 건드리지 않는다. */
    public void suspend() {
        if (status == ProductStatus.SUSPENDED) {
            throw new InvalidStateTransitionException("이미 판매중지된 상품입니다.");
        }
        this.status = ProductStatus.SUSPENDED;
    }

    /** 한정 품목 지정 (US-CAT-04, api-spec 3.2.6) — 호출자는 같은 트랜잭션에서 HqStock을 생성해야 한다. */
    public void designateLimited() {
        if (limited) {
            throw new BusinessException(CatalogErrorCode.ALREADY_LIMITED);
        }
        this.limited = true;
    }

    /** 한정 품목 해제 (US-CAT-04, api-spec 3.2.7) — 호출자는 같은 트랜잭션에서 HqStock을 삭제해야 한다. */
    public void releaseLimited() {
        if (!limited) {
            throw new BusinessException(CatalogErrorCode.NOT_LIMITED);
        }
        this.limited = false;
    }

    public boolean isOnSale() {
        return status == ProductStatus.ON_SALE;
    }

    private static void requireCatalogAttributes(String name, String barcode, String category,
                                                 String orderUnit, BigDecimal unitPrice) {
        Assert.hasText(name, "품명은 필수다");
        Assert.hasText(barcode, "바코드는 필수다");
        Assert.hasText(category, "카테고리는 필수다");
        Assert.hasText(orderUnit, "발주 단위는 필수다");
        Assert.notNull(unitPrice, "단가는 필수다");
        Assert.isTrue(unitPrice.signum() >= 0, "단가는 0 이상이어야 한다");
        // DECIMAL(15,2) 초과 값은 DB에서 조용히 반올림/실패한다 — 도메인에서 선제 차단 (CAT-1 리뷰)
        Assert.isTrue(unitPrice.scale() <= 2, "단가는 소수 둘째 자리까지만 허용된다");
        Assert.isTrue(unitPrice.precision() - unitPrice.scale() <= 13, "단가가 허용 범위를 초과했다");
    }
}
