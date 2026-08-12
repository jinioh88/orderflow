"use client";

import { Copy } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Modal } from "@/components/ui/modal";
import { useToast } from "@/components/ui/toast";

/**
 * 임시 비밀번호 1회 표시 다이얼로그 (US-AUTH-02, PM 지시 2026-07-19).
 * 임시 비밀번호는 등록/재발급 응답에 1회만 평문 포함되고 재조회 불가(스펙 2.3) —
 * 복사 버튼과 "다시 조회할 수 없음" 안내를 반드시 함께 보여준다.
 */
export function TemporaryPasswordDialog({
  open,
  accountLabel,
  password,
  onClose,
}: {
  open: boolean;
  /** 대상 표기 — 예: "박점주 (owner@example.com)" */
  accountLabel: string;
  password: string;
  onClose: () => void;
}) {
  const toast = useToast();

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(password);
      toast.show({ variant: "success", message: "임시 비밀번호가 복사되었습니다." });
    } catch {
      toast.show({
        variant: "error",
        message: "복사하지 못했습니다. 화면의 값을 직접 선택해 복사해 주세요.",
      });
    }
  };

  return (
    <Modal
      open={open}
      title="임시 비밀번호 발급"
      size="sm"
      onClose={onClose}
      footer={
        <Button variant="primary" onClick={onClose}>
          확인
        </Button>
      }
    >
      <div className="flex flex-col gap-3">
        <p className="text-body-md text-fg-body">{accountLabel}</p>
        <div className="flex items-center gap-2 rounded-md border border-border bg-page-bg px-3 py-2">
          <code className="min-w-0 flex-1 select-all text-num-lg text-fg-title">
            {password}
          </code>
          <Button
            variant="ghost"
            size="sm"
            icon={<Copy size={16} strokeWidth={1.5} />}
            onClick={copy}
          >
            복사
          </Button>
        </div>
        <p className="rounded-md bg-warning-bg px-3 py-2 text-body-md text-warning-text">
          이 창을 닫으면 임시 비밀번호를 다시 조회할 수 없습니다. 분실 시에는
          재발급만 가능합니다.
        </p>
      </div>
    </Modal>
  );
}
