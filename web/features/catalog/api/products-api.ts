import { api } from "@/lib/api/client";
import type { PageResponse } from "@/lib/api/types";
import type { Product, ProductListQuery } from "../types";

/** GET /products — 테넌트 소속 전체 (스펙 3.2.2, 공통 페이징 1.5) */
export function listProductsApi(
  query: ProductListQuery = {},
): Promise<PageResponse<Product>> {
  return api.get<PageResponse<Product>>("/products", { query: { ...query } });
}
