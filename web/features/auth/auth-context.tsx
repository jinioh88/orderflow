"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { decodeJwtClaims } from "./jwt";
import { registerAuthMessages } from "./messages";
import {
  clearSession,
  getAccessToken,
  getStoredSession,
  installAuthInterceptors,
  refreshSession,
  startSession,
  subscribeSessionCleared,
  updateSnapshot,
  updateTokens,
} from "./token-manager";
import type {
  AuthUser,
  LoginResponse,
  Role,
  SetPasswordResponse,
} from "./types";

/**
 * 인증 전역 상태 (US-AUTH-03).
 * - `loading`: 새로고침 직후 silent refresh로 세션 복원을 시도하는 중 — 가드는 이 동안
 *   리다이렉트 판단을 보류한다 (안 하면 새로고침마다 로그인으로 튕긴다).
 * - role은 저장된 프로필이 아니라 **액세스 토큰의 JWT 클레임**에서 읽는다(백로그 지시).
 *   재발급으로 role이 바뀌어도 다음 복원부터 반영된다.
 */
type AuthStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthState {
  status: AuthStatus;
  user: AuthUser | null;
  role: Role | null;
  passwordSetupRequired: boolean;
  /** 소속 테넌트명 (스펙 2.4.2) — 사이드바 로고 영역 표시용 */
  tenantName: string | null;
}

interface AuthContextValue extends AuthState {
  /** 로그인 성공 응답을 세션으로 반영한다 — useLogin 훅 전용 */
  signIn: (result: LoginResponse) => void;
  /** 비밀번호 설정 성공 응답(새 토큰 쌍)을 반영한다 — useSetPassword 훅 전용 */
  completePasswordSetup: (result: SetPasswordResponse) => void;
  /** 로컬 세션 정리 + 상태 전환 — 서버 로그아웃 호출은 useLogout 훅이 한다 */
  signOut: () => void;
}

const UNAUTHENTICATED: AuthState = {
  status: "unauthenticated",
  user: null,
  role: null,
  passwordSetupRequired: false,
  tenantName: null,
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth는 AuthProvider 안에서만 쓸 수 있습니다.");
  }
  return context;
}

function roleFromToken(fallback: Role): Role {
  const token = getAccessToken();
  return (token && decodeJwtClaims(token)?.role) || fallback;
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  // 최초 렌더에서 인터셉터를 등록한다 — effect보다 먼저라 자식 쿼리가 Bearer를 놓치지 않는다
  useState(() => {
    installAuthInterceptors();
    registerAuthMessages();
    return null;
  });

  const [state, setState] = useState<AuthState>({
    ...UNAUTHENTICATED,
    status: "loading",
  });

  // 새로고침 복원: 저장된 세션이 있으면 silent refresh로 유효성을 확인한다
  useEffect(() => {
    let cancelled = false;
    (async () => {
      const snapshot = getStoredSession();
      const token = snapshot ? await refreshSession() : null;
      if (cancelled) return;
      setState(
        token && snapshot
          ? {
              status: "authenticated",
              user: snapshot.user,
              role: roleFromToken(snapshot.user.role),
              passwordSetupRequired: snapshot.passwordSetupRequired,
              tenantName: snapshot.tenantName ?? null,
            }
          : UNAUTHENTICATED,
      );
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  // 인터셉터가 세션을 끊으면(재발급 실패, 401) 상태를 따라 내린다 → 가드가 로그인으로 보낸다
  useEffect(
    () =>
      subscribeSessionCleared(() => {
        setState((current) =>
          current.status === "loading" ? current : UNAUTHENTICATED,
        );
      }),
    [],
  );

  const signIn = useCallback((result: LoginResponse) => {
    startSession(result, {
      user: result.user,
      passwordSetupRequired: result.passwordSetupRequired,
      tenantName: result.tenant?.name ?? null,
    });
    setState({
      status: "authenticated",
      user: result.user,
      role: roleFromToken(result.user.role),
      passwordSetupRequired: result.passwordSetupRequired,
      tenantName: result.tenant?.name ?? null,
    });
  }, []);

  const completePasswordSetup = useCallback((result: SetPasswordResponse) => {
    updateTokens(result);
    updateSnapshot({ passwordSetupRequired: result.passwordSetupRequired });
    setState((current) => ({
      ...current,
      passwordSetupRequired: result.passwordSetupRequired,
    }));
  }, []);

  const signOut = useCallback(() => {
    clearSession(); // 구독 콜백이 상태를 UNAUTHENTICATED로 내린다
  }, []);

  const value = useMemo(
    () => ({ ...state, signIn, completePasswordSetup, signOut }),
    [state, signIn, completePasswordSetup, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
