package com.orderflow.domain.iam;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * 애그리거트 루트당 리포지토리 1개 원칙. 쓰기 경로에 필요한 메서드만 선언한다 —
 * 목록·검색은 CQRS-lite로 infra의 QueryDSL 프로젝션이 담당한다 (CLAUDE.md 방법론 6).
 */
public interface TenantRepository extends Repository<Tenant, Long> {

    Tenant save(Tenant tenant);

    Optional<Tenant> findById(Long id);
}
