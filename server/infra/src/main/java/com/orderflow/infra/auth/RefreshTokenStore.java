package com.orderflow.infra.auth;

import java.util.Optional;

/**
 * 리프레시 토큰 저장소 (api-spec 2.2) — 불투명 토큰, TTL 14일 슬라이딩, 회전(rotation).
 */
public interface RefreshTokenStore {

    /** 새 리프레시 토큰 발급·저장. 회전 시에도 이 메서드로 새 토큰을 만든다 (TTL 재시작). */
    String issue(Long userId);

    /**
     * 토큰을 소비(조회 + 즉시 무효화)하고 소유자를 반환한다 — 회전·로그아웃의 원자 단위.
     * 만료·회전됨·위조 토큰은 빈 값. 재사용 감지용 패밀리 추적은 MVP 범위 밖 (api-spec 2.2).
     */
    Optional<Long> consume(String refreshToken);

    /** 사용자의 모든 리프레시 토큰 무효화 — 비밀번호 설정·재발급·계정/가맹점 비활성화 시 (api-spec 2.2). */
    void revokeAll(Long userId);
}
