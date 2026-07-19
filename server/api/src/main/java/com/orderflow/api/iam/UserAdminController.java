package com.orderflow.api.iam;

import com.orderflow.api.auth.AuthenticatedUser;
import com.orderflow.api.common.response.ApiResponse;
import com.orderflow.api.common.response.PageResponse;
import com.orderflow.api.common.web.PageRequests;
import com.orderflow.api.iam.dto.IamDtos.TemporaryPasswordResponse;
import com.orderflow.api.iam.dto.IamDtos.UserCreatedResponse;
import com.orderflow.api.iam.dto.IamDtos.UserRegisterRequest;
import com.orderflow.api.iam.dto.IamDtos.UserResponse;
import com.orderflow.domain.iam.User;
import com.orderflow.domain.iam.UserRole;
import com.orderflow.domain.iam.UserStatus;
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
 * 계정 관리 엔드포인트 (api-spec 2.4.9~2.4.12)
 */
@RestController
@RequiredArgsConstructor
public class UserAdminController {

    private static final Set<String> SORT_FIELDS = Set.of("name", "createdAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final UserAdminService userAdminService;

    @PostMapping("/api/v1/users")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserCreatedResponse> register(@AuthenticationPrincipal AuthenticatedUser principal,
                                                     @Valid @RequestBody UserRegisterRequest request) {
        return ApiResponse.of(userAdminService.registerStoreOwner(principal, request));
    }

    @GetMapping("/api/v1/users")
    public ApiResponse<PageResponse<UserResponse>> list(@RequestParam(required = false) Long storeId,
                                                        @RequestParam(required = false) UserStatus status,
                                                        @RequestParam(required = false) UserRole role,
                                                        Pageable pageable) {
        Pageable resolved = PageRequests.resolve(pageable, SORT_FIELDS, DEFAULT_SORT);
        Page<User> page = userAdminService.list(storeId, status, role, resolved);
        return ApiResponse.of(PageResponse.of(
                page.getContent().stream().map(UserResponse::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements()));
    }

    @PostMapping("/api/v1/users/{userId}/deactivate")
    public ApiResponse<UserResponse> deactivate(@AuthenticationPrincipal AuthenticatedUser principal,
                                                @PathVariable Long userId) {
        return ApiResponse.of(userAdminService.deactivate(principal, userId));
    }

    @PostMapping("/api/v1/users/{userId}/temporary-password")
    public ApiResponse<TemporaryPasswordResponse> reissueTemporaryPassword(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long userId) {
        return ApiResponse.of(new TemporaryPasswordResponse(
                userAdminService.reissueTemporaryPassword(principal, userId)));
    }
}
