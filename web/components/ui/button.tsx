import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

export type ButtonVariant =
  | "primary"
  | "secondary"
  | "ghost"
  | "danger"
  | "danger-ghost";
export type ButtonSize = "sm" | "md" | "lg";

/**
 * 호버 배경(neutral-100)은 web 계층에 대응 토큰이 없어 core 변수를 직접 참조한다.
 * 화면 코드가 아니라 디자인 시스템 구현 레이어이므로 여기서만 허용하고,
 * 토큰 보완 필요는 백로그 "발견 항목"에 기록했다.
 */
const HOVER_NEUTRAL = "hover:bg-[var(--core-color-neutral-100)]";
const DISABLED_NEUTRAL = "disabled:bg-[var(--core-color-neutral-100)]";

// 03 §2 변형 표. disabled는 opacity 트릭 금지 — 대비가 예측 불가해진다.
const VARIANT: Record<ButtonVariant, string> = {
  primary: cn(
    "bg-primary text-white hover:bg-primary-hover",
    DISABLED_NEUTRAL,
    "disabled:text-fg-disabled",
  ),
  secondary: cn(
    "border border-border-strong bg-surface text-fg-body",
    HOVER_NEUTRAL,
    "disabled:border-border",
    DISABLED_NEUTRAL,
    "disabled:text-fg-disabled",
  ),
  ghost: cn(
    "bg-transparent text-fg-body",
    HOVER_NEUTRAL,
    "disabled:bg-transparent disabled:text-fg-disabled",
  ),
  danger: cn(
    "bg-danger-solid text-white hover:bg-danger-text",
    DISABLED_NEUTRAL,
    "disabled:text-fg-disabled",
  ),
  "danger-ghost": cn(
    "bg-transparent text-danger-text hover:bg-danger-bg",
    "disabled:bg-transparent disabled:text-fg-disabled",
  ),
};

// 03 §2 크기: md h=32 기본 / sm h=28 그리드 인라인 / lg h=36 빈 상태 CTA·로그인
const SIZE: Record<ButtonSize, string> = {
  sm: "h-7 gap-1.5 px-2",
  md: "h-8 gap-1.5 px-3",
  lg: "h-9 gap-1.5 px-3",
};

const ICON_ONLY_SIZE: Record<ButtonSize, string> = {
  sm: "size-7 p-0",
  md: "size-8 p-0",
  lg: "size-9 p-0",
};

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  /** 진행 중 표시. 버튼은 disabled가 되고 폭은 변하지 않는다 (03 §2, 02 §2.2) */
  loading?: boolean;
  /** 라벨 좌측 아이콘 — lucide 아이콘을 size 16으로 넘긴다 */
  icon?: ReactNode;
  /** 아이콘만 있는 32×32 버튼. 이 경우 aria-label이 필수다 */
  iconOnly?: boolean;
}

export function Button({
  variant = "secondary",
  size = "md",
  loading = false,
  icon,
  iconOnly = false,
  disabled,
  className,
  children,
  type = "button",
  ...rest
}: ButtonProps) {
  return (
    <button
      type={type}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={cn(
        "relative inline-flex shrink-0 items-center justify-center rounded-md text-body-strong",
        "transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary",
        "disabled:cursor-not-allowed",
        iconOnly ? ICON_ONLY_SIZE[size] : SIZE[size],
        VARIANT[variant],
        className,
      )}
      {...rest}
    >
      {/* 로딩 중에도 라벨 DOM을 그대로 두어 버튼 폭을 고정하고(03 §2 "폭 고정"),
          스피너를 그 위에 겹친다. 라벨을 "저장 중…" 등으로 바꾸지 않는다. */}
      <span
        className={cn(
          "inline-flex items-center gap-1.5",
          loading && "invisible",
        )}
      >
        {icon}
        {children}
      </span>
      {loading && (
        <span className="absolute inset-0 flex items-center justify-center">
          <ButtonSpinner />
        </span>
      )}
    </button>
  );
}

/** 버튼 내 스피너 16px — 현재 텍스트 색을 따라간다 (02 §2.2) */
function ButtonSpinner() {
  return (
    <span
      className="size-4 animate-spin rounded-full border-2 border-current border-t-transparent opacity-70"
      aria-hidden
    />
  );
}
