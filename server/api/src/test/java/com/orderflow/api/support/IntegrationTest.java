package com.orderflow.api.support;

import com.orderflow.domain.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 테스트 베이스 — docker-compose의 MySQL(orderflow_test 전용 DB)·Redis를 재사용한다.
 * 테스트 간 데이터 격리는 테스트 단위 트랜잭션 롤백으로 보장한다.
 * 커밋이 실제로 일어나야 하는 테스트(동시성·락 등)는 이 클래스를 상속하지 말 것.
 *
 * 테넌트 필터: 트랜잭션 시작 훅이 컨텍스트 미설정 상태의 필터를 fail-closed(-1)로 켠다.
 * 테넌트 스코프가 필요한 테스트는 {@link TenantScope}로 전환하고, 컨텍스트 잔재는 여기서 정리한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTest {

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }
}
