"use client";

import { LogOut } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "../auth-context";
import { useLogout } from "../hooks/use-logout";
import { ROLE_LABELS } from "../types";

/** 사이드바 하단 사용자 이름·역할 + 로그아웃 (03 §1) */
export function SidebarUser() {
  const { user, role } = useAuth();
  const logout = useLogout();

  if (!user) return null;

  return (
    <div className="flex items-center gap-2 border-t border-border px-4 py-3">
      <div className="min-w-0 flex-1">
        <p className="truncate text-body-strong text-fg-body">{user.name}</p>
        <p className="truncate text-caption text-fg-caption">
          {role ? ROLE_LABELS[role] : ""}
        </p>
      </div>
      <Button
        variant="ghost"
        size="md"
        iconOnly
        aria-label="로그아웃"
        title="로그아웃"
        icon={<LogOut size={16} strokeWidth={1.5} />}
        loading={logout.isPending}
        onClick={() => logout.mutate()}
      />
    </div>
  );
}
