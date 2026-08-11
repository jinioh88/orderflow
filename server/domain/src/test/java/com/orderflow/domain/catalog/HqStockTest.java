package com.orderflow.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class HqStockTest {

    /** 저장된 상품 흉내 — HqStock.create는 영속 상품(id 존재)만 받는다 */
    private Product persistedProduct() {
        Product product = Product.register(1L, "P-0001", "김치만두 1kg", "8801234567890",
                "냉동식품", "BOX", new BigDecimal("12000"));
        ReflectionTestUtils.setField(product, "id", 100L);
        return product;
    }

    @Test
    @DisplayName("테넌트는 상품에서 복사된다 — (tenantId, productId) 불일치 원천 차단 (CAT-1 리뷰)")
    void tenantIsCopiedFromProduct() {
        HqStock stock = HqStock.create(persistedProduct(), 100);

        assertThat(stock.getTenantId()).isEqualTo(1L);
        assertThat(stock.getProductId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("저장되지 않은 상품으로는 가용 재고를 만들 수 없다")
    void createRequiresPersistedProduct() {
        Product unsaved = Product.register(1L, "P-0001", "김치만두 1kg", "8801234567890",
                "냉동식품", "BOX", new BigDecimal("12000"));

        assertThatIllegalArgumentException().isThrownBy(() ->
                HqStock.create(unsaved, 100));
    }

    @Test
    @DisplayName("가용 재고는 0 이상으로 생성된다 — 음수 생성 불가 (절대 불변식, 04 §2.2)")
    void createRejectsNegativeQty() {
        assertThat(HqStock.create(persistedProduct(), 0).getAvailableQty()).isZero();

        assertThatIllegalArgumentException().isThrownBy(() ->
                HqStock.create(persistedProduct(), -1));
    }

    @Test
    @DisplayName("가용 재고 설정은 절대값 교체다 — 음수 거부 (api-spec 3.2.8)")
    void changeAvailableQtyIsAbsoluteAndNonNegative() {
        HqStock stock = HqStock.create(persistedProduct(), 100);

        stock.changeAvailableQty(250);
        assertThat(stock.getAvailableQty()).isEqualTo(250);

        assertThatIllegalArgumentException().isThrownBy(() ->
                stock.changeAvailableQty(-1));
    }
}
