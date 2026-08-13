"use client";

import { Button } from "./button";
import { Modal } from "./modal";

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  /**
   * 결과 요약 — "발주 12건이 SHIPPED로 전이됩니다"처럼 무슨 일이 벌어지는지 적는다 (02 §4.4).
   */
  description?: string;
  /**
   * 확인 버튼 라벨. **동사로 쓴다** — "확인"이 아니라 "출하 확정" / "비활성화" (02 §4.4).
   */
  confirmLabel: string;
  /** 파괴적 액션(거절·취소·계정 비활성화)이면 확인 버튼이 danger 색이 된다 */
  danger?: boolean;
  /** 확인 처리 진행 중 — 버튼에 스피너가 뜨고 다이얼로그는 닫히지 않는다 */
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

/**
 * 확인 다이얼로그 (02-patterns §4.4) — `Modal`(03 §6.1) 위에 구성한 sm 변형.
 * 되돌릴 수 있는 액션에는 이걸 쓰지 말고 토스트 + 실행취소를 쓴다.
 */
export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  danger = false,
  loading = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <Modal
      open={open}
      title={title}
      size="sm"
      closeDisabled={loading}
      onClose={onCancel}
      footer={
        <>
          {/* 파괴적 동작 확인이므로 초기 포커스는 안전한 쪽(취소)에 둔다 */}
          <Button
            variant="secondary"
            data-autofocus
            disabled={loading}
            onClick={onCancel}
          >
            취소
          </Button>
          <Button
            variant={danger ? "danger" : "primary"}
            loading={loading}
            onClick={onConfirm}
          >
            {confirmLabel}
          </Button>
        </>
      }
    >
      {description && (
        <p className="text-body-md text-fg-body">{description}</p>
      )}
    </Modal>
  );
}
