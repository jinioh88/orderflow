package com.orderflow.api.iam;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderflow.api.support.ApiIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * US-AUTH-04 인수 조건 — API 레벨 테넌트 교차 접근은 전부 404 (403이 아님 — 존재 비노출, NFR-2.1/2.4).
 * 2-테넌트 픽스처는 ApiIntegrationTest.createTenantSetup 재사용 — CAT·ORD 교차 테스트도 같은 패턴을 쓴다.
 */
class CrossTenantApiTest extends ApiIntegrationTest {

    private TenantSetup tenantA;
    private TenantSetup tenantB;

    @BeforeEach
    void setUpTwoTenants() {
        tenantA = createTenantSetup("bonjuk");
        tenantB = createTenantSetup("kimbap");
    }

    @Test
    @DisplayName("타 테넌트 가맹점을 ID로 지정해 수정(비활성화)하면 404")
    void crossTenantStoreMutation() throws Exception {
        mockMvc.perform(post("/api/v1/stores/{id}/deactivate", tenantB.store().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tenantA.admin())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("타 테넌트 계정 비활성화·임시 비밀번호 재발급도 404")
    void crossTenantUserMutation() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/deactivate", tenantB.owner().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tenantA.admin())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/users/{id}/temporary-password", tenantB.owner().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tenantA.admin())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("타 테넌트 가맹점에 점주 등록을 시도하면 404 — 가맹점 존재 자체가 보이지 않는다")
    void crossTenantOwnerRegistration() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tenantA.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "storeId", tenantB.store().getId(),
                                "email", "invader@test.com",
                                "name", "침입자"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("목록 조회는 자기 테넌트만 — 타 테넌트 storeId 필터는 빈 결과")
    void listScopedToOwnTenant() throws Exception {
        mockMvc.perform(get("/api/v1/stores").header(HttpHeaders.AUTHORIZATION, bearer(tenantA.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(tenantA.store().getId()));

        mockMvc.perform(get("/api/v1/users")
                        .param("storeId", String.valueOf(tenantB.store().getId()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(tenantA.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }
}
