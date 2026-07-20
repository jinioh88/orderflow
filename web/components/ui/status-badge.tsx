import { cn } from "@/lib/utils/cn";
import { ORDER_STATUS_STYLE, type OrderStatus } from "@/lib/design/order-status";

/**
 * 발주 상태 뱃지 (02-patterns §1).
 *
 * 7상태 × 2크기를 **이 컴포넌트 하나가** 커버한다 — 상태별 개별 구현 금지(03 §8).
 * 색 단독 표현은 색맹 대비를 위해 금지이므로 라벨을 뗄 수 있는 prop을 두지 않는다(01 §1.4).
 *
 * - sm: 웹 그리드 셀 — 높이 20, 패딩 2×8, caption(12)
 * - md: 웹 상세·모달 — 높이 24, 패딩 4×10, body(13) + dot 6px
 */
export function StatusBadge({
  status,
  size = "sm",
  className,
}: {
  status: OrderStatus;
  size?: "sm" | "md";
  className?: string;
}) {
  const style = ORDER_STATUS_STYLE[status];

  return (
    <span
      className={cn(
        // 상태 전이는 배경색 200ms 트랜지션만 — 움직임 효과 금지(02 §1.1)
        "inline-flex shrink-0 items-center whitespace-nowrap rounded-full font-medium transition-colors duration-200",
        size === "sm" ? "h-5 px-2 text-caption" : "h-6 gap-1.5 px-2.5 text-body",
        style.chip,
        className,
      )}
    >
      {size === "md" && (
        <span className={cn("size-1.5 rounded-full", style.dot)} aria-hidden />
      )}
      {style.label}
    </span>
  );
}
