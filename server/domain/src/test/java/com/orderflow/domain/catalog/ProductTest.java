package com.orderflow.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderflow.common.error.BusinessException;
import com.orderflow.common.error.CatalogErrorCode;
import com.orderflow.domain.common.InvalidStateTransitionException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductTest {

    private Product product() {
        return Product.register(1L, "P-0001", "김치만두 1kg", "8801234567890",
                "냉동식품", "BOX", new BigDecimal("12000"));
    }

    @Test
    @DisplayName("등록된 상품은 판매중·한정 아님 상태로 시작한다 (api-spec 3.2.1)")
    void registeredProductStartsOnSaleAndNotLimited() {
        Product product = product();

        assertThat(product.isOnSale()).isTrue();
        assertThat(product.isLimited()).isFalse();
    }

    @Test
    @DisplayName("단가는 0 이상이어야 한다 — 음수 등록 불가")
    void unitPriceMustBeNonNegative() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                Product.register(1L, "P-0001", "김치만두 1kg", "8801234567890",
                        "냉동식품", "BOX", new BigDecimal("-1")));
    }

    @Test
    @DisplayName("단가는 DECIMAL(15,2) 범위를 넘을 수 없다 — 소수 셋째 자리·정수 14자리 거부 (CAT-1 리뷰)")
    void unitPriceMustFitDecimal15_2() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                Product.register(1L, "P-0001", "김치만두 1kg", "8801234567890",
                        "냉동식품", "BOX", new BigDecimal("12000.005")));

        assertThatIllegalArgumentException().isThrownBy(() ->
                Product.register(1L, "P-0001", "김치만두 1kg", "8801234567890",
                        "냉동식품", "BOX", new BigDecimal("10000000000000")));
    }

    @Test
    @DisplayName("수정은 품목코드를 제외한 카탈로그 속성을 전체 교체한다 (api-spec 3.2.4)")
    void updateReplacesCatalogAttributes() {
        Product product = product();

        product.update("김치만두 2kg", "8809999999999", "냉동만두", "EA", new BigDecimal("15000"));

        assertThat(product.getProductCode()).isEqualTo("P-0001");
        assertThat(product.getName()).isEqualTo("김치만두 2kg");
        assertThat(product.getUnitPrice()).isEqualByComparingTo("15000");
    }

    @Test
    @DisplayName("이미 판매중지된 상품을 다시 중지하면 도메인 예외가 난다 (409 CONFLICT)")
    void suspendTwiceThrows() {
        Product product = product();
        product.suspend();

        assertThatThrownBy(product::suspend)
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("판매중지는 한정 품목 지정을 건드리지 않는다 (api-spec 3.2.5)")
    void suspendKeepsLimitedFlag() {
        Product product = product();
        product.designateLimited();

        product.suspend();

        assertThat(product.isLimited()).isTrue();
    }

    @Test
    @DisplayName("이미 한정 품목인 상품의 재지정은 ALREADY_LIMITED (api-spec 3.2.6)")
    void designateLimitedTwiceThrows() {
        Product product = product();
        product.designateLimited();

        assertThatThrownBy(product::designateLimited)
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(CatalogErrorCode.ALREADY_LIMITED));
    }

    @Test
    @DisplayName("한정 품목이 아닌 상품의 해제는 NOT_LIMITED (api-spec 3.2.7)")
    void releaseWithoutDesignationThrows() {
        Product product = product();

        assertThatThrownBy(product::releaseLimited)
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(CatalogErrorCode.NOT_LIMITED));
    }

    @Test
    @DisplayName("지정 → 해제 → 재지정이 가능하다 (api-spec 3.2.7 — 수량은 새로 설정)")
    void redesignateAfterRelease() {
        Product product = product();
        product.designateLimited();
        product.releaseLimited();

        product.designateLimited();

        assertThat(product.isLimited()).isTrue();
    }
}
