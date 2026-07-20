import type { Metadata } from "next";
import "./globals.css";
import { pretendard } from "./fonts";
import { Providers } from "./providers";
import { ToastProvider } from "@/components/ui/toast";

export const metadata: Metadata = {
  title: {
    default: "OrderFlow 관리자",
    template: "%s | OrderFlow 관리자",
  },
  description: "프랜차이즈 수발주 & 물류 관리 — 본사 관리자",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className={`${pretendard.variable} h-full antialiased`}>
      {/* 최소 뷰포트 1280 — 반응형 없이 가로 스크롤 허용 (03 §1) */}
      <body className="min-w-min-viewport min-h-full bg-page-bg font-sans text-body text-fg-body">
        <Providers>
          <ToastProvider>{children}</ToastProvider>
        </Providers>
      </body>
    </html>
  );
}
