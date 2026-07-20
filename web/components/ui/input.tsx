import type { InputHTMLAttributes, ReactNode } from "react";
import { useId } from "react";
import { cn } from "@/lib/utils/cn";

interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "size"> {
  /** 인풋 위에 붙는 라벨. 플로팅 라벨은 쓰지 않는다 (02 §3) */
  label?: string;
  /** 검증 실패 메시지. "원인 + 해결" 톤으로 쓴다 (02 §3.1) */
  error?: string;
  /** 라벨 우측 보조 표기 — 필수 항목이 과반이면 선택 항목에 "(선택)"을 붙인다 (02 §3) */
  hint?: string;
  /** 인풋 내부 우측 고정 단위 (`원`, `박스`). 붙으면 값은 우측 정렬 + tabular (03 §3) */
  unit?: string;
  /** 좌측 인라인 아이콘 (검색 인풋 등) — lucide 16 */
  icon?: ReactNode;
  className?: string;
}

export function Input({
  label,
  error,
  hint,
  unit,
  icon,
  className,
  id,
  ...rest
}: InputProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const errorId = `${inputId}-error`;
  const invalid = Boolean(error);

  return (
    <div className={cn("flex flex-col gap-1", className)}>
      {label && (
        <label
          htmlFor={inputId}
          className="text-body-md font-medium text-fg-body"
        >
          {label}
          {hint && <span className="ml-1 text-fg-caption">{hint}</span>}
        </label>
      )}
      {/* 아이콘·단위를 인풋의 형제로 두고 보더/포커스 링은 래퍼가 갖는다.
          absolute + padding 방식은 단위 글자 수("원" vs "박스")에 따라 값과 겹친다. */}
      <div
        className={cn(
          "flex h-8 items-center gap-1.5 rounded-sm border bg-surface px-3",
          "transition-[border-color,box-shadow]",
          // 포커스 링 3px 15% — 에러 상태에서는 같은 두께의 danger 링 (03 §3)
          invalid
            ? "border-danger-solid focus-within:ring-[3px] focus-within:ring-danger-solid/15"
            : "border-border-strong focus-within:border-primary focus-within:ring-[3px] focus-within:ring-primary/15",
          rest.disabled && "bg-[var(--core-color-neutral-100)]",
        )}
      >
        {icon && (
          <span className="pointer-events-none shrink-0 text-fg-caption">
            {icon}
          </span>
        )}
        <input
          id={inputId}
          aria-invalid={invalid || undefined}
          aria-describedby={invalid ? errorId : undefined}
          className={cn(
            "min-w-0 flex-1 bg-transparent text-body-md text-fg-body outline-none",
            "placeholder:text-fg-disabled disabled:text-fg-disabled",
            // 단위가 붙는 값(수량·금액·한도)은 우측 정렬 + tabular (03 §3)
            unit && "text-right tabular-nums",
          )}
          {...rest}
        />
        {unit && (
          <span className="pointer-events-none shrink-0 text-body-md text-fg-caption">
            {unit}
          </span>
        )}
      </div>
      {error && (
        <p id={errorId} className="text-caption text-danger-text">
          {error}
        </p>
      )}
    </div>
  );
}
