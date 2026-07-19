package com.orderflow.api.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderflow.api.support.IntegrationTest;
import com.orderflow.api.support.TenantScope;
import com.orderflow.common.error.EntityNotFoundException;
import com.orderflow.domain.iam.Store;
import com.orderflow.domain.iam.StoreRepository;
import com.orderflow.domain.iam.Tenant;
import com.orderflow.domain.iam.TenantRepository;
import com.orderflow.domain.iam.User;
import com.orderflow.domain.iam.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 테넌트 교차 접근 격리 (US-AUTH-04, NFR-2.4) — 필터 레벨 검증.
 * API 레벨(HTTP 404) 교차 테스트는 AUTH 엔드포인트 태스크에서 이 픽스처 패턴을 재사용해 추가한다.
 */
class CrossTenantIsolationTest extends IntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private UserRepository userRepository;
    @PersistenceContext
    private EntityManager em;

    private Tenant tenantA;
    private Tenant tenantB;
    private Store storeA;
    private Store storeB;
    private User ownerB;

    @BeforeEach
    void setUpTwoTenantFixture() {
        tenantA = tenantRepository.save(Tenant.register("테넌트A", null));
        tenantB = tenantRepository.save(Tenant.register("테넌트B", null));
        storeA = storeRepository.save(Store.register(tenantA.getId(), "A-1호점", null));
        storeB = storeRepository.save(Store.register(tenantB.getId(), "B-1호점", null));
        ownerB = userRepository.save(User.registerStoreOwner(
                tenantB.getId(), storeB.getId(), "owner-b@test.com", "$2a$10$encoded", "B점주"));
    }

    @Test
    @DisplayName("테넌트 A 스코프에서 B의 데이터는 ID를 알아도 빈 결과다")
    void crossTenantLookupReturnsEmpty() {
        TenantScope.tenant(em, tenantA.getId());

        assertThat(storeRepository.findById(storeA.getId())).isPresent();
        assertThat(storeRepository.findById(storeB.getId())).isEmpty();
        assertThat(userRepository.findById(ownerB.getId())).isEmpty();
        assertThat(userRepository.findByEmail("owner-b@test.com")).isEmpty();
    }

    @Test
    @DisplayName("대조군 — 같은 ID가 UNFILTERED 스코프에서는 조회된다 (필터가 걸렀음을 증명)")
    void sameLookupSucceedsWhenUnfiltered() {
        TenantScope.unfiltered(em);

        assertThat(storeRepository.findById(storeB.getId())).isPresent();
        assertThat(userRepository.findById(ownerB.getId())).isPresent();
    }

    @Test
    @DisplayName("컨텍스트 미설정이면 자기 테넌트 데이터도 안 보인다 — fail-closed")
    void noContextSeesNothing() {
        TenantScope.none(em);

        assertThat(storeRepository.findById(storeA.getId())).isEmpty();
        assertThat(storeRepository.findById(storeB.getId())).isEmpty();
    }

    @Test
    @DisplayName("교차 접근의 빈 결과는 404(RESOURCE_NOT_FOUND)로 변환된다 — 존재 여부 비노출 규약")
    void emptyResultConvertsTo404() {
        TenantScope.tenant(em, tenantA.getId());

        assertThatThrownBy(() -> storeRepository.findById(storeB.getId())
                .orElseThrow(EntityNotFoundException::new))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(e -> assertThat(((EntityNotFoundException) e).getErrorCode().status()).isEqualTo(404));
    }

    @Test
    @DisplayName("SYSTEM 계정(tenant_id NULL)은 테넌트 스코프에서 걸러진다")
    void systemAccountInvisibleInTenantScope() {
        User system = userRepository.save(
                User.registerSystem("system@orderflow.io", "$2a$10$encoded", "시스템 관리자"));
        TenantScope.tenant(em, tenantA.getId());

        assertThat(userRepository.findById(system.getId())).isEmpty();
        assertThat(userRepository.findByEmail("system@orderflow.io")).isEmpty();
    }
}
