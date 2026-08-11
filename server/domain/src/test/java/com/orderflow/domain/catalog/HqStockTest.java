package com.orderflow.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HqStockTest {

    @Test
    @DisplayName("가용 재고는 0 이상으로 생성된다 — 음수 생성 불가 (절대 불변식, 04 §2.2)")
    void createRejectsNegativeQty() {
        assertThat(HqStock.create(1L, 100L, 0).getAvailableQty()).isZero();

        assertThatIllegalArgumentException().isThrownBy(() ->
                HqStock.create(1L, 100L, -1));
    }

    @Test
    @DisplayName("가용 재고 설정은 절대값 교체다 — 음수 거부 (api-spec 3.2.8)")
    void changeAvailableQtyIsAbsoluteAndNonNegative() {
        HqStock stock = HqStock.create(1L, 100L, 100);

        stock.changeAvailableQty(250);
        assertThat(stock.getAvailableQty()).isEqualTo(250);

        assertThatIllegalArgumentException().isThrownBy(() ->
                stock.changeAvailableQty(-1));
    }
}
