package com.orderflow.api.iam;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderflow.domain.common.TenantContext;
import com.orderflow.domain.iam.Store;
import com.orderflow.domain.iam.StoreRepository;
import com.orderflow.domain.iam.Tenant;
import com.orderflow.domain.iam.TenantRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 트랜잭션 시작 훅(TenantFilterTransactionListener)의 끝-대-끝 검증 (설계 노트 §2).
 *
 * 프로덕션과 동일하게 "컨텍스트 설정 → 트랜잭션 시작"의 순서를 재현해야 하므로
 * 테스트 관리 트랜잭션(@Transactional)을 쓰지 않고 TransactionTemplate으로 경계를 직접 연다.
 * 커밋이 실제로 일어나므로 픽스처는 @AfterEach에서 직접 지운다.
 */
@SpringBootTest
@ActiveProfiles("test")
class TenantFilterTransactionHookTest {

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long tenantAId;
    private Long tenantBId;
    private Long storeAId;
    private Long storeBId;

    @BeforeEach
    void setUpFixture() {
        TenantContext.clear();
        transactionTemplate.executeWithoutResult(status -> {
            Tenant tenantA = tenantRepository.save(Tenant.register("훅검증-테넌트A", null));
            Tenant tenantB = tenantRepository.save(Tenant.register("훅검증-테넌트B", null));
            tenantAId = tenantA.getId();
            tenantBId = tenantB.getId();
            storeAId = storeRepository.save(Store.register(tenantAId, "A-1호점", null)).getId();
            storeBId = storeRepository.save(Store.register(tenantBId, "B-1호점", null)).getId();
        });
    }

    @AfterEach
    void cleanUpFixture() {
        TenantContext.clear();
        jdbcTemplate.update("DELETE FROM store WHERE id IN (?, ?)", storeAId, storeBId);
        jdbcTemplate.update("DELETE FROM tenant WHERE id IN (?, ?)", tenantAId, tenantBId);
    }

    @Test
    @DisplayName("컨텍스트의 테넌트로 시작한 트랜잭션은 타 테넌트 데이터를 보지 못한다")
    void hookEnablesFilterFromContext() {
        TenantContext.setTenant(tenantAId);

        Optional<Store> mine = transactionTemplate.execute(s -> storeRepository.findById(storeAId));
        Optional<Store> others = transactionTemplate.execute(s -> storeRepository.findById(storeBId));

        assertThat(mine).isPresent();
        assertThat(others).isEmpty();
    }

    @Test
    @DisplayName("컨텍스트 없이 시작한 트랜잭션은 아무것도 보지 못한다 — fail-closed")
    void hookFailsClosedWithoutContext() {
        Optional<Store> result = transactionTemplate.execute(s -> storeRepository.findById(storeAId));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("UNFILTERED 컨텍스트로 시작한 트랜잭션은 전역 조회가 된다 (SYSTEM 경로)")
    void hookSkipsFilterWhenUnfiltered() {
        TenantContext.setUnfiltered();

        Optional<Store> result = transactionTemplate.execute(s -> storeRepository.findById(storeBId));

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("runUnfiltered가 트랜잭션 경계를 감싸면 그 트랜잭션만 전역이다 (로그인 경로)")
    void runUnfilteredWrapsTransaction() {
        Optional<Store> inside = TenantContext.runUnfiltered(
                () -> transactionTemplate.execute(s -> storeRepository.findById(storeBId)));
        Optional<Store> after = transactionTemplate.execute(s -> storeRepository.findById(storeBId));

        assertThat(inside).isPresent();
        assertThat(after).isEmpty();
    }
}
