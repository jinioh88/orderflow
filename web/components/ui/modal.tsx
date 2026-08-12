"use client";

import { useEffect, useRef } from "react";
import { X } from "lucide-react";
import { cn } from "@/lib/utils/cn";
import { Button } from "./button";

/**
 * 범용 모달 (03-web-components §6.1).
 * 폭 sm 400 / md 560 / lg 720, surface + radius-lg + shadow-3, 오버레이 neutral-900 50%.
 * 구조: 헤더(heading + 닫기 X) / 본문(패딩 24) / 푸터(우측 정렬 — 호출부가 children으로 구성).
 * ESC·오버레이 클릭 닫기 허용, 단 진행 중(closeDisabled)에는 닫지 않는다.
 * 확인 전용 다이얼로그는 `ConfirmDialog`를 쓴다 — 이건 폼 등 임의 콘텐츠용.
 */

const SIZE_CLASS = { sm: "w-100", md: "w-140", lg: "w-180" } as const;

const FOCUSABLE =
  'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), a[href]';

interface ModalProps {
  open: boolean;
  title: string;
  size?: keyof typeof SIZE_CLASS;
  /** 진행 중(저장·업로드)에는 ESC·오버레이·X로 닫히지 않는다 (03 §6.1) */
  closeDisabled?: boolean;
  onClose: () => void;
  /** 푸터 버튼 영역 — 우측 정렬로 배치된다. 없으면 푸터 미출력 */
  footer?: React.ReactNode;
  children: React.ReactNode;
}

export function Modal({
  open,
  title,
  size = "sm",
  closeDisabled = false,
  onClose,
  footer,
  children,
}: ModalProps) {
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const panel = panelRef.current;
    const previouslyFocused = document.activeElement as HTMLElement | null;
    panel?.querySelector<HTMLElement>(FOCUSABLE)?.focus();

    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        e.stopPropagation();
        if (!closeDisabled) onClose();
        return;
      }
      // 포커스가 모달 밖으로 나가지 않게 Tab을 안에서 순환시킨다
      if (e.key === "Tab" && panel) {
        const focusables = Array.from(
          panel.querySelectorAll<HTMLElement>(FOCUSABLE),
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
  }, [open, closeDisabled, onClose]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-[var(--core-color-neutral-900)]/50"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !closeDisabled) onClose();
      }}
    >
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={cn("rounded-lg bg-surface shadow-3", SIZE_CLASS[size])}
      >
        <div className="flex items-start justify-between gap-4 px-6 pt-5">
          <h2 className="text-heading text-fg-title">{title}</h2>
          <Button
            variant="ghost"
            size="sm"
            iconOnly
            aria-label="닫기"
            disabled={closeDisabled}
            onClick={onClose}
            className="-mr-1.5 -mt-0.5"
          >
            <X size={16} strokeWidth={1.5} />
          </Button>
        </div>
        <div className="px-6 pt-4">{children}</div>
        {footer ? (
          <div className="flex justify-end gap-2 px-6 pb-5 pt-6">{footer}</div>
        ) : (
          <div className="pb-6" />
        )}
      </div>
    </div>
  );
}
