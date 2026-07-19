package com.orderflow.api.auth.jwt;

import com.orderflow.api.auth.AuthenticatedUser;
import com.orderflow.domain.iam.User;
import com.orderflow.domain.iam.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * 액세스 토큰(JWT) 발급·검증 — 클레임: sub(userId), tenant_id, store_id, role (api-spec 2.2).
 * 만료는 {@link io.jsonwebtoken.ExpiredJwtException}, 그 외 무효는 {@link io.jsonwebtoken.JwtException}으로
 * 던져지며 인증 필터가 401 코드(TOKEN_EXPIRED / UNAUTHORIZED)로 구분한다.
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TENANT_ID = "tenant_id";
    private static final String CLAIM_STORE_ID = "store_id";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final long accessTtlSeconds;

    public JwtTokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = properties.accessTokenTtl().toSeconds();
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_TENANT_ID, user.getTenantId())
                .claim(CLAIM_STORE_ID, user.getStoreId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .signWith(key)
                .compact();
    }

    /** @throws io.jsonwebtoken.ExpiredJwtException 만료, @throws io.jsonwebtoken.JwtException 위조·형식 오류 */
    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthenticatedUser(
                Long.valueOf(claims.getSubject()),
                longClaim(claims, CLAIM_TENANT_ID),
                longClaim(claims, CLAIM_STORE_ID),
                UserRole.valueOf(claims.get(CLAIM_ROLE, String.class)));
    }

    public long accessTtlSeconds() {
        return accessTtlSeconds;
    }

    private Long longClaim(Claims claims, String name) {
        Object value = claims.get(name);
        return value == null ? null : ((Number) value).longValue();
    }
}
