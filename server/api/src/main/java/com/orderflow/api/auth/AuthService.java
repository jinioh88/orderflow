package com.orderflow.api.auth;

import com.orderflow.api.auth.dto.AuthDtos.LoginResponse;
import com.orderflow.api.auth.dto.AuthDtos.PasswordSetupResponse;
import com.orderflow.api.auth.dto.AuthDtos.TokenResponse;
import com.orderflow.api.auth.dto.AuthDtos.UserSummary;
import com.orderflow.api.auth.jwt.JwtTokenProvider;
import com.orderflow.common.error.AuthErrorCode;
import com.orderflow.common.error.BusinessException;
import com.orderflow.common.error.CommonErrorCode;
import com.orderflow.common.error.EntityNotFoundException;
import com.orderflow.domain.common.TenantContext;
import com.orderflow.domain.iam.Store;
import com.orderflow.domain.iam.StoreRepository;
import com.orderflow.domain.iam.Tenant;
import com.orderflow.domain.iam.TenantRepository;
import com.orderflow.domain.iam.User;
import com.orderflow.domain.iam.UserRepository;
import com.orderflow.infra.auth.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 유스케이스 (US-AUTH-03) — 로그인/재발급/로그아웃/비밀번호 설정.
 * 로그인·재발급은 인증 전 전역 조회라 runUnfiltered로 감싼다 (격리 설계 노트 §1 화이트리스트).
 * 이 두 메서드는 @Transactional을 걸지 않는다 — 트랜잭션이 runUnfiltered 안에서 시작해야 필터가 풀린다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    public LoginResponse login(String email, String rawPassword) {
        return TenantContext.runUnfiltered(() -> {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));
            if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
                throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
            }
            assertLoginAllowed(user); // 403 ACCOUNT_INACTIVE

            return new LoginResponse(
                    tokenProvider.createAccessToken(user),
                    tokenProvider.accessTtlSeconds(),
                    refreshTokenStore.issue(user.getId()),
                    user.requiresPasswordSetup(),
                    UserSummary.from(user));
        });
    }

    /** 회전(rotation): 기존 토큰은 consume으로 즉시 무효화되고 새 쌍이 발급된다 (api-spec 2.2) */
    public TokenResponse refresh(String refreshToken) {
        return TenantContext.runUnfiltered(() -> {
            Long userId = refreshTokenStore.consume(refreshToken)
                    .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));
            try {
                assertLoginAllowed(user);
            } catch (BusinessException e) {
                // 비활성 사유 불문 동일 코드 — api-spec 2.4.3
                throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
            }
            return new TokenResponse(
                    tokenProvider.createAccessToken(user),
                    tokenProvider.accessTtlSeconds(),
                    refreshTokenStore.issue(user.getId()));
        });
    }

    /** 멱등 — 이미 무효한 토큰이어도 성공 (api-spec 2.4.5) */
    public void logout(String refreshToken) {
        refreshTokenStore.consume(refreshToken);
    }

    /** 임시 상태 해제 + 일반 변경 공용. 기존 리프레시 전체 무효화 후 새 쌍 반환 (api-spec 2.4.4) */
    @Transactional
    public PasswordSetupResponse setPassword(AuthenticatedUser principal, String currentPassword, String newPassword) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(EntityNotFoundException::new);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        if (currentPassword.equals(newPassword)) {
            throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "새 비밀번호는 기존 비밀번호와 달라야 합니다.");
        }
        user.confirmPassword(passwordEncoder.encode(newPassword));
        refreshTokenStore.revokeAll(user.getId());

        return new PasswordSetupResponse(
                tokenProvider.createAccessToken(user),
                tokenProvider.accessTtlSeconds(),
                refreshTokenStore.issue(user.getId()),
                false);
    }

    /** 로그인 가드 — 계정·소속 가맹점·테넌트 중 하나라도 비활성/정지면 거부 (api-spec 2.4.2) */
    private void assertLoginAllowed(User user) {
        if (!user.isActive()) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_INACTIVE);
        }
        if (user.getTenantId() != null) {
            Tenant tenant = tenantRepository.findById(user.getTenantId()).orElse(null);
            if (tenant == null || !tenant.isActive()) {
                throw new BusinessException(AuthErrorCode.ACCOUNT_INACTIVE);
            }
        }
        if (user.getStoreId() != null) {
            Store store = storeRepository.findById(user.getStoreId()).orElse(null);
            if (store == null || !store.isActive()) {
                throw new BusinessException(AuthErrorCode.ACCOUNT_INACTIVE);
            }
        }
    }
}
