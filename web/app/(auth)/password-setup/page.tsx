import type { Metadata } from "next";
import { PasswordSetupForm } from "@/features/auth/components/password-setup-form";
import { PasswordSetupGuard } from "@/features/auth/components/route-guards";

export const metadata: Metadata = { title: "비밀번호 설정" };

export default function PasswordSetupPage() {
  return (
    <PasswordSetupGuard>
      <main className="flex min-h-screen items-center justify-center">
        <PasswordSetupForm />
      </main>
    </PasswordSetupGuard>
  );
}
