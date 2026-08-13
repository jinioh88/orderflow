// 계정 도메인 타입 — 정본: server/docs/api/02-auth.md 2.4.9~2.4.12

import type { Role } from "@/features/auth/types";
import type { PageQuery } from "@/lib/api/types";

export type AccountStatus = "ACTIVE" | "INACTIVE";

/** 계정 목록 items 원소 (스펙 2.4.11) — temporaryPassword는 절대 포함되지 않는다 */
export interface Account {
  id: number;
  email: string;
  name: string;
  role: Role;
  storeId: number | null;
  status: AccountStatus;
  passwordSetupRequired: boolean;
  createdAt: string;
}

/** POST /users — MVP에서 만들 수 있는 역할은 STORE_OWNER뿐 (요청에 role 없음, 스펙 2.4.9) */
export interface CreateAccountRequest {
  storeId: number;
  email: string;
  name: string;
}

/** 등록 응답(2.4.9) — 임시 비밀번호는 이 응답에 1회만 평문 포함된다 (스펙 2.3) */
export interface CreateAccountResponse extends Account {
  temporaryPassword: string;
}

/** POST /users/{userId}/temporary-password 응답 (스펙 2.4.10) */
export interface TemporaryPasswordResponse {
  temporaryPassword: string;
}

/** GET /users 쿼리 (2.4.11) — 정렬 허용: name, createdAt (기본 createdAt,desc) */
export interface AccountListQuery extends PageQuery {
  storeId?: number;
  status?: AccountStatus;
  role?: Role;
}
