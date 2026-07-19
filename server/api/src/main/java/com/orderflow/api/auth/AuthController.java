package com.orderflow.api.auth;

import com.orderflow.api.auth.dto.AuthDtos.LoginRequest;
import com.orderflow.api.auth.dto.AuthDtos.LoginResponse;
import com.orderflow.api.auth.dto.AuthDtos.LogoutRequest;
import com.orderflow.api.auth.dto.AuthDtos.PasswordSetupRequest;
import com.orderflow.api.auth.dto.AuthDtos.PasswordSetupResponse;
import com.orderflow.api.auth.dto.AuthDtos.RefreshRequest;
import com.orderflow.api.auth.dto.AuthDtos.TokenResponse;
import com.orderflow.api.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * AUTH 엔드포인트 (api-spec 2.4.2~2.4.5)
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/api/v1/auth/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.of(authService.login(request.email(), request.password()));
    }

    @PostMapping("/api/v1/auth/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.of(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/api/v1/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
    }

    @PutMapping("/api/v1/users/me/password")
    public ApiResponse<PasswordSetupResponse> setPassword(@AuthenticationPrincipal AuthenticatedUser principal,
                                                          @Valid @RequestBody PasswordSetupRequest request) {
        return ApiResponse.of(authService.setPassword(principal, request.currentPassword(), request.newPassword()));
    }
}
