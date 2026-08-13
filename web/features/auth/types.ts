// AUTH 도메인 타입 — 정본: server/docs/api/02-auth.md (확정 2026-07-19)

/** 역할 5종 (스펙 2.1) */
export type Role =
  | "SYSTEM"
  | "HQ_ADMIN"
  | "HQ_MANAGER"
  | "STORE_OWNER"
  | "STORE_STAFF";

export const ROLE_LABELS: Record<Role, string> = {
  SYSTEM: "시스템 관리자",
  HQ_ADMIN: "본사 관리자",
  HQ_MANAGER: "본사 운영자",
  STORE_OWNER: "점주",
  STORE_STAFF: "점주 직원",
};

/** 로그인 응답의 user (스펙 2.4.2). SYSTEM은 tenantId/storeId가 null */
export interface AuthUser {
  id: number;
  email: string;
  name: string;
  role: Role;
  tenantId: number | null;
  storeId: number | null;
}

export interface LoginRequest {
  email: string;
  password: string;
}

/** 로그인 응답의 소속 테넌트 요약 (스펙 2.4.2, 수정요청 20260812 반영). id는 user.tenantId와 항상 동일 */
export interface TenantSummary {
  id: number;
  name: string;
}

/** POST /auth/login 응답 (스펙 2.4.2) */
export interface LoginResponse {
  accessToken: string;
  accessTokenExpiresIn: number;
  refreshToken: string;
  passwordSetupRequired: boolean;
  user: AuthUser;
  /** SYSTEM 로그인은 null */
  tenant: TenantSummary | null;
}

/** POST /auth/refresh 응답 — 회전된 새 토큰 쌍 (스펙 2.4.3) */
export interface RefreshResponse {
  accessToken: string;
  accessTokenExpiresIn: number;
  refreshToken: string;
}

export interface SetPasswordRequest {
  currentPassword: string;
  newPassword: string;
}

/** PUT /users/me/password 응답 — 기존 리프레시 토큰 전체 무효화 후 새 쌍 반환 (스펙 2.4.4) */
export interface SetPasswordResponse {
  accessToken: string;
  accessTokenExpiresIn: number;
  refreshToken: string;
  passwordSetupRequired: boolean;
}

/** 액세스 토큰 JWT 클레임 (스펙 2.2). 표시·메뉴 노출 판단용 — 권한 강제는 항상 서버가 한다 */
export interface JwtClaims {
  sub: string;
  tenant_id: number | null;
  store_id: number | null;
  role: Role;
  iat: number;
  exp: number;
}

/**
 * 클라이언트에서 합성하는 AUTH 코드 — 서버 응답이 아니다.
 * 웹 접근 불가 역할(점주 등)의 로그인은 성공 응답을 받아도 세션을 만들지 않고
 * 이 코드로 실패 처리한다 (사용자 결정 2026-08-13).
 */
export const AUTH_CLIENT_ERROR_CODES = {
  WEB_ACCESS_DENIED: "WEB_ACCESS_DENIED",
} as const;

/** AUTH 에픽이 추가하는 에러 코드 (스펙 2.5) */
export const AUTH_ERROR_CODES = {
  INVALID_CREDENTIALS: "INVALID_CREDENTIALS",
  INVALID_REFRESH_TOKEN: "INVALID_REFRESH_TOKEN",
  ACCOUNT_INACTIVE: "ACCOUNT_INACTIVE",
  PASSWORD_SETUP_REQUIRED: "PASSWORD_SETUP_REQUIRED",
  EMAIL_DUPLICATED: "EMAIL_DUPLICATED",
  STORE_INACTIVE: "STORE_INACTIVE",
} as const;
