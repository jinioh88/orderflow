package com.orderflow.api.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderflow.api.support.IntegrationTest;
import com.orderflow.api.support.TenantScope;
import com.orderflow.domain.catalog.HqStock;
import com.orderflow.domain.catalog.HqStockRepository;
import com.orderflow.domain.catalog.Product;
import com.orderflow.domain.catalog.ProductRepository;
import com.orderflow.domain.iam.Tenant;
import com.orderflow.domain.iam.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Catalog 애그리거트 영속성 검증 (erd 3.2) — 수동 관리 DDL 위에서 insert·유니크·CHECK가 도는지,
 * 테넌트 필터가 신규 엔티티에도 적용되는지 확인한다. 컨텍스트 기동 시 ddl-auto=validate가
 * 엔티티 매핑 ↔ 스키마 드리프트를 함께 검증한다.
 */
class CatalogPersistenceTest extends IntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private HqStockRepository hqStockRepository;
    @PersistenceContext
    private EntityManager em;

    private Tenant tenantA;
    private Tenant tenantB;

    @BeforeEach
    void createTenants() {
        tenantA = tenantRepository.save(Tenant.register("본죽F&B", null));
        tenantB = tenantRepository.save(Tenant.register("김밥왕국", null));
    }

    private Product product(Long tenantId, String code, String barcode) {
        return Product.register(tenantId, code, "김치만두 1kg", barcode,
                "냉동식품", "BOX", new BigDecimal("12000"));
    }

    @Test
    @DisplayName("상품 → 가용 재고를 스키마 그대로 저장·조회할 수 있다")
    void persistCatalogAggregates() {
        Product saved = productRepository.save(product(tenantA.getId(), "P-0001", "8801111111111"));
        saved.designateLimited();
        hqStockRepository.save(HqStock.create(tenantA.getId(), saved.getId(), 100));
        TenantScope.tenant(em, tenantA.getId());

        Product found = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getUnitPrice()).isEqualByComparingTo("12000");
        assertThat(found.isLimited()).isTrue();
        assertThat(found.getCreatedAt()).isNotNull();

        HqStock stock = hqStockRepository.findByProductId(saved.getId()).orElseThrow();
        assertThat(stock.getAvailableQty()).isEqualTo(100);
        assertThat(stock.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("타 테넌트의 상품·가용 재고는 필터에 걸려 빈 결과다 (US-AUTH-04)")
    void tenantFilterAppliesToCatalog() {
        Product mine = productRepository.save(product(tenantA.getId(), "P-0001", "8801111111111"));
        Product others = productRepository.save(product(tenantB.getId(), "P-0002", "8802222222222"));
        hqStockRepository.save(HqStock.create(tenantB.getId(), others.getId(), 50));
        TenantScope.tenant(em, tenantA.getId());

        assertThat(productRepository.findById(mine.getId())).isPresent();
        assertThat(productRepository.findById(others.getId())).isEmpty();
        assertThat(hqStockRepository.findByProductId(others.getId())).isEmpty();
    }

    @Test
    @DisplayName("품목코드·바코드 유니크는 테넌트 스코프다 — 같은 테넌트만 충돌 (erd 3.2)")
    void productUniquenessIsTenantScoped() {
        productRepository.save(product(tenantA.getId(), "P-0001", "8801111111111"));

        // 다른 테넌트는 같은 품목코드·바코드 사용 가능
        productRepository.save(product(tenantB.getId(), "P-0001", "8801111111111"));

        // 같은 테넌트의 품목코드 중복은 DB 유니크가 막는다 (PRODUCT_CODE_DUPLICATED의 안전망)
        // IDENTITY 전략은 save 시점에 즉시 INSERT가 나간다
        assertThatThrownBy(() ->
                productRepository.save(product(tenantA.getId(), "P-0001", "8803333333333")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("available_qty ≥ 0 은 DB CHECK가 최후 방어선이다 (NFR-3.1)")
    void availableQtyCheckConstraintIsLastLineOfDefense() {
        Product saved = productRepository.save(product(tenantA.getId(), "P-0001", "8801111111111"));
        hqStockRepository.save(HqStock.create(tenantA.getId(), saved.getId(), 10));
        em.flush();

        assertThatThrownBy(() -> em.createNativeQuery(
                        "UPDATE hq_stock SET available_qty = -1 WHERE product_id = :productId")
                .setParameter("productId", saved.getId())
                .executeUpdate())
                .hasRootCauseInstanceOf(SQLException.class);
    }
}
