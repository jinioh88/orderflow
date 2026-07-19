package com.orderflow.api.common.web;

import com.orderflow.domain.common.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청 종료 시 TenantContext 해제 보장 — 스레드 풀 재사용 누수 방지 (설계 노트 §1).
 * 컨텍스트 설정은 JWT 인증 필터(추후 US-AUTH-03 태스크)가, 해제는 항상 여기가 책임진다.
 * 가장 바깥 필터로 두어 이후 어떤 필터/서블릿에서 설정돼도 해제가 보장되게 한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantContextCleanupFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
