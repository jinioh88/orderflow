package com.orderflow.api.system;

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
 * US-AUTH-01 — 테넌트 + 최초 본사 관리자 등록 (api-spec 2.4.1)
 */
class TenantRegistrationApiTest extends ApiIntegrationTest {

    private String body(String tenantName, String adminEmail) {
        return objectMapper.writeValueAsString(Map.of(
                "name", tenantName,
                "admin", Map.of("email", adminEmail, "name", "김운영")));
    }

    @Test
    @DisplayName("SYSTEM은 테넌트와 최초 관리자를 원자적으로 등록하고 임시 비밀번호를 1회 받는다")
    void systemRegistersTenantWithFirstAdmin() throws Exception {
        var system = createSystemAccount();

        mockMvc.perform(post("/api/v1/system/tenants")
                        .header(HttpHeaders.AUTHORIZATION, bearer(system))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("본죽F&B", "admin@bonjuk.co.kr")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tenant.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.tenant.cutoffTime").value("12:00"))
                .andExpect(jsonPath("$.data.admin.role").value("HQ_ADMIN"))
                .andExpect(jsonPath("$.data.admin.temporaryPassword").isNotEmpty());
    }

    @Test
    @DisplayName("이메일이 전역 중복이면 409 EMAIL_DUPLICATED — 다른 테넌트의 계정과도 충돌한다")
    void duplicateEmailAcrossTenants() throws Exception {
        var system = createSystemAccount();
        var existing = createTenantSetup("kimbap"); // kimbap-admin@test.com 존재

        mockMvc.perform(post("/api/v1/system/tenants")
                        .header(HttpHeaders.AUTHORIZATION, bearer(system))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("신규 본사", existing.admin().getEmail())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_DUPLICATED"));
    }

    @Test
    @DisplayName("SYSTEM이 아닌 역할은 403, 익명은 401 — 접근 매트릭스 (NFR-2.3)")
    void onlySystemRoleAllowed() throws Exception {
        var setup = createTenantSetup("bonjuk");

        mockMvc.perform(post("/api/v1/system/tenants")
                        .header(HttpHeaders.AUTHORIZATION, bearer(setup.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("새 본사", "new@test.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/system/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("새 본사", "new@test.com")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
