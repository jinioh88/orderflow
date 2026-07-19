package com.orderflow.domain.iam;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TenantTest {

    @Test
    @DisplayName("마감 시각을 지정하지 않으면 12:00이다 (api-spec 2.4.1)")
    void defaultCutoffTime() {
        Tenant tenant = Tenant.register("본죽F&B", null);

        assertThat(tenant.getCutoffTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(tenant.isActive()).isTrue();
    }

    @Test
    @DisplayName("마감 시각을 지정하면 그대로 저장된다")
    void customCutoffTime() {
        Tenant tenant = Tenant.register("본죽F&B", LocalTime.of(14, 30));

        assertThat(tenant.getCutoffTime()).isEqualTo(LocalTime.of(14, 30));
    }
}
