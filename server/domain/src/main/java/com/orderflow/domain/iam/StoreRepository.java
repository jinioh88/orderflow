package com.orderflow.domain.iam;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends Repository<Store, Long> {

    Store save(Store store);

    /**
     * 단건 조회를 JPQL로 강제 — 파생 findById는 em.find로 풀려 테넌트 필터가 적용되지 않는다
     * (설계 노트 §5-2). 교차 테넌트 ID 조회는 빈 결과 → 서비스가 404로 변환.
     */
    @Query("select s from Store s where s.id = :id")
    Optional<Store> findById(@Param("id") Long id);

    /** 가맹점 목록 (api-spec 2.4.7) — 테넌트 스코프는 필터가 강제한다. */
    @Query("select s from Store s where (:status is null or s.status = :status)")
    Page<Store> findAllByOptionalStatus(@Param("status") StoreStatus status, Pageable pageable);
}
