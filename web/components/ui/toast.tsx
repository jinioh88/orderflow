"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import {
  AlertCircle,
  AlertTriangle,
  CheckCircle2,
  Info,
  X,
} from "lucide-react";
import { cn } from "@/lib/utils/cn";

/**
 * 토스트 (02-patterns §4.1 Web).
 * 우하단에 위로 쌓이고 최대 3개, surface 배경 + 좌측 4px 시맨틱 바.
 * 지속 시간은 성공 3s / 오류 5s.
 *
 * 주의: 토스트로 **중요 정보를 전달하지 않는다** — 사라져도 업무가 되는 보조 확인용이다.
 * 판단이 필요한 내용은 다이얼로그나 배너를 쓴다.
 */
export type ToastVariant = "success" | "error" | "warning" | "info";

export interface ToastOptions {
  variant?: ToastVariant;
  message: string;
  /** 선택 액션 1개까지 — 그 이상은 토스트가 아니라 다이얼로그로 다룰 내용이다 */
  action?: { label: string; onClick: () => void };
  /** 기본값: 오류 5000ms, 그 외 3000ms */
  durationMs?: number;
}

interface ToastItem extends ToastOptions {
  id: number;
  variant: ToastVariant;
}

const MAX_VISIBLE = 3;

const VARIANT_STYLE: Record<
  ToastVariant,
  { bar: string; icon: string; Icon: typeof Info }
> = {
  success: {
    bar: "bg-success-solid",
    icon: "text-success-solid",
    Icon: CheckCircle2,
  },
  error: { bar: "bg-danger-solid", icon: "text-danger-solid", Icon: AlertCircle },
  warning: {
    bar: "bg-warning-solid",
    icon: "text-warning-solid",
    Icon: AlertTriangle,
  },
  info: { bar: "bg-info-solid", icon: "text-info-solid", Icon: Info },
};

interface ToastContextValue {
  show: (options: ToastOptions) => void;
  dismiss: (id: number) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function useToast(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast는 ToastProvider 안에서만 쓸 수 있습니다.");
  }
  return context;
}

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const nextId = useRef(1);

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const show = useCallback((options: ToastOptions) => {
    const variant = options.variant ?? "info";
    const item: ToastItem = { ...options, variant, id: nextId.current++ };
    // 4개째가 오면 가장 오래된 것을 밀어낸다
    setToasts((current) => [...current, item].slice(-MAX_VISIBLE));
  }, []);

  const value = useMemo(() => ({ show, dismiss }), [show, dismiss]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div
        // aria-live 영역은 토스트가 없을 때도 DOM에 있어야 스크린리더가 추가를 인지한다
        role="region"
        aria-live="polite"
        aria-label="알림"
        className="pointer-events-none fixed bottom-6 right-6 z-50 flex flex-col gap-2"
      >
        {toasts.map((toast) => (
          <ToastCard key={toast.id} toast={toast} onDismiss={dismiss} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

function ToastCard({
  toast,
  onDismiss,
}: {
  toast: ToastItem;
  onDismiss: (id: number) => void;
}) {
  const { id, variant, message, action, durationMs } = toast;
  const style = VARIANT_STYLE[variant];

  useEffect(() => {
    const timeout = durationMs ?? (variant === "error" ? 5000 : 3000);
    const timer = setTimeout(() => onDismiss(id), timeout);
    return () => clearTimeout(timer);
  }, [id, variant, durationMs, onDismiss]);

  return (
    <div
      className={cn(
        "pointer-events-auto relative flex w-80 items-start gap-2 overflow-hidden rounded-md",
        "border border-border bg-surface py-2.5 pl-3 pr-2 shadow-2",
      )}
    >
      <span className={cn("absolute left-0 h-full w-1", style.bar)} aria-hidden />
      <span className={cn("mt-px shrink-0", style.icon)} aria-hidden>
        <style.Icon size={16} strokeWidth={1.5} />
      </span>
      <p className="min-w-0 flex-1 break-words text-body text-fg-body">
        {message}
      </p>
      {action && (
        <button
          type="button"
          onClick={() => {
            action.onClick();
            onDismiss(id);
          }}
          className="shrink-0 rounded-sm px-1.5 py-0.5 text-body-strong text-primary hover:bg-primary-bg"
        >
          {action.label}
        </button>
      )}
      <button
        type="button"
        onClick={() => onDismiss(id)}
        aria-label="알림 닫기"
        className="shrink-0 rounded-sm p-0.5 text-fg-caption hover:bg-[var(--core-color-neutral-100)]"
      >
        <X size={16} strokeWidth={1.5} />
      </button>
    </div>
  );
}
