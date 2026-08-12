"use client";

import { useState } from "react";
import { Plus, Store as StoreIcon } from "lucide-react";
import { ActiveBadge } from "@/components/ui/active-badge";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { ErrorMessage } from "@/components/ui/error-message";
import { Pagination } from "@/components/ui/pagination";
import { InlineSpinner, Skeleton } from "@/components/ui/spinner";
import { useAuth } from "@/features/auth/auth-context";
import { cn } from "@/lib/utils/cn";
import { useStores } from "../hooks/use-stores";
import { useDeactivateStore } from "../hooks/use-store-mutations";
import type { Store, StoreStatus } from "../types";
import { StoreFormModal } from "./store-form-modal";

/** 가맹점 목록 화면 (US-AUTH-02). 등록·비활성화는 HQ_ADMIN 전용 (스펙 2.4.6·2.4.8) */

const PAGE_SIZE = 20;

const STATUS_FILTERS: { value: StoreStatus | undefined; label: string }[] = [
  { value: undefined, label: "전체" },
  { value: "ACTIVE", label: "운영중" },
  { value: "INACTIVE", label: "비활성" },
];

export function StoresView() {
  const { role } = useAuth();
  const isAdmin = role === "HQ_ADMIN";

  const [status, setStatus] = useState<StoreStatus | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const [deactivateTarget, setDeactivateTarget] = useState<Store | null>(null);

  const stores = useStores({ status, page, size: PAGE_SIZE });
  const deactivate = useDeactivateStore();

  const changeStatus = (next: StoreStatus | undefined) => {
    setStatus(next);
    setPage(0); // 필터가 바뀌면 첫 페이지부터
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1">
          {STATUS_FILTERS.map(({ value, label }) => (
            <Button
              key={label}
              variant="ghost"
              size="md"
              aria-pressed={status === value}
              onClick={() => changeStatus(value)}
              className={cn(
                status === value && "bg-primary-bg text-primary",
              )}
            >
              {label}
            </Button>
          ))}
          {stores.isFetching && stores.data && <InlineSpinner />}
        </div>
        {isAdmin && (
          <Button
            variant="primary"
            icon={<Plus size={16} strokeWidth={1.5} />}
            onClick={() => setFormOpen(true)}
          >
            가맹점 등록
          </Button>
        )}
      </div>

      {stores.isError ? (
        <ErrorMessage error={stores.error} onRetry={() => stores.refetch()} />
      ) : (
        <div className="overflow-hidden rounded-lg border border-border bg-surface">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border text-left text-caption text-fg-caption">
                <th className="px-4 py-2.5 font-medium">가맹점명</th>
                <th className="w-24 px-4 py-2.5 font-medium">상태</th>
                <th className="px-4 py-2.5 font-medium">주소</th>
                <th className="w-32 px-4 py-2.5 font-medium">등록일</th>
                {isAdmin && <th className="w-28 px-4 py-2.5" />}
              </tr>
            </thead>
            <tbody>
              {stores.isPending ? (
                <SkeletonRows columns={isAdmin ? 5 : 4} />
              ) : stores.data.items.length === 0 ? (
                <tr>
                  <td colSpan={isAdmin ? 5 : 4}>
                    <EmptyState
                      filtered={status !== undefined}
                      onResetFilter={() => changeStatus(undefined)}
                      onCreate={isAdmin ? () => setFormOpen(true) : undefined}
                    />
                  </td>
                </tr>
              ) : (
                stores.data.items.map((store) => (
                  <tr
                    key={store.id}
                    className="border-b border-border text-body-md text-fg-body last:border-b-0"
                  >
                    <td className="px-4 py-2.5 font-medium">{store.name}</td>
                    <td className="px-4 py-2.5">
                      <ActiveBadge
                        active={store.status === "ACTIVE"}
                        activeLabel="운영중"
                        inactiveLabel="비활성"
                      />
                    </td>
                    <td className="px-4 py-2.5 text-fg-caption">
                      {store.address ?? "—"}
                    </td>
                    {/* ISO-8601 오프셋 포함 값(1.1)에서 날짜만 표기 */}
                    <td className="px-4 py-2.5 text-num text-fg-caption">
                      {store.createdAt.slice(0, 10)}
                    </td>
                    {isAdmin && (
                      <td className="px-4 py-2.5 text-right">
                        {store.status === "ACTIVE" && (
                          <Button
                            variant="danger-ghost"
                            size="sm"
                            onClick={() => setDeactivateTarget(store)}
                          >
                            비활성화
                          </Button>
                        )}
                      </td>
                    )}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {stores.data && <Pagination page={stores.data.page} onPageChange={setPage} />}

      <StoreFormModal open={formOpen} onClose={() => setFormOpen(false)} />

      <ConfirmDialog
        open={deactivateTarget !== null}
        title="가맹점 비활성화"
        description={`'${deactivateTarget?.name}'을(를) 비활성화합니다. 소속 사용자의 로그인·토큰 재발급이 즉시 차단되며, 재활성화 기능은 제공되지 않습니다.`}
        confirmLabel="비활성화"
        danger
        loading={deactivate.isPending}
        onCancel={() => setDeactivateTarget(null)}
        onConfirm={() => {
          if (!deactivateTarget) return;
          deactivate.mutate(deactivateTarget.id, {
            onSettled: () => setDeactivateTarget(null),
          });
        }}
      />
    </div>
  );
}

/** 최초 로드 스켈레톤 — 실제 행과 같은 형상 (02 §2.2) */
function SkeletonRows({ columns }: { columns: number }) {
  return (
    <>
      {Array.from({ length: 5 }, (_, i) => (
        <tr key={i} className="border-b border-border last:border-b-0">
          {Array.from({ length: columns }, (_, j) => (
            <td key={j} className="px-4 py-3">
              <Skeleton className="h-4 w-full max-w-40" />
            </td>
          ))}
        </tr>
      ))}
    </>
  );
}

function EmptyState({
  filtered,
  onResetFilter,
  onCreate,
}: {
  filtered: boolean;
  onResetFilter: () => void;
  onCreate?: () => void;
}) {
  return (
    <div className="flex flex-col items-center gap-2 px-6 py-12 text-center">
      <span className="text-[var(--core-color-neutral-300)]" aria-hidden>
        <StoreIcon size={32} strokeWidth={1.5} />
      </span>
      <p className="text-heading text-fg-title">
        {filtered ? "조건에 맞는 가맹점이 없습니다" : "등록된 가맹점이 없습니다"}
      </p>
      <p className="text-body-md text-fg-caption">
        {filtered
          ? "상태 필터를 바꾸거나 초기화해 보세요."
          : "가맹점을 등록하면 점주 계정을 발급할 수 있습니다."}
      </p>
      {filtered ? (
        <Button variant="secondary" size="lg" className="mt-2" onClick={onResetFilter}>
          필터 초기화
        </Button>
      ) : (
        onCreate && (
          <Button variant="primary" size="lg" className="mt-2" onClick={onCreate}>
            가맹점 등록
          </Button>
        )
      )}
    </div>
  );
}
