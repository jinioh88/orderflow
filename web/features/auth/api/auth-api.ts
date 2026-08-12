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
  return api.post<LoginResponse>("/auth/login", { body });
}

/** POST /auth/refresh — 익명, 리프레시 토큰이 자격 증명 (스펙 2.4.3) */
export function refreshApi(refreshToken: string): Promise<RefreshResponse> {
  return api.post<RefreshResponse>("/auth/refresh", { body: { refreshToken } });
}

/** PUT /users/me/password — 임시 상태에서 허용되는 유일한 업무 API (스펙 2.4.4) */
export function setPasswordApi(
  body: SetPasswordRequest,
): Promise<SetPasswordResponse> {
  return api.put<SetPasswordResponse>("/users/me/password", { body });
}

/** POST /auth/logout — 이미 무효한 토큰이어도 204 멱등 (스펙 2.4.5) */
export function logoutApi(refreshToken: string): Promise<void> {
  return api.post<void>("/auth/logout", { body: { refreshToken } });
}
