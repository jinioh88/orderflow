package com.orderflow.api.catalog;

import com.orderflow.api.auth.AuthenticatedUser;
import com.orderflow.api.catalog.dto.CatalogDtos.AvailableQtyRequest;
import com.orderflow.api.catalog.dto.CatalogDtos.HqStockResponse;
import com.orderflow.api.catalog.dto.CatalogDtos.ProductCreateRequest;
import com.orderflow.api.catalog.dto.CatalogDtos.ProductResponse;
import com.orderflow.api.catalog.dto.CatalogDtos.ProductUpdateRequest;
import com.orderflow.common.error.BusinessException;
import com.orderflow.common.error.CatalogErrorCode;
import com.orderflow.common.error.EntityNotFoundException;
import com.orderflow.domain.catalog.HqStock;
import com.orderflow.domain.catalog.HqStockRepository;
import com.orderflow.domain.catalog.Product;
import com.orderflow.domain.catalog.ProductRepository;
import com.orderflow.infra.catalog.ProductQueryRepository;
import com.orderflow.infra.catalog.ProductSearchCondition;
import com.orderflow.infra.catalog.ProductSummary;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 카탈로그 유스케이스 (US-CAT-01·04, api-spec 3.2).
 * 테넌트 스코프는 필터가 강제한다 — tenant_id는 항상 토큰(principal)에서 온다.
 * 교차 테넌트 접근은 필터의 빈 결과 → 404 (US-AUTH-04).
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final HqStockRepository hqStockRepository;
    private final ProductQueryRepository productQueryRepository;

    @Transactional
    public ProductResponse register(AuthenticatedUser principal, ProductCreateRequest request) {
        if (productRepository.existsByProductCode(request.productCode())) {
            throw new BusinessException(CatalogErrorCode.PRODUCT_CODE_DUPLICATED);
        }
        requireBarcodeAvailable(request.barcode(), null);
        Product product = productRepository.save(Product.register(
                principal.tenantId(), request.productCode(), request.name(), request.barcode(),
                request.category(), request.orderUnit(), BigDecimal.valueOf(request.unitPrice())));
        return ProductResponse.from(product, null);
    }

    @Transactional(readOnly = true)
    public Page<ProductSummary> list(ProductSearchCondition condition, Pageable pageable) {
        return productQueryRepository.search(condition, pageable);
    }

    /** 카테고리 칩 UI용 목록 (api-spec 3.2.11) */
    @Transactional(readOnly = true)
    public List<String> listUsedCategories() {
        return productQueryRepository.findUsedCategories();
    }

    @Transactional(readOnly = true)
    public ProductResponse getOne(Long productId) {
        Product product = findProduct(productId);
        return withStock(product);
    }

    @Transactional
    public ProductResponse update(Long productId, ProductUpdateRequest request) {
        Product product = findProduct(productId);
        requireBarcodeAvailable(request.barcode(), productId);
        product.update(request.name(), request.barcode(), request.category(),
                request.orderUnit(), BigDecimal.valueOf(request.unitPrice()));
        return withStock(product);
    }

    @Transactional
    public ProductResponse suspend(Long productId) {
        Product product = findProduct(productId);
        product.suspend(); // 이미 중지면 409
        return withStock(product);
    }

    /**
     * 한정 품목 지정 (api-spec 3.2.6) — Product 플래그와 HqStock 생성을 한 트랜잭션으로 묶는다.
     * "한정 품목만 레코드 존재" 불변식(04 §2.2)의 원자성 때문에 이벤트 분리를 쓰지 않는
     * 애그리거트-단일-트랜잭션 원칙의 명시적 예외 (게이트 CAT-1 승인, 2026-08-11).
     */
    @Transactional
    public ProductResponse designateLimited(Long productId, AvailableQtyRequest request) {
        Product product = findProduct(productId);
        product.designateLimited(); // 이미 한정이면 ALREADY_LIMITED
        hqStockRepository.save(HqStock.create(product, request.availableQty()));
        return ProductResponse.from(product, request.availableQty());
    }

    /** 한정 품목 해제 (api-spec 3.2.7) — 가용 재고 레코드 삭제, 수량 미보존 */
    @Transactional
    public ProductResponse releaseLimited(Long productId) {
        Product product = findProduct(productId);
        product.releaseLimited(); // 한정 아니면 NOT_LIMITED
        hqStockRepository.findByProductId(productId).ifPresent(hqStockRepository::delete);
        return ProductResponse.from(product, null);
    }

    /** 가용 재고 설정 (api-spec 3.2.8) — 절대값 교체 */
    @Transactional
    public HqStockResponse changeAvailableQty(Long productId, AvailableQtyRequest request) {
        Product product = findProduct(productId);
        if (!product.isLimited()) {
            throw new BusinessException(CatalogErrorCode.NOT_LIMITED);
        }
        // 한정인데 재고 행이 없으면 불변식 위반(04 §2.2) — 사용자 오류(409)로 위장하지 않는다
        HqStock stock = hqStockRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalStateException(
                        "한정 품목의 가용 재고 레코드가 없습니다. productId=" + productId));
        stock.changeAvailableQty(request.availableQty());
        return new HqStockResponse(productId, stock.getAvailableQty());
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId).orElseThrow(EntityNotFoundException::new);
    }

    private void requireBarcodeAvailable(String barcode, Long excludeId) {
        if (productRepository.existsByBarcodeExcluding(barcode, excludeId)) {
            throw new BusinessException(CatalogErrorCode.BARCODE_DUPLICATED);
        }
    }

    private ProductResponse withStock(Product product) {
        Integer availableQty = product.isLimited()
                ? hqStockRepository.findByProductId(product.getId()).map(HqStock::getAvailableQty).orElse(null)
                : null;
        return ProductResponse.from(product, availableQty);
    }
}
