package com.orderflow.domain.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Product 애그리거트 리포지토리 — 쓰기 경로 최소 메서드만.
 * 목록 조회(페이징·검색)는 CQRS-lite QueryDSL 프로젝션으로 infra에 둔다 (ProductQueryRepository).
 * 모든 쿼리의 테넌트 스코프는 Hibernate 필터가 강제한다.
 */
public interface ProductRepository extends Repository<Product, Long> {

    Product save(Product product);

    /**
     * 단건 조회를 JPQL로 강제 — 파생 findById는 em.find로 풀려 테넌트 필터가 적용되지 않는다
     * (설계 노트 §5-2). 교차 테넌트 ID 조회는 빈 결과 → 서비스가 404로 변환.
     */
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findById(@Param("id") Long id);

    /**
     * 품목코드 중복 사전 검사 (api-spec 3.4 PRODUCT_CODE_DUPLICATED) — DB 유니크가 안전망.
     * 파생 쿼리는 JPQL로 생성되어 테넌트 필터가 적용된다 (JPQL 강제가 필요한 건 findById뿐).
     */
    boolean existsByProductCode(String productCode);

    /** 바코드 중복 사전 검사 — 수정 시 자기 자신 제외. null 분기가 있어 파생 쿼리로 못 쓴다 */
    @Query("""
            select count(p) > 0 from Product p
            where p.barcode = :barcode
              and (:excludeId is null or p.id <> :excludeId)
            """)
    boolean existsByBarcodeExcluding(@Param("barcode") String barcode, @Param("excludeId") Long excludeId);

    /** 엑셀 업로드의 등록/수정 구분·바코드 충돌 검사용 벌크 조회 (US-CAT-02) */
    List<Product> findAllByProductCodeIn(Collection<String> productCodes);

    List<Product> findAllByBarcodeIn(Collection<String> barcodes);
}
