package com.orderflow.api.iam;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderflow.api.support.ApiIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * US-AUTH-02 — 가맹점 등록/목록/비활성화 (api-spec 2.4.6~2.4.8)
 */
class StoreAdminApiTest extends ApiIntegrationTest {

    @Test
    @DisplayName("HQ_ADMIN이 가맹점을 등록하고 목록에서 확인한다 (페이징·status 필터)")
    void registerAndList() throws Exception {
        var setup = createTenantSetup("bonjuk");

        mockMvc.perform(post("/api/v1/stores")
                        .header(HttpHeaders.AUTHORIZATION, bearer(setup.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "강남역점", "address", "서울 강남구"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/stores")
                        .param("status", "ACTIVE")
                        .param("sort", "name,asc")
                        .header(HttpHeaders.AUTHORIZATION, bearer(setup.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.page.totalElements").value(2));
    }

    @Test
    @DisplayName("허용되지 않은 정렬 필드는 400 INVALID_REQUEST")
    void invalidSortField() throws Exception {
        var setup = createTenantSetup("bonjuk");

        mockMvc.perform(get("/api/v1/stores")
                        .param("sort", "password,asc")
                        .header(HttpHeaders.AUTHORIZATION, bearer(setup.admin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("가맹점 비활성화 — 재차 비활성화는 409, 소속 점주의 로그인·리프레시가 차단된다")
    void deactivateBlocksStoreMembers() throws Exception {
        var setup = createTenantSetup("bonjuk");

        // 점주가 먼저 로그인해 리프레시 토큰 확보
        String loginBody = objectMapper.writeValueAsString(
                Map.of("email", setup.owner().getEmail(), "password", PASSWORD));
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<?, ?> loginData = (Map<?, ?>) objectMapper.readValue(loginResponse, Map.class).get("data");
        String refreshToken = (String) loginData.get("refreshToken");

        mockMvc.perform(post("/api/v1/stores/{id}/deactivate", setup.store().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(setup.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(post("/api/v1/stores/{id}/deactivate", setup.store().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(setup.admin())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));

        // 비활성 가맹점 사용자의 로그인 차단 (도메인 불변식)
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_INACTIVE"));

        // 리프레시 토큰 무효화 (api-spec 2.2 무효화 이벤트)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("STORE_OWNER는 가맹점 관리 API에 접근할 수 없다 — 403 (NFR-2.3)")
    void storeOwnerForbidden() throws Exception {
        var setup = createTenantSetup("bonjuk");

        mockMvc.perform(post("/api/v1/stores")
                        .header(HttpHeaders.AUTHORIZATION, bearer(setup.owner()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "몰래낸지점"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/stores").header(HttpHeaders.AUTHORIZATION, bearer(setup.owner())))
                .andExpect(status().isForbidden());
    }
}
