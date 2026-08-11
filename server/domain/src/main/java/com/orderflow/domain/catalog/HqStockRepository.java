package com.orderflow.domain.catalog;

import java.util.Optional;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * HqStock 애그리거트 리포지토리 — 한정 품목만 레코드 존재 (04 §2.2).
 * 조회는 product_id 기준이 전부다 (product당 1행, D-1).
 */
public interface HqStockRepository extends Repository<HqStock, Long> {

    HqStock save(HqStock hqStock);

    /** JPQL 강제 — 테넌트 필터 적용 보장 (설계 노트 §5-2) */
    @Query("select s from HqStock s where s.productId = :productId")
    Optional<HqStock> findByProductId(@Param("productId") Long productId);

    /** 한정 품목 해제 시 삭제 (api-spec 3.2.7) — 상태 컬럼 예외: 한정 품목만 레코드 존재 불변식 유지 */
    void delete(HqStock hqStock);
}
