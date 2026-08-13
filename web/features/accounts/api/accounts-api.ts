import { api } from "@/lib/api/client";
import type { PageResponse } from "@/lib/api/types";
import type {
  Account,
  AccountListQuery,
  CreateAccountRequest,
  CreateAccountResponse,
  TemporaryPasswordResponse,
} from "../types";

/** GET /users — HQ_ADMIN, HQ_MANAGER (스펙 2.4.11, 공통 페이징 1.5) */
export function listAccountsApi(
  query: AccountListQuery = {},
): Promise<PageResponse<Account>> {
  return api.get<PageResponse<Account>>("/users", { query: { ...query } });
}

/** POST /users — HQ_ADMIN, 점주 계정 등록 (스펙 2.4.9) */
export function createAccountApi(
  body: CreateAccountRequest,
): Promise<CreateAccountResponse> {
  return api.post<CreateAccountResponse>("/users", { body });
}

/** POST /users/{userId}/temporary-password — HQ_ADMIN (스펙 2.4.10). 자기 자신은 409 */
export function reissueTemporaryPasswordApi(
  userId: number,
): Promise<TemporaryPasswordResponse> {
  return api.post<TemporaryPasswordResponse>(
    `/users/${userId}/temporary-password`,
  );
}

/** POST /users/{userId}/deactivate — HQ_ADMIN (스펙 2.4.12). 자기 자신은 409 */
export function deactivateAccountApi(userId: number): Promise<Account> {
  return api.post<Account>(`/users/${userId}/deactivate`);
}
