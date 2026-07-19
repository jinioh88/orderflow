package com.orderflow.api.auth.jwt;

import com.orderflow.api.auth.AuthenticatedUser;
import com.orderflow.api.common.error.SecurityErrorWriter;
import com.orderflow.common.error.AuthErrorCode;
import com.orderflow.domain.common.TenantContext;
import com.orderflow.domain.iam.User;
import com.orderflow.domain.iam.UserRepository;
import com.orderflow.domain.iam.UserRole;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 인증 필터 (US-AUTH-03) — 토큰 검증 → SecurityContext + TenantContext 세팅.
 *
 * - 만료/무효 토큰은 여기서 응답하지 않고 요청 속성에 코드만 남긴다 → 401 EntryPoint가
 *   TOKEN_EXPIRED / UNAUTHORIZED를 구분해 응답 (api-spec 1.4, 2.5).
 * - 임시 비밀번호 상태 차단(api-spec 2.3)은 "요청 시점의 서버 상태" 기준이므로 사용자를 매 요청
 *   로드한다 — 인증 전 전역 조회라 runUnfiltered로 감싼다 (격리 설계 노트 §1 화이트리스트).
 * - TenantContext 해제는 바깥의 TenantContextCleanupFilter가 보장한다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** EntryPoint가 읽는 401 사유 속성 (SecurityConfig의 EntryPoint와 계약) */
    public static final String AUTH_ERROR_ATTRIBUTE = "orderflow.auth.error";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final SecurityErrorWriter errorWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        AuthenticatedUser principal;
        try {
            principal = tokenProvider.parse(header.substring(BEARER_PREFIX.length()));
        } catch (ExpiredJwtException e) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, AuthErrorCode.TOKEN_EXPIRED);
            chain.doFilter(request, response);
            return;
        } catch (JwtException | IllegalArgumentException e) {
            chain.doFilter(request, response);
            return;
        }

        User user = TenantContext.runUnfiltered(
                () -> userRepository.findById(principal.userId()).orElse(null));
        if (user == null || !user.isActive()) {
            chain.doFilter(request, response);
            return;
        }
        if (user.requiresPasswordSetup() && !isAllowedForTemporaryState(request)) {
            errorWriter.write(response, AuthErrorCode.PASSWORD_SETUP_REQUIRED);
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))));
        if (principal.role() == UserRole.SYSTEM) {
            TenantContext.setUnfiltered();
        } else {
            TenantContext.setTenant(principal.tenantId());
        }
        chain.doFilter(request, response);
    }

    /** 임시 상태 허용 목록 (api-spec 2.3): 로그인/재발급/로그아웃 + 비밀번호 설정 */
    private boolean isAllowedForTemporaryState(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/v1/auth/")) {
            return true;
        }
        return "PUT".equals(request.getMethod()) && "/api/v1/users/me/password".equals(uri);
    }
}
