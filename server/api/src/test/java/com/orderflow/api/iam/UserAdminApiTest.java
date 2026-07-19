package com.orderflow.api.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderflow.api.support.ApiIntegrationTest;
import com.orderflow.domain.iam.User;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * US-AUTH-02 — 점주 계정 등록/목록/비활성화/임시 비밀번호 재발급 (api-spec 2.4.9~2.4.12)
 */
class UserAdminApiTest extends ApiIntegrationTest {

    private String registerBody(Long storeId, String email) {
        return objectMapper.writeValueAsString(Map.of("storeId", storeId, "email", email, "name", "박점주"));
    }

    private String loginBody(String email, String password) {
        return objectMapper.writeValueAsString(Map.of("email", email, "password", password));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dataOf(String responseBody) {
        return (Map<String, Object>) objectMapper.readValue(responseBody, Map.class).get("data");
    }

    @Test
    @DisplayName("점주 등록 — 임시 비밀번호 1회 노출, 그 비밀번호로 로그인하면 설정 필요 상태다")
    void registerOwnerWithTemporaryPassword() throws Exception {
        var setup = createTenantSetup("bonjuk");

        String body = mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(setup.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(setup.store().getId(), "new-owner@test.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("STORE_OWNER"))
                .andExpect(jsonPath("$.data.passwordSetupRequired").value(true))
                .andReturn().getResponse().getContentAsString();
        String temporaryPassword = (String) dataOf(body).get("temporaryPassword");
        assertThat(temporaryPassword).hasSize(12);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("new-owner@test.com", temporaryPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passwordSetupRequired").value(true));
    }

    @Test
    @DisplayName("이메일 전역 중복(타 테넌트 포함) 409, 비활성 가맹점 등록 409 STORE_INACTIVE")
    void registerGuards() throws Exception {
        var tenantA = createTenantSetup("bonjuk");
        var tenantB = createTenantSetup("kimbap");

        // 타 테넌트에 이미 존재하는 이메일
        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tenantA.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(tenantA.store().getId(), tenantB.owner().getEmail())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_DUPLICATED"));

        // 비활성 가맹점
        mockMvc.perform(post("/api/v1/stores/{id}/deactivate", tenantA.store().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tenantA.admin())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tenantA.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(tenantA.store().getId(), "another@test.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("STORE_INACTIVE"));
    }

    @Test
    @DisplayName("계정 비활성화 — 로그인 즉시 차단, 자기 자신은 409")
    void deactivateUser() throws Exception {
        var setup = createTenantSetup("bonjuk");

        mockMvc.perform(post("/api/v1/users/{id}/deactivate", setup.owner().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(setup.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(setup.owner().getEmail(), PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_INACTIVE"));

        mockMvc.perform(post("/api/v1/users/{id}/deactivate", setup.admin().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(setup.admin())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("임시 비밀번호 재발급 — 기존 비밀번호 무효, 새 임시 비밀번호로 로그인, 임시 상태 복귀")
    void reissueTemporaryPassword() throws Exception {
        var setup = createTenantSetup("bonjuk");

        String body = mockMvc.perform(post("/api/v1/users/{id}/temporary-password", setup.owner().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(setup.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newTemp = (String) dataOf(body).get("temporaryPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(setup.owner().getEmail(), PASSWORD)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(setup.owner().getEmail(), newTemp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passwordSetupRequired").value(true));

        // 자기 자신 재발급 금지
        mockMvc.perform(post("/api/v1/users/{id}/temporary-password", setup.admin().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(setup.admin())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("SYSTEM의 재발급은 HQ_ADMIN 대상만 — 점주 대상은 403")
    void systemReissueOnlyForHqAdmin() throws Exception {
        var setup = createTenantSetup("bonjuk");
        User system = createSystemAccount();

        mockMvc.perform(post("/api/v1/users/{id}/temporary-password", setup.admin().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(system)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.temporaryPassword").isNotEmpty());

        mockMvc.perform(post("/api/v1/users/{id}/temporary-password", setup.owner().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(system)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("계정 목록 필터(storeId·role) — HQ_MANAGER는 조회 가능, 등록은 403 (NFR-2.3)")
    void listAndManagerAuthorization() throws Exception {
        var setup = createTenantSetup("bonjuk");
        User manager = saveUser(withConfirmedPassword(
                User.registerHqManager(setup.tenant().getId(), "manager@test.com", encodedPassword(), "운영자")));

        mockMvc.perform(get("/api/v1/users")
                        .param("role", "STORE_OWNER")
                        .header(HttpHeaders.AUTHORIZATION, bearer(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].role").value("STORE_OWNER"))
                .andExpect(jsonPath("$.data.items[0].temporaryPassword").doesNotExist());

        mockMvc.perform(get("/api/v1/users")
                        .param("storeId", String.valueOf(setup.store().getId()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(setup.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1));

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(setup.store().getId(), "x@test.com")))
                .andExpect(status().isForbidden());
    }

    private User withConfirmedPassword(User user) {
        user.confirmPassword(encodedPassword());
        return user;
    }
}
