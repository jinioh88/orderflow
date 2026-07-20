import type { Metadata } from "next";

export const metadata: Metadata = { title: "로그인" };

// US-AUTH-03에서 features/auth의 로그인 폼으로 교체될 placeholder
export default function LoginPage() {
  return (
    <main className="flex min-h-screen items-center justify-center">
      <div className="w-100 rounded-lg border border-border bg-surface p-8 shadow-1">
        <h1 className="text-display text-fg-title">OrderFlow 관리자</h1>
        <p className="mt-2 text-body-md text-fg-caption">
          로그인 화면은 US-AUTH-03에서 구현됩니다.
        </p>
      </div>
    </main>
  );
}
