"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useToast } from "@/components/ui/toast";
import { useServerDatasource, type SortModelItem } from "@/components/ui/data-grid";
import { userMessageOf } from "@/lib/api/error-messages";
import { listProductsApi } from "../api/products-api";
import type { ProductListQuery, ProductStatus } from "../types";

/** 필터바 상태 (스펙 3.2.2 쿼리) — 적용 즉시 반영, 검색어만 디바운스 (03 §4.3) */
export interface ProductFilters {
  keyword: string;
  category: string;
  status: ProductStatus | "";
}

export const EMPTY_FILTERS: ProductFilters = {
  keyword: "",
  category: "",
  status: "",
};

const KEYWORD_DEBOUNCE_MS = 300;

/**
 * 상품 목록 그리드 데이터 로직 (US-CAT-01).
 * 필터 상태 + Infinite Row Model datasource + 총 건수를 관리한다.
 * 정렬은 그리드 헤더(sortModel)에서 오고, 필터가 바뀌면 datasource가 새로 만들어져
 * 그리드가 첫 블록부터 다시 로드한다.
 */
export function useProductsGrid() {
  const toast = useToast();
  const [filters, setFilters] = useState<ProductFilters>(EMPTY_FILTERS);
  const [debouncedKeyword, setDebouncedKeyword] = useState("");

  useEffect(() => {
    const timer = setTimeout(
      () => setDebouncedKeyword(filters.keyword.trim()),
      KEYWORD_DEBOUNCE_MS,
    );
    return () => clearTimeout(timer);
  }, [filters.keyword]);

  const query = useMemo<Omit<ProductListQuery, "page" | "size" | "sort">>(
    () => ({
      keyword: debouncedKeyword || undefined,
      category: filters.category.trim() || undefined,
      status: filters.status || undefined,
    }),
    [debouncedKeyword, filters.category, filters.status],
  );

  const fetchPage = useCallback(
    (page: number, size: number, sort: SortModelItem | undefined) =>
      listProductsApi({
        ...query,
        page,
        size,
        // 정렬 허용 필드(3.2.2)만 colDef에서 sortable이므로 여기서는 그대로 전달
        sort: sort ? `${sort.colId},${sort.sort}` : undefined,
      }),
    [query],
  );

  const { datasource, totalElements } = useServerDatasource(
    fetchPage,
    (error) => toast.show({ variant: "error", message: userMessageOf(error) }),
  );

  return { filters, setFilters, datasource, totalElements };
}
