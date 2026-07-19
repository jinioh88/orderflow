package com.orderflow.api.iam;

import com.orderflow.api.auth.AuthenticatedUser;
import com.orderflow.api.common.response.ApiResponse;
import com.orderflow.api.common.response.PageResponse;
import com.orderflow.api.common.web.PageRequests;
import com.orderflow.api.iam.dto.IamDtos.StoreRegisterRequest;
import com.orderflow.api.iam.dto.IamDtos.StoreResponse;
import com.orderflow.domain.iam.Store;
import com.orderflow.domain.iam.StoreStatus;
import jakarta.validation.Valid;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 가맹점 관리 엔드포인트 (api-spec 2.4.6~2.4.8)
 */
@RestController
@RequiredArgsConstructor
public class StoreAdminController {

    private static final Set<String> SORT_FIELDS = Set.of("name", "createdAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final StoreAdminService storeAdminService;

    @PostMapping("/api/v1/stores")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StoreResponse> register(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @Valid @RequestBody StoreRegisterRequest request) {
        return ApiResponse.of(storeAdminService.register(principal, request));
    }

    @GetMapping("/api/v1/stores")
    public ApiResponse<PageResponse<StoreResponse>> list(@RequestParam(required = false) StoreStatus status,
                                                         Pageable pageable) {
        Pageable resolved = PageRequests.resolve(pageable, SORT_FIELDS, DEFAULT_SORT);
        Page<Store> page = storeAdminService.list(status, resolved);
        return ApiResponse.of(PageResponse.of(
                page.getContent().stream().map(StoreResponse::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements()));
    }

    @PostMapping("/api/v1/stores/{storeId}/deactivate")
    public ApiResponse<StoreResponse> deactivate(@PathVariable Long storeId) {
        return ApiResponse.of(storeAdminService.deactivate(storeId));
    }
}
