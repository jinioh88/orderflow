package com.orderflow.api.auth.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * JWT 설정 — 시크릿은 환경변수 주입(NFR-2.5), 액세스 TTL 기본 30분 (api-spec 2.2).
 */
@ConfigurationProperties(prefix = "orderflow.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl) {

    public JwtProperties {
        Assert.hasText(secret, "orderflow.jwt.secret이 비어 있다 — JWT_SECRET 환경변수를 설정하라");
        Assert.isTrue(secret.getBytes().length >= 32, "JWT 시크릿은 32바이트(256비트) 이상이어야 한다 (HS256)");
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(30);
        }
    }
}
