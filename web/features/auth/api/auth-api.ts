import { api } from "@/lib/api/client";
import type {
  LoginRequest,
  LoginResponse,
  RefreshResponse,
  SetPasswordRequest,
  SetPasswordResponse,
} from "../types";

/** POST /auth/login — 익명 (스펙 2.4.2) */
export function loginApi(body: LoginRequest): Promise<LoginResponse> {
  return api.post<LoginResponse>("/auth/login", { body, anonymous: true });
}

/** POST /auth/refresh — 익명, 리프레시 토큰이 자격 증명 (스펙 2.4.3) */
export function refreshApi(refreshToken: string): Promise<RefreshResponse> {
  return api.post<RefreshResponse>("/auth/refresh", {
    body: { refreshToken },
    // 익명 + 재시도 금지: 만료 Bearer를 싣지 않고, 재발급이 재발급을 부르는 재귀를 차단
    anonymous: true,
    skipAuthRetry: true,
  });
}

/** PUT /users/me/password — 임시 상태에서 허용되는 유일한 업무 API (스펙 2.4.4) */
export function setPasswordApi(
  body: SetPasswordRequest,
): Promise<SetPasswordResponse> {
  return api.put<SetPasswordResponse>("/users/me/password", { body });
}

/** POST /auth/logout — 이미 무효한 토큰이어도 204 멱등 (스펙 2.4.5) */
export function logoutApi(refreshToken: string): Promise<void> {
  return api.post<void>("/auth/logout", {
    body: { refreshToken },
    // 자동 재시도 금지: 재발급으로 회전된 뒤 옛 토큰을 다시 보내면 새 토큰이
    // 서버에 살아남는다 — 만료 대비는 useLogout이 선제 재발급으로 처리한다
    skipAuthRetry: true,
  });
}
