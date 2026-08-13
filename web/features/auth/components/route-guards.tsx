"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { ShieldAlert } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";
import { homePathFor, navItemOf, visibleNavItemsFor } from "@/lib/design/nav";
import { useAuth } from "../auth-context";
import { useLogout } from "../hooks/use-logout";

/**
 * 라우트 가드 (US-AUTH-03). 인증 상태를 3개 구역으로 사상한다:
 * 미인증 → /login · 임시 비밀번호 상태 → /password-setup · 인증 완료 → (admin).
 * 현재 구역과 있어야 할 구역이 다르면 replace로 옮긴다. 복원(loading) 중에는 판단을
 * 보류한다 — 안 그러면 새로고침마다 로그인으로 튕긴다.
 */
type Area = "admin" | "login" | "password-setup";

const AREA_PATH: Record<Exclude<Area, "admin">, string> = {
  login: "/login",
  "password-setup": "/password-setup",
};

function useAreaRouting(area: Area): boolean {
  const { status, role, passwordSetupRequired } = useAuth();
  const router = useRouter();

  const desired: Area | null =
    status === "loading"
      ? null
      : status === "unauthenticated"
        ? "login"
        : passwordSetupRequired
          ? "password-setup"
          : "admin";
  const ready = desired === area;

  useEffect(() => {
    if (desired === null || ready) return;
    router.replace(
      desired === "admin" ? homePathFor(role) : AREA_PATH[desired],
    );
  }, [desired, ready, role, router]);

  return ready;
}

/** 복원·리다이렉트 중 화면 — 콘텐츠를 잠깐이라도 노출하지 않는다 */
function GuardPending() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <Spinner label="확인 중" />
    </div>
  );
}

/** `(admin)` 전용: 인증 완료 + 역할이 허용된 라우트만 통과 */
export function AuthGuard({ children }: { children: React.ReactNode }) {
  const ready = useAreaRouting("admin");
  const { role } = useAuth();
  const pathname = usePathname();
  const router = useRouter();

  // 볼 수 있는 메뉴가 하나도 없는 역할(점주 등이 웹에 로그인) — 리다이렉트할 홈이
  // 없으므로 홈으로 보내면 무한 루프다. 안내 화면 + 로그아웃만 제공한다.
  const noAccess = ready && visibleNavItemsFor(role).length === 0;

  // 역할 밖 메뉴로 직접 진입하면 홈으로 (메뉴 숨김만으로는 URL 입력을 못 막는다)
  const item = navItemOf(pathname);
  const roleAllowed = !item || (role !== null && item.roles.includes(role));

  useEffect(() => {
    if (ready && !noAccess && !roleAllowed) router.replace(homePathFor(role));
  }, [ready, noAccess, roleAllowed, role, router]);

  if (noAccess) return <NoAccessScreen />;
  if (!ready || !roleAllowed) return <GuardPending />;
  return children;
}

/** 웹 관리자 화면 권한이 없는 역할용 안내 (색 단독 표현 금지 — 아이콘+문구) */
function NoAccessScreen() {
  const logout = useLogout();

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-2 text-center">
      <span className="text-danger-solid" aria-hidden>
        <ShieldAlert size={32} strokeWidth={1.5} />
      </span>
      <p className="text-heading text-fg-title">
        이 계정으로는 관리자 화면을 사용할 수 없습니다
      </p>
      <p className="text-body-md text-fg-caption">
        본사 계정으로 다시 로그인해 주세요. 가맹점 발주는 모바일 앱에서 진행합니다.
      </p>
      <Button
        variant="secondary"
        size="lg"
        className="mt-2"
        loading={logout.isPending}
        onClick={() => logout.mutate()}
      >
        로그아웃
      </Button>
    </div>
  );
}

/** 로그인 화면 전용: 이미 로그인돼 있으면 상태에 맞는 구역으로 보낸다 */
export function GuestGuard({ children }: { children: React.ReactNode }) {
  const ready = useAreaRouting("login");
  if (!ready) return <GuardPending />;
  return children;
}

/** 비밀번호 설정 화면 전용: 임시 상태의 인증 세션만 허용 */
export function PasswordSetupGuard({
  children,
}: {
  children: React.ReactNode;
}) {
  const ready = useAreaRouting("password-setup");
  if (!ready) return <GuardPending />;
  return children;
}
