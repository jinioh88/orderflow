package com.orderflow.api.config;

import com.orderflow.api.auth.jwt.JwtAuthenticationFilter;
import com.orderflow.api.auth.jwt.JwtProperties;
import com.orderflow.api.auth.jwt.JwtTokenProvider;
import com.orderflow.api.common.error.SecurityErrorWriter;
import com.orderflow.common.error.CommonErrorCode;
import com.orderflow.common.error.ErrorCode;
import com.orderflow.domain.iam.UserRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security 설정 — stateless JWT (NFR-2.2), 역할 기반 인가 매트릭스는 api-spec 2.4 표와 1:1.
 * 익명 허용은 로그인·토큰 재발급뿐이다 (api-spec 1.2 — 스펙에 명시된 것만 익명).
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // NFR-2.5
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtTokenProvider tokenProvider,
                                                   UserRepository userRepository,
                                                   SecurityErrorWriter errorWriter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // api-spec 2.4 접근 매트릭스
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/me/password").authenticated()
                        .requestMatchers("/api/v1/system/**").hasRole("SYSTEM")
                        .requestMatchers(HttpMethod.GET, "/api/v1/stores").hasAnyRole("HQ_ADMIN", "HQ_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users").hasAnyRole("HQ_ADMIN", "HQ_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/*/temporary-password")
                        .hasAnyRole("HQ_ADMIN", "SYSTEM")
                        .requestMatchers("/api/v1/stores/**").hasRole("HQ_ADMIN")
                        .requestMatchers("/api/v1/users/**").hasRole("HQ_ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint(errorWriter))
                        .accessDeniedHandler(accessDeniedHandler(errorWriter)))
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider, userRepository, errorWriter),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 401 — JWT 필터가 남긴 사유(TOKEN_EXPIRED)를 구분해 응답 (api-spec 1.4) */
    private AuthenticationEntryPoint authenticationEntryPoint(SecurityErrorWriter errorWriter) {
        return (request, response, exception) -> {
            Object reason = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
            errorWriter.write(response,
                    reason instanceof ErrorCode errorCode ? errorCode : CommonErrorCode.UNAUTHORIZED);
        };
    }

    /** 403 — 역할 권한 없음 (NFR-2.3). 임시 비밀번호 차단(PASSWORD_SETUP_REQUIRED)은 JWT 필터가 직접 응답한다 */
    private AccessDeniedHandler accessDeniedHandler(SecurityErrorWriter errorWriter) {
        return (request, response, exception) -> errorWriter.write(response, CommonErrorCode.FORBIDDEN);
    }
}
