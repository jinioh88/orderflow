package com.orderflow.domain.catalog;

import java.util.Optional;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Product 애그리거트 리포지토리 — 쓰기 경로 최소 메서드만.
 * 목록 조회(페이징·검색)는 CQRS-lite QueryDSL 프로젝션으로 infra에 둔다 (CAT API 태스크).
 */
public interface ProductRepository extends Repository<Product, Long> {

    Product save(Product product);

    /**
     * 단건 조회를 JPQL로 강제 — 파생 findById는 em.find로 풀려 테넌트 필터가 적용되지 않는다
     * (설계 노트 §5-2). 교차 테넌트 ID 조회는 빈 결과 → 서비스가 404로 변환.
     */
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findById(@Param("id") Long id);
}
