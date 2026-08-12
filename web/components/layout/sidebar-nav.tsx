"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/features/auth/auth-context";
import { cn } from "@/lib/utils/cn";
import { visibleNavItemsFor } from "@/lib/design/nav";

/**
 * 사이드바 메뉴 (03 §1).
 * 항목 h=36, 아이콘 20 + 라벨 body-strong.
 * 활성 = primaryBg 배경 + primary 텍스트/아이콘 + 좌측 3px primary 바.
 * 노출 목록은 로그인 역할 기준 (US-AUTH-03 — JWT 클레임 role).
 */
export function SidebarNav() {
  const pathname = usePathname();
  const { role } = useAuth();

  return (
    <nav className="flex flex-1 flex-col gap-1 p-3">
      {visibleNavItemsFor(role).map(({ href, label, icon: Icon }) => {
        const active = pathname === href || pathname.startsWith(`${href}/`);
        return (
          <Link
            key={href}
            href={href}
            aria-current={active ? "page" : undefined}
            className={cn(
              "relative flex h-9 items-center gap-2 rounded-md pl-3 pr-2 text-body-strong transition-colors",
              active
                ? "bg-primary-bg text-primary"
                : "text-fg-body hover:bg-[var(--core-color-neutral-100)]",
            )}
          >
            {active && (
              <span
                aria-hidden
                className="absolute inset-y-1 left-0 w-[3px] rounded-full bg-primary"
              />
            )}
            <Icon size={20} strokeWidth={1.5} aria-hidden />
            {label}
          </Link>
        );
      })}
    </nav>
  );
}
