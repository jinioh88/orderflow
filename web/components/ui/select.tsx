"use client";

import type { ReactNode, SelectHTMLAttributes } from "react";
import { useId } from "react";
import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils/cn";

/**
 * 셀렉트 (03 §3) — 인풋과 동일 몸체(h=32, borderStrong) + 우측 chevron-down 16.
 * 드롭다운 커스텀(옵션 h=32·선택 표시 체크)은 네이티브 <select>라 미적용 — M1에서는
 * 네이티브로 가고, 편차는 백로그 발견 항목에 기록했다.
 */
interface SelectProps
  extends Omit<SelectHTMLAttributes<HTMLSelectElement>, "size"> {
  label?: string;
  /** 라벨 우측 보조 표기 — 예: "(선택)" (02 §3) */
  hint?: string;
  error?: string;
  children: ReactNode;
  className?: string;
}

export function Select({
  label,
  hint,
  error,
  className,
  id,
  children,
  ...rest
}: SelectProps) {
  const generatedId = useId();
  const selectId = id ?? generatedId;
  const errorId = `${selectId}-error`;
  const invalid = Boolean(error);

  return (
    <div className={cn("flex flex-col gap-1", className)}>
      {label && (
        <label
          htmlFor={selectId}
          className="text-body-md font-medium text-fg-body"
        >
          {label}
          {hint && <span className="ml-1 text-fg-caption">{hint}</span>}
        </label>
      )}
      <div
        className={cn(
          "relative flex h-8 items-center rounded-sm border bg-surface",
          "transition-[border-color,box-shadow]",
          invalid
            ? "border-danger-solid focus-within:ring-[3px] focus-within:ring-danger-solid/15"
            : "border-border-strong focus-within:border-primary focus-within:ring-[3px] focus-within:ring-primary/15",
          rest.disabled && "bg-[var(--core-color-neutral-100)]",
        )}
      >
        <select
          id={selectId}
          aria-invalid={invalid || undefined}
          aria-describedby={invalid ? errorId : undefined}
          className={cn(
            "h-full w-full appearance-none bg-transparent pl-3 pr-8 text-body-md text-fg-body outline-none",
            "disabled:text-fg-disabled",
          )}
          {...rest}
        >
          {children}
        </select>
        <span className="pointer-events-none absolute right-2 text-fg-caption">
          <ChevronDown size={16} strokeWidth={1.5} aria-hidden />
        </span>
      </div>
      {error && (
        <p id={errorId} className="text-caption text-danger-text">
          {error}
        </p>
      )}
    </div>
  );
}
