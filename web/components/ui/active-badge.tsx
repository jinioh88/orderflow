import { cn } from "@/lib/utils/cn";

/**
 * 활성/비활성 뱃지 — 가맹점·계정 상태(ACTIVE/INACTIVE)용.
 * 발주 상태 7종 전용인 `StatusBadge`(02 §1)와 달리 디자인 시스템에 스펙이 없어
 * badge-sm 형태 규칙(높이 20, 필 배경)에 success/중립 토큰을 얹었다 — 발견 항목에 기록.
 */
export function ActiveBadge({
  active,
  activeLabel = "활성",
  inactiveLabel = "비활성",
  className,
}: {
  active: boolean;
  activeLabel?: string;
  inactiveLabel?: string;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex h-5 shrink-0 items-center whitespace-nowrap rounded-full px-2 text-caption font-medium",
        active
          ? "bg-success-bg text-success-text"
          : "bg-[var(--core-color-neutral-100)] text-fg-caption",
        className,
      )}
    >
      {active ? activeLabel : inactiveLabel}
    </span>
  );
}
