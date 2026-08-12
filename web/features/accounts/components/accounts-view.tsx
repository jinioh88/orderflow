"use client";

import { useMemo, useState } from "react";
import { KeyRound, Plus, Users as UsersIcon } from "lucide-react";
import { ActiveBadge } from "@/components/ui/active-badge";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { ErrorMessage } from "@/components/ui/error-message";
import { Pagination } from "@/components/ui/pagination";
import { InlineSpinner, Skeleton } from "@/components/ui/spinner";
import { useAuth } from "@/features/auth/auth-context";
import { ROLE_LABELS } from "@/features/auth/types";
import { useStores } from "@/features/stores/hooks/use-stores";
import { cn } from "@/lib/utils/cn";
import { useAccounts } from "../hooks/use-accounts";
import {
  useDeactivateAccount,
  useReissueTemporaryPassword,
} from "../hooks/use-account-mutations";
import type { Account, AccountStatus } from "../types";
import { AccountFormModal } from "./account-form-modal";
import { TemporaryPasswordDialog } from "./temporary-password-dialog";

/**
 * 계정 관리 화면 (US-AUTH-02, 사용자 확정 2026-08-12 — 가맹점 화면과 분리된 별도 메뉴).
 * 등록·임시 비밀번호 재발급·비활성화는 HQ_ADMIN 전용 (스펙 2.4.9~2.4.12).
 */

const PAGE_SIZE = 20;

const STATUS_FILTERS: { value: AccountStatus | undefined; label: string }[] = [
  { value: undefined, label: "전체" },
  { value: "ACTIVE", label: "활성" },
  { value: "INACTIVE", label: "비활성" },
];

interface IssuedPassword {
  accountLabel: string;
  password: string;
}

