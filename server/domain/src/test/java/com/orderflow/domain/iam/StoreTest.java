package com.orderflow.domain.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderflow.domain.common.InvalidStateTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StoreTest {

    @Test
    @DisplayName("등록된 가맹점은 활성 상태로 시작한다")
    void registeredStoreStartsActive() {
        Store store = Store.register(1L, "강남역점", "서울 강남구");

        assertThat(store.isActive()).isTrue();
    }

    @Test
    @DisplayName("테넌트 없이 가맹점을 만들 수 없다")
    void storeRequiresTenant() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                Store.register(null, "강남역점", null));
    }

    @Test
    @DisplayName("이미 비활성인 가맹점을 다시 비활성화하면 도메인 예외가 난다 (409 CONFLICT)")
    void deactivateTwiceThrows() {
        Store store = Store.register(1L, "강남역점", null);
        store.deactivate();

        assertThatThrownBy(store::deactivate)
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
