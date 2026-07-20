"use client";

import { useEffect, useState } from "react";
import { cn } from "@/lib/utils/cn";

/** 300ms 미만 응답에는 로딩을 표시하지 않는다 — 깜빡임 방지 (02-patterns §2.2) */
const LOADING_DELAY_MS = 300;

function useDelayedVisible(delayMs = LOADING_DELAY_MS): boolean {
  const [visible, setVisible] = useState(false);
  useEffect(() => {
    const timer = setTimeout(() => setVisible(true), delayMs);
    return () => clearTimeout(timer);
  }, [delayMs]);
  return visible;
}

/**
 * 화면 영역 로딩 표시. 마운트 후 300ms가 지나야 실제로 보인다.
 * 목록·카드의 최초 로드는 스피너보다 `Skeleton`이 우선이다 (02 §2.2).
 */
export function Spinner({
  label = "불러오는 중",
  delayMs,
}: {
  label?: string;
  delayMs?: number;
}) {
  const visible = useDelayedVisible(delayMs);

  return (
    <div
      role="status"
      aria-label={label}
      aria-busy
      className={cn(
        "flex items-center justify-center gap-2 p-8 text-body text-fg-caption",
        !visible && "invisible",
      )}
    >
      <span className="size-4 animate-spin rounded-full border-2 border-border-strong border-t-fg-body" />
      {label}…
    </div>
  );
}

/**
 * 재조회(이미 데이터가 있는 상태) 표시용 미세 스피너.
 * 화면 전체를 로딩으로 덮지 않고, 영역 우상단에 이것만 띄운다 (02 §2.2).
 */
export function InlineSpinner({
  label = "갱신 중",
  delayMs,
}: {
  label?: string;
  delayMs?: number;
}) {
  const visible = useDelayedVisible(delayMs);
  if (!visible) return null;

  return (
    <span
      role="status"
      aria-label={label}
      className="inline-block size-3.5 animate-spin rounded-full border-2 border-border-strong border-t-fg-caption"
    />
  );
}

/**
 * 스켈레톤 — 목록·카드 최초 로드의 기본 패턴.
 * 실제 레이아웃과 같은 형상으로 배치한다 (그리드는 행, 카드는 카드).
 */
export function Skeleton({ className }: { className?: string }) {
  return (
    <span
      aria-hidden
      className={cn(
        // 1.5s 셔머 (02 §2.2) — Tailwind 기본 pulse는 2s라 주기를 맞춘다
        "block animate-pulse rounded-sm bg-[var(--core-color-neutral-100)] [animation-duration:1.5s]",
        className,
      )}
    />
  );
}
