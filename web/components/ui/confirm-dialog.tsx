"use client";

import { useEffect, useRef } from "react";
import { X } from "lucide-react";
import { Button } from "./button";

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  /**
   * 결과 요약 — "발주 12건이 SHIPPED로 전이됩니다"처럼 무슨 일이 벌어지는지 적는다 (02 §4.4).
   */
  description?: string;
  /**
   * 확인 버튼 라벨. **동사로 쓴다** — "확인"이 아니라 "출하 확정" / "비활성화" (02 §4.4).
   */
  confirmLabel: string;
  /** 파괴적 액션(거절·취소·계정 비활성화)이면 확인 버튼이 danger 색이 된다 */
  danger?: boolean;
  /** 확인 처리 진행 중 — 버튼에 스피너가 뜨고 다이얼로그는 닫히지 않는다 */
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

/**
 * 확인 다이얼로그 (02-patterns §4.4 + 03-web-components §6.1 모달 sm=400).
 * 되돌릴 수 있는 액션에는 이걸 쓰지 말고 토스트 + 실행취소를 쓴다.
 */
export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  danger = false,
  loading = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const panel = panelRef.current;
    const previouslyFocused = document.activeElement as HTMLElement | null;
    // 파괴적 동작 확인이므로 초기 포커스는 안전한 쪽(취소)에 둔다
    panel?.querySelector<HTMLElement>("[data-autofocus]")?.focus();

    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        e.stopPropagation();
        // 진행 중에는 닫지 않는다 (03 §6.1)
        if (!loading) onCancel();
        return;
      }
      // 포커스가 다이얼로그 밖(배경)으로 나가지 않게 Tab을 안에서 순환시킨다
      if (e.key === "Tab" && panel) {
        const focusables = Array.from(
          panel.querySelectorAll<HTMLElement>("button:not([disabled])"),
        );
        if (focusables.length === 0) return;
        const first = focusables[0];
        const last = focusables[focusables.length - 1];
        const current = document.activeElement;
        if (e.shiftKey && (current === first || !panel.contains(current))) {
          e.preventDefault();
          last.focus();
        } else if (!e.shiftKey && (current === last || !panel.contains(current))) {
          e.preventDefault();
          first.focus();
        }
      }
    };

    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      previouslyFocused?.focus();
    };
  }, [open, loading, onCancel]);

  if (!open) return null;

  return (
    <div
      // 오버레이 rgba(15,23,42,0.5) = neutral-900 50% (03 §6.1)
      className="fixed inset-0 z-50 flex items-center justify-center bg-[var(--core-color-neutral-900)]/50"
      // click 대신 mousedown 기준 — 패널 안에서 드래그를 시작해 배경에서 놓아도 닫히지 않게
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !loading) onCancel();
      }}
    >
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="w-100 rounded-lg bg-surface shadow-3"
      >
        <div className="flex items-start justify-between gap-4 px-6 pt-5">
          <h2 className="text-heading text-fg-title">{title}</h2>
          <Button
            variant="ghost"
            size="sm"
            iconOnly
            aria-label="닫기"
            disabled={loading}
            onClick={onCancel}
            className="-mr-1.5 -mt-0.5"
          >
            <X size={16} strokeWidth={1.5} />
          </Button>
        </div>
        {description && (
          <p className="px-6 pt-2 text-body-md text-fg-body">{description}</p>
        )}
        <div className="flex justify-end gap-2 px-6 pb-5 pt-6">
          <Button
            variant="secondary"
            data-autofocus
            disabled={loading}
            onClick={onCancel}
          >
            취소
          </Button>
          <Button
            variant={danger ? "danger" : "primary"}
            loading={loading}
            onClick={onConfirm}
          >
            {confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  );
}
