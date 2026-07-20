import { AppHeader } from "@/components/layout/app-header";
import { SidebarNav } from "@/components/layout/sidebar-nav";

/**
 * 앱 셸 (03-web-components §1): 사이드바 w=240 · 헤더 h=56 · 콘텐츠 좌우 패딩 24.
 * 인증 가드(미인증 시 /login 리다이렉트)는 US-AUTH-03에서 이 레이아웃에 걸린다.
 */
export default function AdminLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <div className="flex min-h-screen">
      <aside className="flex w-sidebar shrink-0 flex-col border-r border-border bg-surface">
        {/* 상단 로고 영역 h=56 — 테넌트명은 US-AUTH-03에서 로그인 정보로 채운다 */}
        <div className="flex h-header shrink-0 items-center border-b border-border px-4 text-heading text-fg-title">
          OrderFlow
        </div>
        <SidebarNav />
        {/* 하단 사용자 이름·역할 + 로그아웃 자리 (US-AUTH-03) */}
        <div className="border-t border-border px-4 py-3 text-caption text-fg-caption">
          로그인 정보
        </div>
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <AppHeader />
        <main className="flex-1 p-6">{children}</main>
      </div>
    </div>
  );
}
