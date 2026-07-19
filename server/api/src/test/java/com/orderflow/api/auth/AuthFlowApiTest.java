package com.orderflow.api.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderflow.api.auth.jwt.JwtProperties;
import com.orderflow.api.support.ApiIntegrationTest;
import com.orderflow.domain.iam.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * US-AUTH-03 — 로그인/재발급/로그아웃/비밀번호 설정 흐름 (api-spec 2.2~2.4)
 */
class AuthFlowApiTest extends ApiIntegrationTest {

    @Autowired
    private JwtProperties jwtProperties;

    private MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andReturn();
    }

    private String jsonAt(MvcResult result, String path) throws Exception {
        Map<?, ?> root = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Object cursor = root;
        for (String key : path.split("\\.")) {
            cursor = ((Map<?, ?>) cursor).get(key);
        }
        return String.valueOf(cursor);
    }

    @Test
    @DisplayName("로그인 성공 — 토큰 쌍과 사용자 요약, 임시 상태 아님")
    void loginSuccess() throws Exception {
        var setup = createTenantSetup("bonjuk");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", setup.owner().getEmail(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.accessTokenExpiresIn").value(1800))
                .andExpect(jsonPath("$.data.passwordSetupRequired").value(false))
                .andExpect(jsonPath("$.data.user.role").value("STORE_OWNER"))
                .andExpect(jsonPath("$.data.user.storeId").value(setup.store().getId()));
    }

    @Test
    @DisplayName("자격 증명 불일치는 원인 구분 없이 401 INVALID_CREDENTIALS")
    void invalidCredentials() throws Exception {
        var setup = createTenantSetup("bonjuk");

        login(setup.owner().getEmail(), "WrongPass1").getResponse();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", setup.owner().getEmail(), "password", "WrongPass1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "no-such@test.com", "password", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("비활성 계정 로그인은 403 ACCOUNT_INACTIVE")
    void inactiveAccountLogin() throws Exception {
        var setup = createTenantSetup("bonjuk");
        inUnfilteredTx(() ->
                userRepository.findById(setup.owner().getId()).orElseThrow().deactivate());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", setup.owner().getEmail(), "password", PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_INACTIVE"));
    }

    @Test
    @DisplayName("리프레시 회전 — 새 쌍 발급, 회전된 기존 토큰 재사용은 401 INVALID_REFRESH_TOKEN")
    void refreshRotation() throws Exception {
        var setup = createTenantSetup("bonjuk");
        String refreshToken = jsonAt(login(setup.owner().getEmail(), PASSWORD), "data.refreshToken");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("로그아웃하면 해당 리프레시 토큰이 무효화된다 (멱등 204)")
    void logoutRevokesRefreshToken() throws Exception {
        var setup = createTenantSetup("bonjuk");
        String refreshToken = jsonAt(login(setup.owner().getEmail(), PASSWORD), "data.refreshToken");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(setup.owner()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("만료된 액세스 토큰은 401 TOKEN_EXPIRED — 재발급 트리거 (api-spec 1.4)")
    void expiredAccessToken() throws Exception {
        var setup = createTenantSetup("bonjuk");
        String expired = Jwts.builder()
                .subject(String.valueOf(setup.admin().getId()))
                .claim("tenant_id", setup.tenant().getId())
                .claim("role", "HQ_ADMIN")
                .issuedAt(new Date(System.currentTimeMillis() - 60_000))
                .expiration(new Date(System.currentTimeMillis() - 30_000))
                .signWith(Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/v1/stores").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("TOKEN_EXPIRED"));
    }

    @Test
    @DisplayName("임시 비밀번호 흐름 — 로그인 표시 → 타 API 차단 → 설정 → 같은 액세스 토큰으로 즉시 정상 접근")
    void temporaryPasswordFlow() throws Exception {
        var setup = createTenantSetup("bonjuk");
        User tempAdmin = saveUser(User.registerHqAdmin(
                setup.tenant().getId(), "temp-admin@test.com", encodedPassword(), "신규관리자"));

        // 로그인 응답에 비밀번호 설정 필요 표시 (US-AUTH-03 인수 조건)
        MvcResult loginResult = login("temp-admin@test.com", PASSWORD);
        org.assertj.core.api.Assertions.assertThat(jsonAt(loginResult, "data.passwordSetupRequired"))
                .isEqualTo("true");
        String accessToken = jsonAt(loginResult, "data.accessToken");

        // 허용 목록 외 API 차단 (US-AUTH-02 인수 조건)
        mockMvc.perform(get("/api/v1/stores").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PASSWORD_SETUP_REQUIRED"));

        // 잘못된 현재 비밀번호 → 401
        mockMvc.perform(put("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", "WrongPass1", "newPassword", "MyNewPass1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));

        // 비밀번호 설정 성공 → 새 토큰 쌍
        mockMvc.perform(put("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", PASSWORD, "newPassword", "MyNewPass1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passwordSetupRequired").value(false))
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

        // 임시 상태는 요청 시점 상태 기준 — 같은 액세스 토큰으로 즉시 정상 접근 (api-spec 2.3)
        mockMvc.perform(get("/api/v1/stores").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 새 비밀번호로 재로그인 가능, 임시 상태 해제 확인
        org.assertj.core.api.Assertions.assertThat(
                jsonAt(login("temp-admin@test.com", "MyNewPass1"), "data.passwordSetupRequired"))
                .isEqualTo("false");
    }

    @Test
    @DisplayName("비밀번호 정책 위반은 400 VALIDATION_ERROR (8~64자, 영문+숫자)")
    void passwordPolicyViolation() throws Exception {
        var setup = createTenantSetup("bonjuk");

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(setup.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", PASSWORD, "newPassword", "short1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
