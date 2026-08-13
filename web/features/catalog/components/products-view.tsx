"use client";

import { Search } from "lucide-react";
import type { ICellRendererParams } from "ag-grid-community";
import { ActiveBadge } from "@/components/ui/active-badge";
import { DataGrid, type ColDef } from "@/components/ui/data-grid";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { useProductsGrid } from "../hooks/use-products-grid";
import {
  PRODUCT_STATUS_LABELS,
  type Product,
  type ProductStatus,
} from "../types";

/**
 * 상품 목록 (US-CAT-01, 03 §4.3 필터바 + Virtual Scroll 그리드).
 * 필터는 적용 즉시 반영(적용 버튼 없음), 총 건수는 필터 반영 건수.
 * 컬럼 정렬은 서버 정렬 허용 필드(3.2.2)에만 켠다.
 */

function StatusCell(params: ICellRendererParams<Product, ProductStatus>) {
  if (!params.data) return null; // 로딩 중 블록
  return (
    <ActiveBadge
      active={params.value === "ON_SALE"}
      activeLabel={PRODUCT_STATUS_LABELS.ON_SALE}
      inactiveLabel={PRODUCT_STATUS_LABELS.SUSPENDED}
    />
  );
}

/** 한정 품목 미니 뱃지 — warning 계열 (04 §? 앱 규칙과 톤 통일) */
function LimitedCell(params: ICellRendererParams<Product, boolean>) {
  if (!params.data) return null;
  if (!params.value) return <span className="text-fg-disabled">—</span>;
  return (
    <span className="inline-flex h-5 items-center rounded-full bg-warning-bg px-2 text-caption font-medium text-warning-text">
      한정
    </span>
  );
}

const COLUMN_DEFS: ColDef<Product>[] = [
  { field: "productCode", headerName: "품목코드", width: 130, sortable: true },
  { field: "name", headerName: "상품명", flex: 1, minWidth: 200, sortable: true },
  { field: "barcode", headerName: "바코드", width: 150 },
  {
    field: "unitPrice",
    headerName: "단가",
    width: 110,
    sortable: true,
    // 숫자는 우측 정렬 + tabular (03 §4.2). 포맷은 천 단위 구분
    type: "rightAligned",
    headerClass: "ag-right-aligned-header",
    cellClass: "text-num tabular-nums",
    valueFormatter: ({ value }) =>
      value === null || value === undefined ? "" : value.toLocaleString(),
  },
  { field: "orderUnit", headerName: "발주 단위", width: 100 },
  { field: "category", headerName: "카테고리", width: 130 },
  {
    field: "status",
    headerName: "판매 상태",
    width: 110,
    cellRenderer: StatusCell,
  },
  {
    field: "limited",
    headerName: "한정 품목",
    width: 100,
    cellRenderer: LimitedCell,
  },
];

export function ProductsView() {
  const { filters, setFilters, datasource, totalElements } = useProductsGrid();

  return (
    <div className="flex h-full min-h-0 flex-col gap-4">
      {/* 필터바 (03 §4.3): 적용 즉시 반영. 프리셋 칩은 화면 정의서 확정 후 (수주 M3 기본안) */}
      <div className="flex shrink-0 items-center gap-2">
        <Input
          icon={<Search size={16} strokeWidth={1.5} />}
          placeholder="품목코드·품명·바코드 검색"
          value={filters.keyword}
          onChange={(e) =>
            setFilters((f) => ({ ...f, keyword: e.target.value }))
          }
          className="w-60"
          aria-label="상품 검색"
        />
        <Input
          placeholder="카테고리 (정확 일치)"
          value={filters.category}
          onChange={(e) =>
            setFilters((f) => ({ ...f, category: e.target.value }))
          }
          className="w-44"
          aria-label="카테고리 필터"
        />
        <Select
          value={filters.status}
          onChange={(e) =>
            setFilters((f) => ({
              ...f,
              status: e.target.value as ProductStatus | "",
            }))
          }
          className="w-32"
          aria-label="판매 상태 필터"
        >
          <option value="">상태 전체</option>
          <option value="ON_SALE">{PRODUCT_STATUS_LABELS.ON_SALE}</option>
          <option value="SUSPENDED">{PRODUCT_STATUS_LABELS.SUSPENDED}</option>
        </Select>
        <p className="ml-auto text-caption text-fg-caption">
          총{" "}
          <span className="text-num">
            {totalElements === null ? "—" : totalElements.toLocaleString()}
          </span>
          건
        </p>
      </div>
      <div className="min-h-0 flex-1 overflow-hidden rounded-lg border border-border bg-surface">
        <DataGrid<Product> columnDefs={COLUMN_DEFS} datasource={datasource} />
      </div>
    </div>
  );
}