export function AccountsView() {
  const { user, role } = useAuth();
  const isAdmin = role === "HQ_ADMIN";

  const [status, setStatus] = useState<AccountStatus | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const [issued, setIssued] = useState<IssuedPassword | null>(null);
  const [reissueTarget, setReissueTarget] = useState<Account | null>(null);
  const [deactivateTarget, setDeactivateTarget] = useState<Account | null>(null);

  const accounts = useAccounts({ status, page, size: PAGE_SIZE });
  // 소속 가맹점명 표기용 (2.4.11 items에는 storeId만 있다) — 상태 무관 전체 조회
  const stores = useStores({ size: 100 });
  const reissue = useReissueTemporaryPassword();
  const deactivate = useDeactivateAccount();

  const storeNames = useMemo(() => {
    const map = new Map<number, string>();
    stores.data?.items.forEach((store) => map.set(store.id, store.name));
    return map;
  }, [stores.data]);

  const changeStatus = (next: AccountStatus | undefined) => {
    setStatus(next);
    setPage(0);
  };

  const accountLabelOf = (account: Account) =>
    `${account.name} (${account.email})`;

  const columnCount = isAdmin ? 7 : 6;

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
              className={cn(status === value && "bg-primary-bg text-primary")}
            >
              {label}
            </Button>
          ))}
          {accounts.isFetching && accounts.data && <InlineSpinner />}
        </div>
        {isAdmin && (
          <Button
            variant="primary"
            icon={<Plus size={16} strokeWidth={1.5} />}
            onClick={() => setFormOpen(true)}
          >
            점주 계정 등록
          </Button>
        )}
      </div>

      {accounts.isError ? (
        <ErrorMessage error={accounts.error} onRetry={() => accounts.refetch()} />
      ) : (
        <div className="overflow-hidden rounded-lg border border-border bg-surface">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border text-left text-caption text-fg-caption">
                <th className="px-4 py-2.5 font-medium">이름</th>
                <th className="px-4 py-2.5 font-medium">이메일</th>
                <th className="w-28 px-4 py-2.5 font-medium">역할</th>
                <th className="px-4 py-2.5 font-medium">소속</th>
                <th className="w-36 px-4 py-2.5 font-medium">상태</th>
                <th className="w-32 px-4 py-2.5 font-medium">등록일</th>
                {isAdmin && <th className="w-52 px-4 py-2.5" />}
              </tr>
            </thead>
            <tbody>
              {accounts.isPending ? (
                <SkeletonRows columns={columnCount} />
              ) : accounts.data.items.length === 0 ? (
                <tr>
                  <td colSpan={columnCount}>
                    <EmptyState
                      filtered={status !== undefined}
                      onResetFilter={() => changeStatus(undefined)}
                      onCreate={isAdmin ? () => setFormOpen(true) : undefined}
                    />
                  </td>
                </tr>
              ) : (
                accounts.data.items.map((account) => {
                  const isSelf = account.id === user?.id;
                  return (
                    <tr
                      key={account.id}
                      className="border-b border-border text-body-md text-fg-body last:border-b-0"
                    >
                      <td className="px-4 py-2.5 font-medium">
                        {account.name}
                        {isSelf && (
                          <span className="ml-1.5 text-caption text-fg-caption">
                            (나)
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-2.5 text-fg-caption">
                        {account.email}
                      </td>
                      <td className="px-4 py-2.5">{ROLE_LABELS[account.role]}</td>
                      <td className="px-4 py-2.5 text-fg-caption">
                        {account.storeId === null
                          ? "본사"
                          : (storeNames.get(account.storeId) ??
                            `가맹점 #${account.storeId}`)}
                      </td>
                      <td className="px-4 py-2.5">
                        <span className="flex items-center gap-1.5">
                          <ActiveBadge active={account.status === "ACTIVE"} />
                          {account.passwordSetupRequired && (
                            <span className="whitespace-nowrap text-caption text-warning-text">
                              비밀번호 설정 대기
                            </span>
                          )}
                        </span>
                      </td>
                      <td className="px-4 py-2.5 text-num text-fg-caption">
                        {account.createdAt.slice(0, 10)}
                      </td>
                      {isAdmin && (
                        <td className="px-4 py-2.5 text-right">
                          {/* 자기 자신 대상 재발급·비활성화는 409(2.4.10·2.4.12) — 버튼 자체를 숨긴다 */}
                          {!isSelf && account.status === "ACTIVE" && (
                            <span className="inline-flex gap-1">
                              <Button
                                variant="ghost"
                                size="sm"
                                icon={<KeyRound size={16} strokeWidth={1.5} />}
                                onClick={() => setReissueTarget(account)}
                              >
                                비밀번호 재발급
                              </Button>
                              <Button
                                variant="danger-ghost"
                                size="sm"
                                onClick={() => setDeactivateTarget(account)}
                              >
                                비활성화
                              </Button>
                            </span>
                          )}
                        </td>
                      )}
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      )}

      {accounts.data && (
        <Pagination page={accounts.data.page} onPageChange={setPage} />
      )}

      <AccountFormModal
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onCreated={(account) =>
          setIssued({
            accountLabel: accountLabelOf(account),
            password: account.temporaryPassword,
          })
        }
      />

      <TemporaryPasswordDialog
        open={issued !== null}
        accountLabel={issued?.accountLabel ?? ""}
        password={issued?.password ?? ""}
        onClose={() => setIssued(null)}
      />

      <ConfirmDialog
        open={reissueTarget !== null}
        title="임시 비밀번호 재발급"
        description={`'${reissueTarget?.name}'의 비밀번호를 임시 비밀번호로 교체합니다. 기존 비밀번호와 로그인 세션이 즉시 무효화됩니다.`}
        confirmLabel="재발급"
        loading={reissue.isPending}
        onCancel={() => setReissueTarget(null)}
        onConfirm={() => {
          if (!reissueTarget) return;
          const target = reissueTarget;
          reissue.mutate(target.id, {
            onSuccess: (data) =>
              setIssued({
                accountLabel: accountLabelOf(target),
                password: data.temporaryPassword,
              }),
            onSettled: () => setReissueTarget(null),
          });
        }}
      />

      <ConfirmDialog
        open={deactivateTarget !== null}
        title="계정 비활성화"
        description={`'${deactivateTarget?.name}' 계정을 비활성화합니다. 로그인과 토큰 재발급이 즉시 차단되며, 재활성화 기능은 제공되지 않습니다.`}
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
        <UsersIcon size={32} strokeWidth={1.5} />
      </span>
      <p className="text-heading text-fg-title">
        {filtered ? "조건에 맞는 계정이 없습니다" : "등록된 계정이 없습니다"}
      </p>
      <p className="text-body-md text-fg-caption">
        {filtered
          ? "상태 필터를 바꾸거나 초기화해 보세요."
          : "가맹점을 먼저 등록한 뒤 점주 계정을 발급하세요."}
      </p>
      {filtered ? (
        <Button
          variant="secondary"
          size="lg"
          className="mt-2"
          onClick={onResetFilter}
        >
          필터 초기화
        </Button>
      ) : (
        onCreate && (
          <Button variant="primary" size="lg" className="mt-2" onClick={onCreate}>
            점주 계정 등록
          </Button>
        )
      )}
    </div>
  );
}
