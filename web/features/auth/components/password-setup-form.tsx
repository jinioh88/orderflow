"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { userMessageOf } from "@/lib/api/error-messages";
import { API_ERROR_CODES, ApiError } from "@/lib/api/types";
import { AUTH_ERROR_CODES } from "../types";
import { useSetPassword } from "../hooks/use-set-password";

/**
 * 최초 로그인 비밀번호 설정 (US-AUTH-02·03, 스펙 2.3·2.4.4).
 * 임시 비밀번호(currentPassword)로 본인 확인 후 새 비밀번호를 설정한다.
 * 정책: 8~64자, 영문자 1자 이상 + 숫자 1자 이상, 임시 비밀번호와 달라야 한다.
 */

const FIELD_IDS = {
  currentPassword: "setup-current",
  newPassword: "setup-new",
  confirmPassword: "setup-confirm",
} as const;
type FieldName = keyof typeof FIELD_IDS;
type FieldErrors = Partial<Record<FieldName, string>>;

function validateField(
  name: FieldName,
  values: Record<FieldName, string>,
): string | undefined {
  const value = values[name];
  if (!value) {
    if (name === "currentPassword") return "임시 비밀번호를 입력해 주세요.";
    if (name === "newPassword") return "새 비밀번호를 입력해 주세요.";
    return "새 비밀번호를 한 번 더 입력해 주세요.";
  }
  if (name === "newPassword") {
    if (value.length < 8 || value.length > 64)
      return "비밀번호는 8~64자여야 합니다.";
    if (!/[a-zA-Z]/.test(value) || !/[0-9]/.test(value))
      return "영문자와 숫자를 각각 1자 이상 포함해야 합니다.";
    if (values.currentPassword && value === values.currentPassword)
      return "임시 비밀번호와 다른 비밀번호를 사용해 주세요.";
  }
  if (name === "confirmPassword" && value !== values.newPassword)
    return "새 비밀번호와 일치하지 않습니다.";
  return undefined;
}

function focusField(errors: FieldErrors) {
  const first = (Object.keys(FIELD_IDS) as FieldName[]).find(
    (name) => errors[name],
  );
  if (first) document.getElementById(FIELD_IDS[first])?.focus();
}

export function PasswordSetupForm() {
  const setPassword = useSetPassword();
  const [values, setValues] = useState<Record<FieldName, string>>({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);

  const handleChange = (name: FieldName, value: string) => {
    setValues((current) => ({ ...current, [name]: value }));
    setFieldErrors((current) => ({ ...current, [name]: undefined }));
    setFormError(null);
  };

  const handleBlur = (name: FieldName) => {
    const message = validateField(name, values);
    if (message) setFieldErrors((current) => ({ ...current, [name]: message }));
  };

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    const errors: FieldErrors = {};
    for (const name of Object.keys(FIELD_IDS) as FieldName[]) {
      errors[name] = validateField(name, values);
    }
    if (Object.values(errors).some(Boolean)) {
      setFieldErrors(errors);
      focusField(errors);
      return;
    }
    setPassword.mutate(
      {
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      },
      {
        onError: (error) => {
          if (error instanceof ApiError) {
            // 401 INVALID_CREDENTIALS = 임시 비밀번호 불일치 (스펙 2.4.4)
            if (error.code === AUTH_ERROR_CODES.INVALID_CREDENTIALS) {
              const mapped = {
                currentPassword: "임시 비밀번호가 올바르지 않습니다.",
              };
              setFieldErrors(mapped);
              focusField(mapped);
              return;
            }
            if (error.code === API_ERROR_CODES.VALIDATION_ERROR) {
              const serverErrors = error.fieldErrors();
              const mapped: FieldErrors = {
                currentPassword: serverErrors.currentPassword,
                newPassword: serverErrors.newPassword,
              };
              if (mapped.currentPassword || mapped.newPassword) {
                setFieldErrors(mapped);
                focusField(mapped);
                return;
              }
            }
          }
          setFormError(userMessageOf(error));
        },
      },
    );
  };

  return (
    <div className="w-100 rounded-lg border border-border bg-surface p-8 shadow-1">
      <h1 className="text-display text-fg-title">비밀번호 설정</h1>
      <p className="mt-1 text-body-md text-fg-caption">
        발급받은 임시 비밀번호를 확인하고 새 비밀번호를 설정해 주세요.
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
          id={FIELD_IDS.currentPassword}
          label="임시 비밀번호"
          type="password"
          autoComplete="current-password"
          value={values.currentPassword}
          error={fieldErrors.currentPassword}
          onChange={(e) => handleChange("currentPassword", e.target.value)}
          onBlur={() => handleBlur("currentPassword")}
        />
        <Input
          id={FIELD_IDS.newPassword}
          label="새 비밀번호"
          hint="(8~64자, 영문+숫자)"
          type="password"
          autoComplete="new-password"
          value={values.newPassword}
          error={fieldErrors.newPassword}
          onChange={(e) => handleChange("newPassword", e.target.value)}
          onBlur={() => handleBlur("newPassword")}
        />
        <Input
          id={FIELD_IDS.confirmPassword}
          label="새 비밀번호 확인"
          type="password"
          autoComplete="new-password"
          value={values.confirmPassword}
          error={fieldErrors.confirmPassword}
          onChange={(e) => handleChange("confirmPassword", e.target.value)}
          onBlur={() => handleBlur("confirmPassword")}
        />
        <Button
          type="submit"
          variant="primary"
          size="lg"
          loading={setPassword.isPending}
          className="mt-2 w-full"
        >
          설정 완료
        </Button>
      </form>
    </div>
  );
}
