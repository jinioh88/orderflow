package com.orderflow.domain.common;

import java.util.function.Supplier;
import org.springframework.util.Assert;

/**
 * 요청별 테넌트 컨텍스트 (US-AUTH-04, 설계 노트 §1) — ThreadLocal 3-상태.
 *
 * <ul>
 *   <li>TENANT(id): 테넌트 스코프 요청 — JWT 인증 필터가 토큰의 tenant_id 클레임으로 설정</li>
 *   <li>UNFILTERED: 전역 접근 — SYSTEM 역할 인증 시 또는 {@link #runUnfiltered} 명시 블록</li>
 *   <li>미설정(NONE): 필터가 불가능 값({@link TenantFilter#NO_TENANT})으로 켜져 빈 결과 (fail-closed)</li>
 * </ul>
 *
 * 필터는 트랜잭션 시작 시점에 세션에 활성화되므로, 컨텍스트는 반드시 트랜잭션 시작 <b>전에</b>
 * 확정되어야 한다. {@link #runUnfiltered}도 @Transactional 경계 바깥에서 감싸야 효과가 있다.
 */
public final class TenantContext {

    private enum Mode { TENANT, UNFILTERED }

    private record Scope(Mode mode, Long tenantId) {
    }

    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    /** 테넌트 스코프 설정 — 인증 필터 전용. 이미 설정된 상태면 이전 요청 누수(버그)로 보고 즉시 실패한다. */
    public static void setTenant(Long tenantId) {
        Assert.notNull(tenantId, "tenantId는 필수다");
        assertNotSet();
        CURRENT.set(new Scope(Mode.TENANT, tenantId));
    }

    /** 전역 접근 설정 — SYSTEM 역할 인증 전용. */
    public static void setUnfiltered() {
        assertNotSet();
        CURRENT.set(new Scope(Mode.UNFILTERED, null));
    }

    /**
     * 명시적 필터 우회 블록 — 인증 전 전역 조회(로그인의 findByEmail, 토큰 재발급)만 사용한다.
     * 사용처는 아키텍처 테스트 화이트리스트로 관리한다 (설계 노트 §5-8).
     * 반드시 트랜잭션 시작 전에 감싸야 한다 (이미 열린 세션의 필터는 바뀌지 않는다).
     */
    public static <T> T runUnfiltered(Supplier<T> action) {
        Scope previous = CURRENT.get();
        CURRENT.set(new Scope(Mode.UNFILTERED, null));
        try {
            return action.get();
        } finally {
            if (previous != null) {
                CURRENT.set(previous);
            } else {
                CURRENT.remove();
            }
        }
    }

    public static boolean isUnfiltered() {
        Scope scope = CURRENT.get();
        return scope != null && scope.mode() == Mode.UNFILTERED;
    }

    /** 필터 파라미터 값 — 미설정(NONE)이면 fail-closed 센티널. UNFILTERED 상태에서 부르면 버그다. */
    public static long tenantIdOrSentinel() {
        Scope scope = CURRENT.get();
        if (scope == null) {
            return TenantFilter.NO_TENANT;
        }
        if (scope.mode() == Mode.UNFILTERED) {
            throw new IllegalStateException("UNFILTERED 컨텍스트에는 테넌트 ID가 없다");
        }
        return scope.tenantId();
    }

    /** 요청 종료 시 반드시 호출 — 스레드 풀 재사용 누수 방지 (서블릿 필터 finally). */
    public static void clear() {
        CURRENT.remove();
    }

    private static void assertNotSet() {
        if (CURRENT.get() != null) {
            throw new IllegalStateException("TenantContext가 이미 설정되어 있다 — 이전 요청의 해제 누락이 의심된다");
        }
    }
}
