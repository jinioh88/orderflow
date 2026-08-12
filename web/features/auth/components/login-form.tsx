"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { userMessageOf } from "@/lib/api/error-messages";
import { API_ERROR_CODES, ApiError } from "@/lib/api/types";
import { useLogin } from "../hooks/use-login";

/**
 * 로그인 폼 (US-AUTH-03, 02-patterns §3).
 * 검증은 블러 시 1차 + 제출 시 전체, 재입력 시작하면 즉시 해제. 서버 필드 오류
 * (VALIDATION_ERROR details)는 해당 필드 아래, 전역 오류는 폼 상단 배너에 붙인다.
 */

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const FIELD_IDS = { email: "login-email", password: "login-password" } as const;
type FieldName = keyof typeof FIELD_IDS;
type FieldErrors = Partial<Record<FieldName, string>>;

function validateField(name: FieldName, value: string): string | undefined {
  if (name === "email") {
    if (!value) return "이메일을 입력해 주세요.";
    if (!EMAIL_PATTERN.test(value))
      return "이메일 형식이 아닙니다 (예: name@company.com)";
  }
  if (name === "password" && !value) return "비밀번호를 입력해 주세요.";
  return undefined;
}

function focusField(errors: FieldErrors) {
  const first = (Object.keys(FIELD_IDS) as FieldName[]).find(
    (name) => errors[name],
  );
  if (first) document.getElementById(FIELD_IDS[first])?.focus();
}

export function LoginForm() {
  const login = useLogin();
  const [values, setValues] = useState({ email: "", password: "" });
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);

  const handleChange = (name: FieldName, value: string) => {
    setValues((current) => ({ ...current, [name]: value }));
    // 재입력이 시작되면 이전 판정은 즉시 치운다 (02 §3)
    setFieldErrors((current) => ({ ...current, [name]: undefined }));
    setFormError(null);
  };

  const handleBlur = (name: FieldName) => {
    const message = validateField(name, values[name]);
    if (message) setFieldErrors((current) => ({ ...current, [name]: message }));
  };

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    const errors: FieldErrors = {
      email: validateField("email", values.email),
      password: validateField("password", values.password),
    };
    if (errors.email || errors.password) {
      setFieldErrors(errors);
      focusField(errors);
      return;
    }
    login.mutate(values, {
      onError: (error) => {
        if (
          error instanceof ApiError &&
          error.code === API_ERROR_CODES.VALIDATION_ERROR
        ) {
          const serverErrors = error.fieldErrors();
          const mapped: FieldErrors = {
            email: serverErrors.email,
            password: serverErrors.password,
          };
          if (mapped.email || mapped.password) {
            setFieldErrors(mapped);
            focusField(mapped);
            return;
          }
        }
        // INVALID_CREDENTIALS·ACCOUNT_INACTIVE 등 — 코드별 문구는 messages.ts 매핑
        setFormError(userMessageOf(error));
      },
    });
  };

  return (
    <div className="w-100 rounded-lg border border-border bg-surface p-8 shadow-1">
      <h1 className="text-display text-fg-title">OrderFlow 관리자</h1>
      <p className="mt-1 text-body-md text-fg-caption">
        본사 계정으로 로그인하세요.
      </p>
      <form onSubmit={handleSubmit} noValidate className="mt-6 flex flex-col gap-4">
        {formError && (
          <div
            role="alert"
            className="rounded-md bg-danger-bg px-3 py-2 text-body-md text-danger-text"
          >
            {formError}
          </div>
        )}
        <Input
          id={FIELD_IDS.email}
          label="이메일"
          type="email"
          autoComplete="email"
          placeholder="name@company.com"
          value={values.email}
          error={fieldErrors.email}
          onChange={(e) => handleChange("email", e.target.value)}
          onBlur={() => handleBlur("email")}
        />
        <Input
          id={FIELD_IDS.password}
          label="비밀번호"
          type="password"
          autoComplete="current-password"
          value={values.password}
          error={fieldErrors.password}
          onChange={(e) => handleChange("password", e.target.value)}
          onBlur={() => handleBlur("password")}
        />
        <Button
          type="submit"
          variant="primary"
          size="lg"
          loading={login.isPending}
          className="mt-2 w-full"
        >
          로그인
        </Button>
      </form>
    </div>
  );
}
