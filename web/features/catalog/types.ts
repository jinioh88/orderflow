// 상품 카탈로그 도메인 타입 — 정본: server/docs/api/03-cat.md (확정 2026-08-11)

import type { PageQuery } from "@/lib/api/types";

export type ProductStatus = "ON_SALE" | "SUSPENDED";

/** 상품 객체 (스펙 3.1) — 목록·단건·수정 응답 공통 스키마 */
export interface Product {
  id: number;
  /** 테넌트 내 유일, 등록 후 수정 불가 — 엑셀 매칭 키 */
  productCode: string;
  name: string;
  barcode: string;
  /** 자유 문자열 — MVP는 계층 카테고리 미도입 */
  category: string;
  /** 발주 단위, 자유 문자열 (BOX, EA, KG …) */
  orderUnit: string;
  /** KRW 정수 */
  unitPrice: number;
  /** 한정 품목 여부 (US-CAT-04) */
  limited: boolean;
  /** 본사 가용 재고 — 한정 품목일 때만 값, 아니면 null */
  availableQty: number | null;
  status: ProductStatus;
  createdAt: string;
}

/** GET /products 쿼리 (스펙 3.2.2) — 정렬 허용: productCode, name, unitPrice, createdAt */
export interface ProductListQuery extends PageQuery {
  /** 품목코드·품명·바코드 부분 일치 */
  keyword?: string;
  /** 정확 일치 */
  category?: string;
  status?: ProductStatus;
  limited?: boolean;
}

export const PRODUCT_STATUS_LABELS: Record<ProductStatus, string> = {
  ON_SALE: "판매중",
  SUSPENDED: "판매중지",
};
