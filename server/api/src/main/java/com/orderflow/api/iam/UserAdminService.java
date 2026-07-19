package com.orderflow.api.iam;

import com.orderflow.api.auth.AuthenticatedUser;
import com.orderflow.api.auth.TemporaryPasswordGenerator;
import com.orderflow.api.iam.dto.IamDtos.UserCreatedResponse;
import com.orderflow.api.iam.dto.IamDtos.UserRegisterRequest;
import com.orderflow.api.iam.dto.IamDtos.UserResponse;
import com.orderflow.common.error.AuthErrorCode;
import com.orderflow.common.error.BusinessException;
import com.orderflow.common.error.CommonErrorCode;
import com.orderflow.common.error.EntityNotFoundException;
import com.orderflow.domain.common.TenantContext;
import com.orderflow.domain.iam.Store;
import com.orderflow.domain.iam.StoreRepository;
import com.orderflow.domain.iam.User;
import com.orderflow.domain.iam.UserRepository;
import com.orderflow.domain.iam.UserRole;
import com.orderflow.domain.iam.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.orderflow.infra.auth.RefreshTokenStore;

/**
 * 계정 관리 유스케이스 (US-AUTH-02, api-spec 2.4.9~2.4.12).
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final RefreshTokenStore refreshTokenStore;
    private final TransactionTemplate transactionTemplate;

    /**
     * 점주 계정 등록 (api-spec 2.4.9) — 역할은 STORE_OWNER 고정.
     * 이메일 전역 중복 검사는 테넌트 필터 밖에서 해야 하므로(타 테넌트의 동일 이메일도 충돌),
     * 트랜잭션 시작 전에 runUnfiltered로 검사하고 본 등록은 별도 트랜잭션으로 진행한다.
     * 검사~등록 사이의 레이스는 DB 유니크 제약이 막는다 (409 CONFLICT 변환).
     */
    public UserCreatedResponse registerStoreOwner(AuthenticatedUser principal, UserRegisterRequest request) {
        boolean emailTaken = TenantContext.runUnfiltered(() -> userRepository.existsByEmail(request.email()));
        if (emailTaken) {
            throw new BusinessException(AuthErrorCode.EMAIL_DUPLICATED);
        }
        return transactionTemplate.execute(status -> {
            Store store = storeRepository.findById(request.storeId()).orElseThrow(EntityNotFoundException::new);
            if (!store.isActive()) {
                throw new BusinessException(AuthErrorCode.STORE_INACTIVE);
            }
            String temporaryPassword = temporaryPasswordGenerator.generate();
            User owner = userRepository.save(User.registerStoreOwner(
                    principal.tenantId(), store.getId(), request.email(),
                    passwordEncoder.encode(temporaryPassword), request.name()));
            return UserCreatedResponse.of(owner, temporaryPassword);
        });
    }

    @Transactional(readOnly = true)
    public Page<User> list(Long storeId, UserStatus status, UserRole role, Pageable pageable) {
        return userRepository.findAllByFilters(storeId, status, role, pageable);
    }

    @Transactional
    public UserResponse deactivate(AuthenticatedUser principal, Long userId) {
        if (userId.equals(principal.userId())) {
            throw new BusinessException(CommonErrorCode.CONFLICT, "자기 자신은 비활성화할 수 없습니다.");
        }
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
        user.deactivate(); // 이미 비활성이면 409
        refreshTokenStore.revokeAll(user.getId());
        return UserResponse.from(user);
    }

    /**
     * 임시 비밀번호 재발급 (api-spec 2.4.10).
     * HQ_ADMIN: 자기 테넌트 사용자만 (필터가 강제 — 교차·SYSTEM 대상은 404).
     * SYSTEM: HQ_ADMIN 대상만 (그 외 역할은 403).
     */
    @Transactional
    public String reissueTemporaryPassword(AuthenticatedUser principal, Long userId) {
        if (userId.equals(principal.userId())) {
            throw new BusinessException(CommonErrorCode.CONFLICT, "자기 자신에게는 재발급할 수 없습니다.");
        }
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
        if (principal.role() == UserRole.SYSTEM && user.getRole() != UserRole.HQ_ADMIN) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "SYSTEM은 본사 관리자에게만 재발급할 수 있습니다.");
        }
        String temporaryPassword = temporaryPasswordGenerator.generate();
        user.issueTemporaryPassword(passwordEncoder.encode(temporaryPassword));
        refreshTokenStore.revokeAll(user.getId());
        return temporaryPassword;
    }
}
