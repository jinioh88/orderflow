package com.orderflow.api.iam;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderflow.api.support.IntegrationTest;
import com.orderflow.api.support.TenantScope;
import com.orderflow.domain.iam.PasswordStatus;
import com.orderflow.domain.iam.Store;
import com.orderflow.domain.iam.StoreRepository;
import com.orderflow.domain.iam.Tenant;
import com.orderflow.domain.iam.TenantRepository;
import com.orderflow.domain.iam.User;
import com.orderflow.domain.iam.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * IAM 애그리거트 영속성 검증 — 수동 관리 DDL(db/schema.sql) 위에서 실제 insert가 도는지,
 * JPA Auditing이 공통 컬럼을 채우는지 확인한다. 컨텍스트 기동 시 ddl-auto=validate가
 * 엔티티 매핑 ↔ 스키마 드리프트를 함께 검증한다.
 */
class IamPersistenceTest extends IntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private UserRepository userRepository;
    @PersistenceContext
    private EntityManager em;

    @Test
    @DisplayName("테넌트 → 가맹점 → 점주 계정을 스키마 그대로 저장·조회할 수 있다")
    void persistIamAggregates() {
        Tenant tenant = tenantRepository.save(Tenant.register("본죽F&B", null));
        Store store = storeRepository.save(Store.register(tenant.getId(), "강남역점", "서울 강남구"));
        User owner = userRepository.save(User.registerStoreOwner(
                tenant.getId(), store.getId(), "owner@test.com", "$2a$10$encoded", "박점주"));
        TenantScope.tenant(em, tenant.getId());

        User found = userRepository.findByEmail("owner@test.com").orElseThrow();
        assertThat(found.getTenantId()).isEqualTo(tenant.getId());
        assertThat(found.getStoreId()).isEqualTo(store.getId());
        assertThat(found.getPasswordStatus()).isEqualTo(PasswordStatus.TEMPORARY);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("SYSTEM 계정은 tenant_id 없이 저장된다 (AUTH-1 승인)")
    void persistSystemAccountWithoutTenant() {
        User system = userRepository.save(
                User.registerSystem("system@orderflow.io", "$2a$10$encoded", "시스템 관리자"));
        TenantScope.unfiltered(em);

        User found = userRepository.findById(system.getId()).orElseThrow();
        assertThat(found.getTenantId()).isNull();
        assertThat(found.requiresPasswordSetup()).isFalse();
    }

    @Test
    @DisplayName("이메일 전역 중복을 리포지토리로 검사할 수 있다 (EMAIL_DUPLICATED의 근거)")
    void emailUniquenessCheck() {
        Tenant tenant = tenantRepository.save(Tenant.register("본죽F&B", null));
        userRepository.save(User.registerHqAdmin(tenant.getId(), "admin@test.com", "$2a$10$encoded", "김운영"));
        TenantScope.unfiltered(em);

        assertThat(userRepository.existsByEmail("admin@test.com")).isTrue();
        assertThat(userRepository.existsByEmail("other@test.com")).isFalse();
    }
}
