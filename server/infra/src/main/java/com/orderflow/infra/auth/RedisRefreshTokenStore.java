package com.orderflow.infra.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 리프레시 토큰 저장소 (PM 결정 2026-07-17 — Redis 저장).
 *
 * 키 구조:
 * - {@code auth:refresh:{token}} → userId (TTL 14일) — 토큰 검증·소비
 * - {@code auth:refresh:user:{userId}} → 토큰 Set (TTL 14일) — 사용자 단위 일괄 무효화 역인덱스
 */
@Component
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String TOKEN_KEY_PREFIX = "auth:refresh:";
    private static final String USER_KEY_PREFIX = "auth:refresh:user:";
    private static final int TOKEN_BYTES = 32;

    private final StringRedisTemplate redis;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    public RedisRefreshTokenStore(StringRedisTemplate redis,
                                  @Value("${orderflow.auth.refresh-token-ttl:14d}") Duration ttl) {
        this.redis = redis;
        this.ttl = ttl;
    }

    @Override
    public String issue(Long userId) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);

        redis.opsForValue().set(TOKEN_KEY_PREFIX + token, String.valueOf(userId), ttl);
        String userKey = USER_KEY_PREFIX + userId;
        redis.opsForSet().add(userKey, token);
        redis.expire(userKey, ttl);
        return token;
    }

    @Override
    public Optional<Long> consume(String refreshToken) {
        String userId = redis.opsForValue().getAndDelete(TOKEN_KEY_PREFIX + refreshToken);
        if (userId == null) {
            return Optional.empty();
        }
        redis.opsForSet().remove(USER_KEY_PREFIX + userId, refreshToken);
        return Optional.of(Long.valueOf(userId));
    }

    @Override
    public void revokeAll(Long userId) {
        String userKey = USER_KEY_PREFIX + userId;
        Set<String> tokens = redis.opsForSet().members(userKey);
        if (tokens != null) {
            tokens.forEach(token -> redis.delete(TOKEN_KEY_PREFIX + token));
        }
        redis.delete(userKey);
    }
}
