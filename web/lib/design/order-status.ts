/**
 * 발주 상태 7종의 표시 라벨 (01-foundations §1.4 / tokens.json semantic.color.orderStatus).
 *
 * 라벨은 토큰의 `label` 값을 그대로 쓴다 — 화면마다 다른 표현("승인완료" 등) 금지(02 §1).
 * 색은 tokens.css의 `--color-status-<상태>-{dot,text,bg}` 변수로 이미 존재하지만,
 * Tailwind는 클래스명을 정적으로 스캔하므로 완전한 클래스 문자열을 여기에 나열한다
 * (`bg-status-${x}-bg` 같은 동적 조합은 빌드 시 유틸리티가 생성되지 않는다).
 */
export const ORDER_STATUSES = [
  "SUBMITTED",
  "PENDING_APPROVAL",
  "APPROVED",
  "REJECTED",
  "SHIPPED",
  "RECEIVED",
  "CANCELED",
] as const;

export type OrderStatus = (typeof ORDER_STATUSES)[number];

interface OrderStatusStyle {
  label: string;
  /** 뱃지 배경 + 라벨 색 */
  chip: string;
  /** badge-md의 dot 색 */
  dot: string;
}

export const ORDER_STATUS_STYLE: Record<OrderStatus, OrderStatusStyle> = {
  SUBMITTED: {
    label: "제출됨",
    chip: "bg-status-submitted-bg text-status-submitted-text",
    dot: "bg-status-submitted-dot",
  },
  PENDING_APPROVAL: {
    label: "승인 대기",
    chip: "bg-status-pending-approval-bg text-status-pending-approval-text",
    dot: "bg-status-pending-approval-dot",
  },
  APPROVED: {
    label: "승인됨",
    chip: "bg-status-approved-bg text-status-approved-text",
    dot: "bg-status-approved-dot",
  },
  REJECTED: {
    label: "거절됨",
    chip: "bg-status-rejected-bg text-status-rejected-text",
    dot: "bg-status-rejected-dot",
  },
  SHIPPED: {
    label: "출하됨",
    chip: "bg-status-shipped-bg text-status-shipped-text",
    dot: "bg-status-shipped-dot",
  },
  RECEIVED: {
    label: "입고 완료",
    chip: "bg-status-received-bg text-status-received-text",
    dot: "bg-status-received-dot",
  },
  CANCELED: {
    label: "취소됨",
    chip: "bg-status-canceled-bg text-status-canceled-text",
    dot: "bg-status-canceled-dot",
  },
};
