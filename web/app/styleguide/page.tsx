"use client";

import { useState } from "react";
import { Download, Search, Upload } from "lucide-react";
import { Button, type ButtonVariant } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { ErrorMessage, InlineErrorMessage } from "@/components/ui/error-message";
import { Input } from "@/components/ui/input";
import { InlineSpinner, Skeleton, Spinner } from "@/components/ui/spinner";
import { StatusBadge } from "@/components/ui/status-badge";
import { useToast, type ToastVariant } from "@/components/ui/toast";
import { ORDER_STATUSES } from "@/lib/design/order-status";
import { ApiError } from "@/lib/api/types";

/**
 * 내부 스타일가이드 — design/design-system/styleguide-web.html과 시각 대조하는 화면.
 * 사이드바 메뉴에 노출하지 않는 개발 참고용 페이지다(라우트 직접 접근).
 */
const VARIANTS: ButtonVariant[] = [
  "primary",
  "secondary",
  "ghost",
  "danger",
  "danger-ghost",
];

export default function StyleguidePage() {
  const { show } = useToast();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [dangerDialogOpen, setDangerDialogOpen] = useState(false);
  const [loadingDemo, setLoadingDemo] = useState(false);

  return (
    <main className="mx-auto flex max-w-5xl flex-col gap-10 p-10">
      <header>
        <h1 className="text-display text-fg-title">웹 디자인 시스템 스타일가이드</h1>
        <p className="mt-1 text-body-md text-fg-caption">
          design/design-system/styleguide-web.html과 대조하기 위한 구현 확인 화면입니다.
        </p>
      </header>

      <Section title="1. 타이포그래피 (web 스케일)">
        <div className="flex flex-col gap-1">
          <p className="text-display text-fg-title">display 20/28 600 — 페이지 타이틀</p>
          <p className="text-heading text-fg-title">heading 16/24 600 — 카드·모달 제목</p>
          <p className="text-body-md text-fg-body">body-md 14/20 400 — 폼·서술형 본문</p>
          <p className="text-body text-fg-body">body 13/20 400 — 기본값(그리드 셀·목록)</p>
          <p className="text-body-strong text-fg-body">body-strong 13/20 600 — 강조 셀·버튼</p>
          <p className="text-caption text-fg-caption">caption 12/16 400 — 그리드 헤더·타임스탬프</p>
          <p className="text-num text-fg-body">num 13/20 tabular — 1,240,000 / 1,111,111</p>
          <p className="text-num-lg text-fg-title">num-lg 24/32 700 tabular — ₩1,240,000</p>
        </div>
      </Section>

      <Section title="2. 버튼 (5변형 × 3크기)">
        <div className="flex flex-col gap-3">
          {VARIANTS.map((variant) => (
            <div key={variant} className="flex items-center gap-3">
              <span className="w-28 shrink-0 text-caption text-fg-caption">
                {variant}
              </span>
              <Button variant={variant} size="sm">
                sm 28
              </Button>
              <Button variant={variant} size="md">
                md 32
              </Button>
              <Button variant={variant} size="lg">
                lg 36
              </Button>
              <Button variant={variant} icon={<Download size={16} strokeWidth={1.5} />}>
                아이콘+라벨
              </Button>
              <Button variant={variant} iconOnly aria-label="업로드">
                <Upload size={16} strokeWidth={1.5} />
              </Button>
              <Button variant={variant} disabled>
                disabled
              </Button>
            </div>
          ))}
          <div className="flex items-center gap-3">
            <span className="w-28 shrink-0 text-caption text-fg-caption">loading</span>
            <Button
              variant="primary"
              loading={loadingDemo}
              onClick={() => {
                setLoadingDemo(true);
                setTimeout(() => setLoadingDemo(false), 2000);
              }}
            >
              저장
            </Button>
            <span className="text-caption text-fg-caption">
              클릭 → 2초간 스피너. 라벨을 바꾸지 않고 폭도 고정된다.
            </span>
          </div>
        </div>
      </Section>

      <Section title="3. 인풋 · 셀렉트">
        <div className="grid max-w-2xl grid-cols-2 gap-4">
          <Input label="상품명" placeholder="상품명을 입력하세요" />
          <Input label="품목코드" hint="(선택)" placeholder="A-101" />
          <Input label="단가" unit="원" defaultValue="12000" />
          <Input label="발주 단위" unit="박스" defaultValue="3" />
          <Input
            label="품목코드"
            defaultValue="A-101"
            error="품목코드 'A-101'이 이미 존재합니다. 다른 코드를 입력하세요"
          />
          <Input
            label="검색"
            icon={<Search size={16} strokeWidth={1.5} />}
            placeholder="상품명·품목코드"
          />
          <Input label="비활성" placeholder="수정 불가" disabled />
        </div>
      </Section>

      <Section title="4. 상태 뱃지 (7상태 × sm/md — 단일 컴포넌트)">
        <div className="flex flex-col gap-3">
          <div className="flex flex-wrap items-center gap-2">
            {ORDER_STATUSES.map((status) => (
              <StatusBadge key={status} status={status} size="sm" />
            ))}
            <span className="text-caption text-fg-caption">sm — 그리드 셀</span>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            {ORDER_STATUSES.map((status) => (
              <StatusBadge key={status} status={status} size="md" />
            ))}
            <span className="text-caption text-fg-caption">md — 상세·모달 (dot 포함)</span>
          </div>
        </div>
      </Section>

      <Section title="5. 토스트 (우하단 · 최대 3개)">
        <div className="flex gap-2">
          {(["success", "error", "warning", "info"] as ToastVariant[]).map((variant) => (
            <Button
              key={variant}
              variant="secondary"
              onClick={() =>
                show({
                  variant,
                  message: TOAST_SAMPLE[variant],
                  action:
                    variant === "error"
                      ? { label: "다시 시도", onClick: () => {} }
                      : undefined,
                })
              }
            >
              {variant}
            </Button>
          ))}
        </div>
      </Section>

      <Section title="6. 확인 다이얼로그">
        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => setDialogOpen(true)}>
            확인 등급
          </Button>
          <Button variant="danger" onClick={() => setDangerDialogOpen(true)}>
            파괴 등급
          </Button>
        </div>
        <ConfirmDialog
          open={dialogOpen}
          title="출하 확정"
          description="발주 12건이 SHIPPED로 전이됩니다. 전이 후에는 되돌릴 수 없습니다."
          confirmLabel="출하 확정"
          onConfirm={() => {
            setDialogOpen(false);
            show({ variant: "success", message: "발주 12건을 출하 확정했습니다." });
          }}
          onCancel={() => setDialogOpen(false)}
        />
        <ConfirmDialog
          open={dangerDialogOpen}
          title="계정 비활성화"
          description="강남1호점 점주 계정이 비활성화되어 로그인할 수 없게 됩니다."
          confirmLabel="비활성화"
          danger
          onConfirm={() => {
            setDangerDialogOpen(false);
            show({ variant: "success", message: "계정을 비활성화했습니다." });
          }}
          onCancel={() => setDangerDialogOpen(false)}
        />
      </Section>

      <Section title="7. 로딩 · 에러">
        <div className="flex flex-col gap-4">
          <div className="flex items-center gap-4 rounded-lg border border-border bg-surface p-4">
            <Spinner />
            <div className="flex items-center gap-2 text-caption text-fg-caption">
              재조회 표시 <InlineSpinner />
            </div>
          </div>
          <div className="flex flex-col gap-2 rounded-lg border border-border bg-surface p-4">
            <span className="text-caption text-fg-caption">스켈레톤(목록 최초 로드)</span>
            {[0, 1, 2].map((row) => (
              <div key={row} className="flex gap-3">
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-4 flex-1" />
                <Skeleton className="h-4 w-16" />
              </div>
            ))}
          </div>
          <ErrorMessage
            error={SAMPLE_ERROR}
            onRetry={() => show({ variant: "info", message: "다시 시도했습니다." })}
          />
          <InlineErrorMessage error={SAMPLE_ERROR} onRetry={() => {}} />
        </div>
      </Section>
    </main>
  );
}

const TOAST_SAMPLE: Record<ToastVariant, string> = {
  success: "상품 12건이 등록되었습니다.",
  error: "승인 실패 — 이미 김대리 님이 거절한 건입니다.",
  warning: "마감까지 29분 남았습니다.",
  info: "익영업일 마감분으로 접수됩니다.",
};

const SAMPLE_ERROR = new ApiError(500, "INTERNAL_ERROR", "");

function Section({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <section className="flex flex-col gap-3">
      <h2 className="text-heading text-fg-title">{title}</h2>
      {children}
    </section>
  );
}
