import type { Metadata } from "next";
import { LoginForm } from "@/features/auth/components/login-form";
import { GuestGuard } from "@/features/auth/components/route-guards";

export const metadata: Metadata = { title: "로그인" };

export default function LoginPage() {
  return (
    <GuestGuard>
      <main className="flex min-h-screen items-center justify-center">
        <LoginForm />
      </main>
    </GuestGuard>
  );
}
