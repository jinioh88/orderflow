// 가맹점 도메인 타입 — 정본: server/docs/api/02-auth.md 2.4.6~2.4.8

import type { PageQuery } from "@/lib/api/types";

export type StoreStatus = "ACTIVE" | "INACTIVE";

/** 가맹점 (스펙 2.4.6 응답 · 2.4.7 items 원소) */
export interface Store {
  id: number;
  name: string;
  status: StoreStatus;
  address: string | null;
  createdAt: string;
}

export interface CreateStoreRequest {
  name: string;
  /** 선택 (스펙 2.4.6) */
  address?: string;
}

/** GET /stores 쿼리 (2.4.7) — 정렬 허용: name, createdAt (기본 createdAt,desc) */
export interface StoreListQuery extends PageQuery {
  status?: StoreStatus;
}
