package com.orderflow.api.system;

import com.orderflow.api.common.response.ApiResponse;
import com.orderflow.api.system.dto.TenantDtos.TenantRegisterRequest;
import com.orderflow.api.system.dto.TenantDtos.TenantRegisterResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시스템 관리자 전용 API (api-spec 2.4.1) — 접근 통제는 SecurityConfig의 /api/v1/system/** → SYSTEM
 */
@RestController
@RequiredArgsConstructor
public class SystemTenantController {

    private final TenantRegistrationService tenantRegistrationService;

    @PostMapping("/api/v1/system/tenants")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TenantRegisterResponse> register(@Valid @RequestBody TenantRegisterRequest request) {
        return ApiResponse.of(tenantRegistrationService.register(request));
    }
}
