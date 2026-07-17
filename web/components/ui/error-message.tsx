"use client";

import { userMessageOf } from "@/lib/api/error-messages";

export function ErrorMessage({
  error,
  onRetry,
}: {
  error: unknown;
  onRetry?: () => void;
}) {
  return (
    <div
      role="alert"
      className="flex flex-col items-center gap-3 rounded-md border border-red-200 bg-red-50 p-6 text-sm text-red-700"
    >
      <p>{userMessageOf(error)}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="rounded-md border border-red-300 px-3 py-1.5 font-medium hover:bg-red-100"
        >
          다시 시도
        </button>
      )}
    </div>
  );
}
