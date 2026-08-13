package com.orderflow.api.catalog;

import com.orderflow.api.auth.AuthenticatedUser;
import com.orderflow.api.catalog.dto.CatalogDtos.AvailableQtyRequest;
import com.orderflow.api.catalog.dto.CatalogDtos.HqStockResponse;
import com.orderflow.api.catalog.dto.CatalogDtos.ProductCreateRequest;
import com.orderflow.api.catalog.dto.CatalogDtos.ProductResponse;
import com.orderflow.api.catalog.dto.CatalogDtos.ProductUpdateRequest;
import com.orderflow.api.common.response.ApiResponse;
import com.orderflow.api.common.response.PageResponse;
import com.orderflow.api.common.web.PageRequests;
import com.orderflow.infra.catalog.ProductSearchCondition;
import com.orderflow.infra.catalog.ProductSummary;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상품 카탈로그 엔드포인트 (api-spec 3.2.1~3.2.8, 3.2.11)
 */
@RestController
@RequiredArgsConstructor
public class ProductController {

    private static final Set<String> SORT_FIELDS = Set.of("productCode", "name", "unitPrice", "createdAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final ProductService productService;

    @PostMapping("/api/v1/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> register(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @Valid @RequestBody ProductCreateRequest request) {
        return ApiResponse.of(productService.register(principal, request));
    }

    /** 검색 조건은 레코드 생성자 바인딩 — 다운로드(3.3.3)와 같은 필터를 공유한다 */
    @GetMapping("/api/v1/products")
    public ApiResponse<PageResponse<ProductResponse>> list(ProductSearchCondition condition,
                                                           Pageable pageable) {
        Pageable resolved = PageRequests.resolve(pageable, SORT_FIELDS, DEFAULT_SORT);
        Page<ProductSummary> page = productService.list(condition, resolved);
        return ApiResponse.of(PageResponse.of(
                page.getContent().stream().map(ProductResponse::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements()));
    }

    /**
     * 카테고리 칩 UI용 목록 (api-spec 3.2.11).
     * 리터럴 경로라 아래 {@code /{productId}}(3.2.3)보다 우선 매칭된다 — 스프링의 패턴 우선순위에 의존하므로
     * 회귀 테스트로 고정해 뒀다 (뒤집히면 "categories"를 Long으로 변환하려다 400이 나간다).
     */
    @GetMapping("/api/v1/products/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.of(productService.listUsedCategories());
    }

    @GetMapping("/api/v1/products/{productId}")
    public ApiResponse<ProductResponse> getOne(@PathVariable Long productId) {
        return ApiResponse.of(productService.getOne(productId));
    }

    @PutMapping("/api/v1/products/{productId}")
    public ApiResponse<ProductResponse> update(@PathVariable Long productId,
                                               @Valid @RequestBody ProductUpdateRequest request) {
        return ApiResponse.of(productService.update(productId, request));
    }

    @PostMapping("/api/v1/products/{productId}/suspend")
    public ApiResponse<ProductResponse> suspend(@PathVariable Long productId) {
        return ApiResponse.of(productService.suspend(productId));
    }

    @PostMapping("/api/v1/products/{productId}/limited")
    public ApiResponse<ProductResponse> designateLimited(@PathVariable Long productId,
                                                         @Valid @RequestBody AvailableQtyRequest request) {
        return ApiResponse.of(productService.designateLimited(productId, request));
    }

    @DeleteMapping("/api/v1/products/{productId}/limited")
    public ApiResponse<ProductResponse> releaseLimited(@PathVariable Long productId) {
        return ApiResponse.of(productService.releaseLimited(productId));
    }

    @PutMapping("/api/v1/products/{productId}/hq-stock")
    public ApiResponse<HqStockResponse> changeAvailableQty(@PathVariable Long productId,
                                                           @Valid @RequestBody AvailableQtyRequest request) {
        return ApiResponse.of(productService.changeAvailableQty(productId, request));
    }
}
