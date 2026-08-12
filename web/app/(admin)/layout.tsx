import { AppHeader } from "@/components/layout/app-header";
import { SidebarNav } from "@/components/layout/sidebar-nav";
import { AuthGuard } from "@/features/auth/components/route-guards";
import { SidebarUser } from "@/features/auth/components/sidebar-user";

/**
 * 앱 셸 (03-web-components §1): 사이드바 w=240 · 헤더 h=56 · 콘텐츠 좌우 패딩 24.
 * AuthGuard가 미인증 접근을 로그인으로, 임시 비밀번호 상태를 설정 화면으로 보낸다 (US-AUTH-03).
 */
export default function AdminLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <AuthGuard>
      <div className="flex min-h-screen">
        <aside className="flex w-sidebar shrink-0 flex-col border-r border-border bg-surface">
          {/* 상단 로고 영역 h=56 — 테넌트명 표시는 스펙에 조회 수단이 없어 수정요청 검토 중 (발견 항목) */}
          <div className="flex h-header shrink-0 items-center border-b border-border px-4 text-heading text-fg-title">
            OrderFlow
          </div>
          <SidebarNav />
          <SidebarUser />
        </aside>
        <div className="flex min-w-0 flex-1 flex-col">
          <AppHeader />
          <main className="flex-1 p-6">{children}</main>
        </div>
      </div>
    </AuthGuard>
  );
}
