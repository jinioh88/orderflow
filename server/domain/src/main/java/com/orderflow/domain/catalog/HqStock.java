package com.orderflow.domain.catalog;

import com.orderflow.domain.common.BaseEntity;
import com.orderflow.domain.common.TenantFilter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.springframework.util.Assert;

/**
 * 본사 가용 재고 — 한정 품목만 레코드 존재 (04 §2.2, D-1: 동시성 핫스팟을 Product에서 분리).
 * 절대 불변식: available_qty ≥ 0 — DB CHECK 제약이 최후 방어선 (NFR-3.1, erd 3.2).
 * Product와는 ID로만 참조한다 (같은 컨텍스트라도 애그리거트가 다르다 — 04 §1).
 * 차감(deduct)·복원(restore)은 M2 ORD 에픽에서 Redisson 락과 함께 추가한다.
 */
@Entity
@Table(name = "hq_stock", uniqueConstraints =
        @UniqueConstraint(name = "uk_hq_stock_product", columnNames = "product_id"))
@Filter(name = TenantFilter.NAME, condition = TenantFilter.CONDITION)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HqStock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "available_qty", nullable = false)
    private int availableQty;

    private HqStock(Long tenantId, Long productId, int availableQty) {
        Assert.notNull(tenantId, "가용 재고는 테넌트 소속이어야 한다");
        Assert.notNull(productId, "가용 재고는 상품에 속해야 한다");
        requireNonNegative(availableQty);
        this.tenantId = tenantId;
        this.productId = productId;
        this.availableQty = availableQty;
    }

    /** 한정 품목 지정 시 생성 (US-CAT-04, api-spec 3.2.6) — product당 1행. */
    public static HqStock create(Long tenantId, Long productId, int availableQty) {
        return new HqStock(tenantId, productId, availableQty);
    }

    /** 가용 재고 설정 — 절대값 교체, 증감 아님 (US-CAT-04, api-spec 3.2.8). */
    public void changeAvailableQty(int availableQty) {
        requireNonNegative(availableQty);
        this.availableQty = availableQty;
    }

    private static void requireNonNegative(int availableQty) {
        Assert.isTrue(availableQty >= 0, "가용 재고는 0 이상이어야 한다");
    }
}
