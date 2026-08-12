"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import type { PageResponse } from "@/lib/api/types";
import { Button } from "./button";

/**
 * 목록 하단 페이지네이션 — 공통 페이징 규약(스펙 1.5)의 `page` 응답을 그대로 받는다.
 * AG-Grid 도입 전 단순 테이블용. 총건수·현재 페이지 표기 + 이전/다음 이동.
 */
export function Pagination({
  page,
  onPageChange,
}: {
  page: PageResponse<unknown>["page"];
  onPageChange: (page: number) => void;
}) {
  if (page.totalElements === 0) return null;

  return (
    <div className="flex items-center justify-between">
      <p className="text-caption text-fg-caption">
        총 <span className="text-num">{page.totalElements.toLocaleString()}</span>건
      </p>
      <div className="flex items-center gap-2">
        <Button
          variant="secondary"
          size="sm"
          iconOnly
          aria-label="이전 페이지"
          disabled={page.number === 0}
          onClick={() => onPageChange(page.number - 1)}
        >
          <ChevronLeft size={16} strokeWidth={1.5} />
        </Button>
        <span className="text-caption text-fg-body">
          <span className="text-num">{page.number + 1}</span> /{" "}
          <span className="text-num">{Math.max(page.totalPages, 1)}</span> 페이지
        </span>
        <Button
          variant="secondary"
          size="sm"
          iconOnly
          aria-label="다음 페이지"
          disabled={page.number >= page.totalPages - 1}
          onClick={() => onPageChange(page.number + 1)}
        >
          <ChevronRight size={16} strokeWidth={1.5} />
        </Button>
      </div>
    </div>
  );
}
