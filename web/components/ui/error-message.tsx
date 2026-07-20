"use client";

import { AlertCircle } from "lucide-react";
import { userMessageOf } from "@/lib/api/error-messages";
import { cn } from "@/lib/utils/cn";
import { Button } from "./button";

/**
 * 화면 단위 실패 표시 (02-patterns §2.3).
 * 빈 상태와 같은 구성(아이콘 + 제목 + 설명 + 액션)에 danger 아이콘을 쓰고,
 * **"다시 시도" 버튼은 필수**다.
 */
export function ErrorMessage({
  error,
  onRetry,
  title = "불러오지 못했습니다",
  className,
}: {
  error: unknown;
  onRetry?: () => void;
  title?: string;
  className?: string;
}) {
  return (
    <div
      role="alert"
      className={cn(
        "flex flex-col items-center gap-2 rounded-lg border border-border bg-surface px-6 py-10 text-center",
        className,
      )}
    >
      <span className="text-danger-solid" aria-hidden>
        <AlertCircle size={32} strokeWidth={1.5} />
      </span>
      <p className="text-heading text-fg-title">{title}</p>
      <p className="text-body-md text-fg-caption">{userMessageOf(error)}</p>
      {onRetry && (
        <Button variant="secondary" size="lg" onClick={onRetry} className="mt-2">
          다시 시도
        </Button>
      )}
    </div>
  );
}

/**
 * 부분 실패(한 영역만) 표시 — 화면 전체를 죽이지 않는다 (02 §2.3).
 */
export function InlineErrorMessage({
  error,
  onRetry,
  className,
}: {
  error: unknown;
  onRetry?: () => void;
  className?: string;
}) {
  return (
    <div
      role="alert"
      className={cn(
        "flex items-center gap-2 rounded-md bg-danger-bg px-3 py-2 text-body text-danger-text",
        className,
      )}
    >
      <span aria-hidden className="shrink-0">
        <AlertCircle size={16} strokeWidth={1.5} />
      </span>
      <p className="min-w-0 flex-1">{userMessageOf(error)}</p>
      {onRetry && (
        <Button variant="danger-ghost" size="sm" onClick={onRetry}>
          다시 시도
        </Button>
      )}
    </div>
  );
}
