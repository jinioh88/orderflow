import { SidebarNav } from "@/components/layout/sidebar-nav";

// 인증 가드(미인증 시 /login 리다이렉트)는 US-AUTH-03에서 이 레이아웃에 걸린다
export default function AdminLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <div className="flex min-h-screen">
      <aside className="flex w-56 shrink-0 flex-col border-r border-gray-200 bg-white">
        <div className="flex h-14 items-center border-b border-gray-200 px-4 text-lg font-bold">
          OrderFlow
        </div>
        <SidebarNav />
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center justify-end border-b border-gray-200 bg-white px-6">
          {/* US-AUTH-03: 로그인 사용자 표시 + 로그아웃 버튼 자리 */}
          <span className="text-sm text-gray-400">본사 관리자</span>
        </header>
        <main className="flex-1 p-6">{children}</main>
      </div>
    </div>
  );
}
