package com.orderflow.api.system;

import com.orderflow.api.auth.TemporaryPasswordGenerator;
import com.orderflow.api.system.dto.TenantDtos.TenantRegisterRequest;
import com.orderflow.api.system.dto.TenantDtos.TenantRegisterResponse;
import com.orderflow.common.error.AuthErrorCode;
import com.orderflow.common.error.BusinessException;
import com.orderflow.domain.iam.Tenant;
import com.orderflow.domain.iam.TenantRepository;
import com.orderflow.domain.iam.User;
import com.orderflow.domain.iam.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테넌트 + 최초 본사 관리자 등록 (US-AUTH-01, api-spec 2.4.1) — 원자적 생성.
 * SYSTEM 전용이라 항상 UNFILTERED 컨텍스트에서 실행된다 → 이메일 전역 중복 검사가 그대로 유효.
 */
@Service
@RequiredArgsConstructor
public class TenantRegistrationService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    @Transactional
    public TenantRegisterResponse register(TenantRegisterRequest request) {
        if (userRepository.existsByEmail(request.admin().email())) {
            throw new BusinessException(AuthErrorCode.EMAIL_DUPLICATED);
        }
        Tenant tenant = tenantRepository.save(Tenant.register(request.name(), request.cutoffTime()));
        String temporaryPassword = temporaryPasswordGenerator.generate();
        User admin = userRepository.save(User.registerHqAdmin(
                tenant.getId(),
                request.admin().email(),
                passwordEncoder.encode(temporaryPassword),
                request.admin().name()));

        return TenantRegisterResponse.of(tenant, admin, temporaryPassword);
    }
}
