"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import {
  AllCommunityModule,
  ModuleRegistry,
  themeQuartz,
  type ColDef,
  type GridApi,
  type GridReadyEvent,
  type IDatasource,
  type SortModelItem,
} from "ag-grid-community";
import { AG_GRID_LOCALE_KR } from "@ag-grid-community/locale";
import { AgGridReact } from "ag-grid-react";
import type { PageResponse } from "@/lib/api/types";

/**
 * AG-Grid 공통 래퍼 (03-web-components §4) — 상품 목록(US-CAT-01)이 첫 사용처,
 * M3 수주 그리드가 재사용하는 전제로 만든다. 선택지·근거는 study/ag-grid.md.
 *
 * - 테마: Quartz 위에 §4.1 오버라이드 표의 토큰만 얹는다 (D0 결정 #6).
 *   디자인 시스템 구현 레이어이므로 core 토큰(neutral-50) 참조 허용 (01 §0).
 * - 행 모델: Infinite Row Model — 서버 공통 페이징(스펙 1.5, 최대 size 100)을
 *   블록 단위로 이어 받아 §4.3의 "Virtual Scroll, 페이지네이션 없음"을 충족한다.
 */

// 모듈은 앱 수명 동안 1회 등록. AllCommunity는 미사용 기능까지 포함하지만(번들 +α)
// 화면마다 필요 모듈을 고르다 누락으로 런타임 에러를 내는 것보다 안전을 택했다.
ModuleRegistry.registerModules([AllCommunityModule]);

const gridTheme = themeQuartz.withParams({
  fontFamily: "var(--font-pretendard), sans-serif",
  fontSize: 13, // web.body
  rowHeight: 36,
  headerHeight: 32,
  headerBackgroundColor: "var(--core-color-neutral-50)",
  headerTextColor: "var(--color-fg-caption)",
  headerFontSize: 12, // web.caption
  headerFontWeight: 500,
  headerRowBorder: { color: "var(--color-border-strong)" },
  rowBorder: { color: "var(--color-border)" }, // 수평선만
  columnBorder: false, // 수직선 없음 — 밀도 확보
  oddRowBackgroundColor: "transparent", // 줄무늬 없음
  rowHoverColor: "var(--core-color-neutral-50)",
  selectedRowBackgroundColor: "var(--color-grid-row-selected)",
  accentColor: "var(--color-primary)", // 선택 체크박스·포커스
  cellHorizontalPadding: 12,
  foregroundColor: "var(--color-fg-body)",
  backgroundColor: "var(--color-surface)",
  borderColor: "var(--color-border)",
  wrapperBorder: false, // 카드 컨테이너(rounded-lg border)가 외곽을 담당한다
  wrapperBorderRadius: 8,
});

/** 기본 컬럼 옵션 — 정렬은 서버 정렬 허용 필드에만 colDef로 명시적으로 켠다 */
const DEFAULT_COL_DEF: ColDef = {
  sortable: false,
  resizable: true,
  suppressMovable: true,
};

interface DataGridProps<T> {
  columnDefs: ColDef<T>[];
  datasource: IDatasource;
  /** 그리드 영역 높이. 셸 콘텐츠 안에서 flex로 채우려면 "100%" */
  height?: number | string;
  onGridReady?: (event: GridReadyEvent) => void;
}

export function DataGrid<T>({
  columnDefs,
  datasource,
  height = "100%",
  onGridReady,
}: DataGridProps<T>) {
  return (
    <div style={{ height }} className="min-h-0">
      <AgGridReact<T>
        theme={gridTheme}
        localeText={AG_GRID_LOCALE_KR}
        columnDefs={columnDefs}
        defaultColDef={DEFAULT_COL_DEF}
        rowModelType="infinite"
        datasource={datasource}
        // 블록 = 서버 페이징 1페이지. 스펙 1.5 최대 size(100)에 맞춘다
        cacheBlockSize={100}
        // 뒤로 스크롤한 블록도 유지 — 목록 왕복 시 재요청 방지 (필터 변경 시엔 datasource 교체로 초기화)
        maxBlocksInCache={20}
        cellSelection={false}
        suppressCellFocus
        onGridReady={onGridReady}
      />
    </div>
  );
}

/**
 * 서버 페이징(스펙 1.5) ↔ Infinite Row Model 연결 헬퍼.
 * `fetchPage`가 바뀌면(필터·정렬 외부 상태 변경) 새 datasource를 만들어 그리드가
 * 처음부터 다시 로드하게 한다. 총 건수는 필터바 우측 "총 N건" 표시용으로 함께 준다.
 *
 * React Query를 쓰지 않는 이유: Infinite Row Model이 블록 캐시·중복 요청 방지를
 * 자체 관리하므로 캐시 레이어가 이중이 된다. 대신 API 함수는 훅 레이어에서만 호출해
 * View/로직 분리 원칙(CLAUDE.md 구현 원칙 1)을 지킨다 — 근거: study/ag-grid.md.
 */
export function useServerDatasource<T>(
  fetchPage: (
    page: number,
    size: number,
    sort: SortModelItem | undefined,
  ) => Promise<PageResponse<T>>,
  onError?: (error: unknown) => void,
): { datasource: IDatasource; totalElements: number | null } {
  const [totalElements, setTotalElements] = useState<number | null>(null);
  // 콜백 최신값 참조 (datasource 재생성은 fetchPage 변경에만 반응)
  const onErrorRef = useRef(onError);
  useEffect(() => {
    onErrorRef.current = onError;
  });

  const datasource = useMemo<IDatasource>(() => {
    return {
      getRows: (params) => {
        const size = params.endRow - params.startRow;
        const page = Math.floor(params.startRow / size);
        fetchPage(page, size, params.sortModel[0])
          .then((result) => {
            setTotalElements(result.page.totalElements);
            params.successCallback(result.items, result.page.totalElements);
          })
          .catch((error) => {
            onErrorRef.current?.(error);
            params.failCallback();
          });
      },
    };
  }, [fetchPage]);

  return { datasource, totalElements };
}

export type { ColDef, GridApi, IDatasource, SortModelItem };
