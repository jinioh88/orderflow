package com.orderflow.api.iam;

import com.orderflow.api.auth.AuthenticatedUser;
import com.orderflow.api.iam.dto.IamDtos.StoreRegisterRequest;
import com.orderflow.api.iam.dto.IamDtos.StoreResponse;
import com.orderflow.common.error.EntityNotFoundException;
import com.orderflow.domain.iam.Store;
import com.orderflow.domain.iam.StoreRepository;
import com.orderflow.domain.iam.StoreStatus;
import com.orderflow.domain.iam.UserRepository;
import com.orderflow.infra.auth.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가맹점 관리 유스케이스 (US-AUTH-02, api-spec 2.4.6~2.4.8).
 * 테넌트 스코프는 필터가 강제한다 — tenant_id는 항상 토큰(principal)에서 온다.
 */
@Service
@RequiredArgsConstructor
public class StoreAdminService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional
    public StoreResponse register(AuthenticatedUser principal, StoreRegisterRequest request) {
        Store store = storeRepository.save(
                Store.register(principal.tenantId(), request.name(), request.address()));
        return StoreResponse.from(store);
    }

    @Transactional(readOnly = true)
    public Page<Store> list(StoreStatus status, Pageable pageable) {
        return storeRepository.findAllByOptionalStatus(status, pageable);
    }

    /** 교차 테넌트 접근은 필터의 빈 결과 → 404 (US-AUTH-04) */
    @Transactional
    public StoreResponse deactivate(Long storeId) {
        Store store = storeRepository.findById(storeId).orElseThrow(EntityNotFoundException::new);
        store.deactivate(); // 이미 비활성이면 409

        // 소속 사용자 전원 재발급 차단 (api-spec 2.2 무효화 이벤트)
        userRepository.findAllByStoreId(storeId)
                .forEach(user -> refreshTokenStore.revokeAll(user.getId()));
        return StoreResponse.from(store);
    }
}
