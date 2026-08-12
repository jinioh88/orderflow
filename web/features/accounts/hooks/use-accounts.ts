"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { listAccountsApi } from "../api/accounts-api";
import type { AccountListQuery } from "../types";

export const accountsKeys = {
  all: ["accounts"] as const,
  list: (query: AccountListQuery) => ["accounts", "list", query] as const,
};

/** 계정 목록 (US-AUTH-02) */
export function useAccounts(query: AccountListQuery = {}) {
  return useQuery({
    queryKey: accountsKeys.list(query),
    queryFn: () => listAccountsApi(query),
    placeholderData: keepPreviousData,
  });
}
