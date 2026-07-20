"use client";

import { usePathname } from "next/navigation";
import { navLabelOf } from "@/lib/design/nav";

/**
 * 앱 헤더 (03 §1): h=56, surface + 하단 border, 좌측 페이지 타이틀(display), sticky.
 * 우측 알림 벨은 v1.1(NTF-03) — MVP에서는 자리만 비워 둔다.
 */
export function AppHeader() {
  const pathname = usePathname();
  const title = navLabelOf(pathname) ?? "";

  return (
    <header className="sticky top-0 z-10 flex h-header shrink-0 items-center justify-between border-b border-border bg-surface px-6">
      <h1 className="text-display text-fg-title">{title}</h1>
      {/* US-AUTH-03: 사용자 메뉴(이름·역할·로그아웃)가 여기에 들어온다 */}
      <div className="flex items-center gap-2 text-body text-fg-caption">
        본사 관리자
      </div>
    </header>
  );
}
