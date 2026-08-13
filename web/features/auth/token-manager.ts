import {
  setAccessTokenProvider,
  setTokenRefreshHandler,
  setUnauthorizedHandler,
} from "@/lib/api/client";
import { ApiError } from "@/lib/api/types";
import { refreshApi } from "./api/auth-api";
import { AUTH_ERROR_CODES, type AuthUser } from "./types";

/**
 * 토큰 저장 전략 (사용자 확정 2026-08-12, 근거: study/auth-tokens.md)
 * - 액세스 토큰(TTL 30분): **모듈 메모리에만** 둔다. XSS로 localStorage를 읽혀도
 *   즉시 쓸 수 있는 토큰은 없다.
 * - 리프레시 토큰(TTL 14일 슬라이딩·회전): localStorage. 새로고침 시 silent refresh로
 *   세션을 복원한다. 스펙이 본문 왕복(2.4.3)이라 httpOnly 쿠키는 선택지에 없다.
 * - 사용자 프로필: localStorage. 표시용이다 — 권한 강제는 항상 서버가 한다 (NFR-2.3).
 */

const REFRESH_TOKEN_KEY = "orderflow.auth.refreshToken";
const SESSION_KEY = "orderflow.auth.session";

/** 재발급 응답(2.4.3)에는 user가 없으므로 새로고침 복원용 표시 정보를 함께 저장한다 */
export interface SessionSnapshot {
  user: AuthUser;
  passwordSetupRequired: boolean;
  /** 사이드바 표시용 테넌트명 (스펙 2.4.2 tenant.name). 구버전 스냅샷에는 없을 수 있다 */
  tenantName?: string | null;
}

let accessToken: string | null = null;

function storage(): Storage | null {
  return typeof window === "undefined" ? null : window.localStorage;
}

export function getAccessToken(): string | null {
  return accessToken;
}

export function getStoredRefreshToken(): string | null {
  return storage()?.getItem(REFRESH_TOKEN_KEY) ?? null;
}

export function getStoredSession(): SessionSnapshot | null {
  const raw = storage()?.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as SessionSnapshot;
  } catch {
    return null;
  }
}

export function startSession(
  tokens: { accessToken: string; refreshToken: string },
  snapshot: SessionSnapshot,
) {
  accessToken = tokens.accessToken;
  storage()?.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
  storage()?.setItem(SESSION_KEY, JSON.stringify(snapshot));
}

/** 회전(스펙 2.2): 재발급·비밀번호 설정 응답의 새 토큰 쌍으로 교체한다 */
export function updateTokens(tokens: {
  accessToken: string;
  refreshToken: string;
}) {
  accessToken = tokens.accessToken;
  storage()?.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
}

export function updateSnapshot(patch: Partial<SessionSnapshot>) {
  const current = getStoredSession();
  if (!current) return;
  storage()?.setItem(SESSION_KEY, JSON.stringify({ ...current, ...patch }));
}

// ── 세션 종료 구독 — 인터셉터가 세션을 정리하면 AuthProvider가 상태를 내린다 ──

const clearedListeners = new Set<() => void>();

export function subscribeSessionCleared(listener: () => void): () => void {
  clearedListeners.add(listener);
  return () => clearedListeners.delete(listener);
}

export function clearSession() {
  accessToken = null;
  storage()?.removeItem(REFRESH_TOKEN_KEY);
  storage()?.removeItem(SESSION_KEY);
  clearedListeners.forEach((listener) => listener());
}

// ── silent refresh (single-flight) ──

let refreshInFlight: Promise<string | null> | null = null;

/**
 * 동시 다발 401이 와도 재발급 요청은 1개만 나간다 — 회전(2.2) 때문에 필수다:
 * 두 번째 재발급 요청이 이미 회전된 토큰을 쓰면 `INVALID_REFRESH_TOKEN`으로
 * 멀쩡한 세션이 끊긴다.
 */
export function refreshSession(): Promise<string | null> {
  refreshInFlight ??= doRefresh().finally(() => {
    refreshInFlight = null;
  });
  return refreshInFlight;
}

async function doRefresh(): Promise<string | null> {
  const storedToken = getStoredRefreshToken();
  if (!storedToken) return null;
  try {
    const data = await refreshApi(storedToken);
    updateTokens(data);
    return data.accessToken;
  } catch (error) {
    // 401(만료·회전·무효화)만 세션 종료. 네트워크 단절 등 일시 오류에 로그아웃시키지 않는다
    if (error instanceof ApiError && error.status === 401) clearSession();
    return null;
  }
}

/**
 * lib/api/client의 인증 연동 지점에 구현체를 등록한다.
 * AuthProvider 최초 렌더에서 1회 호출 — 자식의 어떤 쿼리보다 먼저다.
 */
export function installAuthInterceptors() {
  setAccessTokenProvider(getAccessToken);
  setTokenRefreshHandler(refreshSession);
  setUnauthorizedHandler((error) => {
    // INVALID_CREDENTIALS는 로그인·비밀번호 폼의 입력 오류다(2.5) — 세션과 무관하므로 유지
    if (error.code === AUTH_ERROR_CODES.INVALID_CREDENTIALS) return;
    clearSession();
  });
}
