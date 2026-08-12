"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { listStoresApi } from "../api/stores-api";
import type { StoreListQuery } from "../types";

export const storesKeys = {
  all: ["stores"] as const,
  list: (query: StoreListQuery) => ["stores", "list", query] as const,
};

/** 가맹점 목록 (US-AUTH-02). 페이지 이동 시 이전 데이터를 유지해 깜빡임을 막는다 (02 §2.2) */
export function useStores(query: StoreListQuery = {}) {
  return useQuery({
    queryKey: storesKeys.list(query),
    queryFn: () => listStoresApi(query),
    placeholderData: keepPreviousData,
  });
}
