package com.orderflow.api.auth.dto;

import com.orderflow.domain.iam.User;
import com.orderflow.domain.iam.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * AUTH 요청/응답 스키마 — api-spec 2.4.2~2.4.5와 1:1.
 */
public final class AuthDtos {

    /** 비밀번호 정책 (api-spec 2.3): 8~64자, 영문자 1+ 그리고 숫자 1+ */
    public static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";
    public static final String PASSWORD_POLICY_MESSAGE = "8~64자, 영문자와 숫자를 각각 1자 이상 포함해야 합니다.";

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record LoginResponse(
            String accessToken,
            long accessTokenExpiresIn,
            String refreshToken,
            boolean passwordSetupRequired,
            UserSummary user) {
    }

    public record UserSummary(Long id, String email, String name, UserRole role, Long tenantId, Long storeId) {

        public static UserSummary from(User user) {
            return new UserSummary(user.getId(), user.getEmail(), user.getName(),
                    user.getRole(), user.getTenantId(), user.getStoreId());
        }
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    /** 회전된 새 토큰 쌍 (api-spec 2.4.3) */
    public record TokenResponse(String accessToken, long accessTokenExpiresIn, String refreshToken) {
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }

    public record PasswordSetupRequest(
            @NotBlank String currentPassword,
            @NotBlank @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_POLICY_MESSAGE) String newPassword) {
    }

    /** 비밀번호 설정 응답 — 기존 리프레시 전체 무효화에 따른 새 토큰 쌍 (api-spec 2.4.4) */
    public record PasswordSetupResponse(
            String accessToken,
            long accessTokenExpiresIn,
            String refreshToken,
            boolean passwordSetupRequired) {
    }
}
