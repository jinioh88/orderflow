"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const MENU_ITEMS = [
  { href: "/stores", label: "가맹점 관리" }, // US-AUTH-02
  { href: "/products", label: "상품 관리" }, // US-CAT-01~04
];

export function SidebarNav() {
  const pathname = usePathname();

  return (
    <nav className="flex flex-col gap-1 p-3">
      {MENU_ITEMS.map(({ href, label }) => {
        const active = pathname.startsWith(href);
        return (
          <Link
            key={href}
            href={href}
            className={`rounded-md px-3 py-2 text-sm font-medium ${
              active
                ? "bg-gray-900 text-white"
                : "text-gray-600 hover:bg-gray-100"
            }`}
          >
            {label}
          </Link>
        );
      })}
    </nav>
  );
}
