package com.orderflow.domain.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("미설정 상태의 필터 파라미터는 fail-closed 센티널이다")
    void sentinelWhenNotSet() {
        assertThat(TenantContext.tenantIdOrSentinel()).isEqualTo(TenantFilter.NO_TENANT);
    }

    @Test
    @DisplayName("테넌트 설정 후 파라미터는 해당 ID다")
    void tenantIdWhenSet() {
        TenantContext.setTenant(7L);

        assertThat(TenantContext.tenantIdOrSentinel()).isEqualTo(7L);
        assertThat(TenantContext.isUnfiltered()).isFalse();
    }

    @Test
    @DisplayName("이미 설정된 상태에서 다시 설정하면 누수 의심으로 즉시 실패한다")
    void doubleSetThrows() {
        TenantContext.setTenant(7L);

        assertThatIllegalStateException().isThrownBy(() -> TenantContext.setTenant(8L));
        assertThatIllegalStateException().isThrownBy(TenantContext::setUnfiltered);
    }

    @Test
    @DisplayName("UNFILTERED 상태에서 테넌트 ID를 요구하면 버그로 보고 실패한다")
    void unfilteredHasNoTenantId() {
        TenantContext.setUnfiltered();

        assertThat(TenantContext.isUnfiltered()).isTrue();
        assertThatIllegalStateException().isThrownBy(TenantContext::tenantIdOrSentinel);
    }

    @Test
    @DisplayName("runUnfiltered는 블록 종료 시 이전 컨텍스트를 복원한다")
    void runUnfilteredRestoresPreviousScope() {
        TenantContext.setTenant(7L);

        Boolean insideUnfiltered = TenantContext.runUnfiltered(TenantContext::isUnfiltered);

        assertThat(insideUnfiltered).isTrue();
        assertThat(TenantContext.tenantIdOrSentinel()).isEqualTo(7L);
    }

    @Test
    @DisplayName("runUnfiltered는 미설정 상태에서 시작하면 미설정으로 되돌린다")
    void runUnfilteredRestoresNone() {
        TenantContext.runUnfiltered(() -> null);

        assertThat(TenantContext.isUnfiltered()).isFalse();
        assertThat(TenantContext.tenantIdOrSentinel()).isEqualTo(TenantFilter.NO_TENANT);
    }
}
