import { api } from "@/lib/api/client";
import type { PageResponse } from "@/lib/api/types";
import type { CreateStoreRequest, Store, StoreListQuery } from "../types";

/** GET /stores — HQ_ADMIN, HQ_MANAGER (스펙 2.4.7, 공통 페이징 1.5) */
export function listStoresApi(
  query: StoreListQuery = {},
): Promise<PageResponse<Store>> {
  return api.get<PageResponse<Store>>("/stores", { query: { ...query } });
}

/** POST /stores — HQ_ADMIN (스펙 2.4.6) */
export function createStoreApi(body: CreateStoreRequest): Promise<Store> {
  return api.post<Store>("/stores", { body });
}

/** POST /stores/{storeId}/deactivate — HQ_ADMIN (스펙 2.4.8). 재활성화 API는 MVP에 없다 */
export function deactivateStoreApi(storeId: number): Promise<Store> {
  return api.post<Store>(`/stores/${storeId}/deactivate`);
}
