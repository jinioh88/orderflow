import type { LucideIcon } from "lucide-react";
import { Button } from "./button";

/**
 * 빈 상태 (02-patterns §2.1): 아이콘(도메인 개념, neutral-300, 32) + 제목 1줄 +
 * 설명 1줄 + 액션. "필터 결과 없음"은 "데이터 없음"과 다르게 표현한다 — 필터를
 * 의심하게 만들어야 하므로 액션은 필터 초기화다.
 * core 토큰(neutral-300) 참조는 디자인 시스템 구현 레이어인 여기로 한정한다 (01 §0).
 */
export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
}: {
  icon: LucideIcon;
  title: string;
  description: string;
  action?: { label: string; onClick: () => void; primary?: boolean };
}) {
  return (
    <div className="flex flex-col items-center gap-2 px-6 py-12 text-center">
      <span className="text-[var(--core-color-neutral-300)]" aria-hidden>
        <Icon size={32} strokeWidth={1.5} />
      </span>
      <p className="text-heading text-fg-title">{title}</p>
      <p className="text-body-md text-fg-caption">{description}</p>
      {action && (
        <Button
          variant={action.primary ? "primary" : "secondary"}
          size="lg"
          className="mt-2"
          onClick={action.onClick}
        >
          {action.label}
        </Button>
      )}
    </div>
  );
}
