"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { Select } from "@/components/ui/select";
import { AUTH_ERROR_CODES } from "@/features/auth/types";
import { useStores } from "@/features/stores/hooks/use-stores";
import { userMessageOf } from "@/lib/api/error-messages";
import { API_ERROR_CODES, ApiError } from "@/lib/api/types";
import { useCreateAccount } from "../hooks/use-account-mutations";
import type { CreateAccountResponse } from "../types";

/**
 * 점주 계정 등록 모달 (US-AUTH-02, 스펙 2.4.9). MVP에서 만들 수 있는 역할은
 * STORE_OWNER뿐이라 역할 선택이 없다. 성공 시 임시 비밀번호 다이얼로그는 호출부가 연다.
 * 소속 가맹점 셀렉트는 운영중 가맹점 첫 100건 — 초과분 대응은 발견 항목에 기록.
 */

const FIELD_IDS = {
  storeId: "account-store",
  email: "account-email",
  name: "account-name",
} as const;
type FieldName = keyof typeof FIELD_IDS;
type FieldErrors = Partial<Record<FieldName, string>>;

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function AccountFormModal({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  /** 등록 성공 — 임시 비밀번호 표시는 부모가 이 응답으로 이어간다 */
  onCreated: (account: CreateAccountResponse) => void;
}) {
  const createAccount = useCreateAccount();
  // 점주는 운영중 가맹점에만 등록 가능 (비활성 가맹점은 409 STORE_INACTIVE — 2.4.9)
  const stores = useStores({ status: "ACTIVE", size: 100, sort: "name,asc" });

  const [values, setValues] = useState({ storeId: "", email: "", name: "" });
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);

  const reset = () => {
    setValues({ storeId: "", email: "", name: "" });
    setFieldErrors({});
    setFormError(null);
  };

  const close = () => {
    reset();
    onClose();
  };

  const handleChange = (name: FieldName, value: string) => {
    setValues((current) => ({ ...current, [name]: value }));
    setFieldErrors((current) => ({ ...current, [name]: undefined }));
    setFormError(null);
  };

  const validateField = (name: FieldName): string | undefined => {
    const value = values[name];
    if (name === "storeId" && !value) return "소속 가맹점을 선택해 주세요.";
    if (name === "email") {
      if (!value) return "이메일을 입력해 주세요.";
      if (!EMAIL_PATTERN.test(value))
        return "이메일 형식이 아닙니다 (예: name@company.com)";
    }
    if (name === "name" && !value.trim()) return "이름을 입력해 주세요.";
    return undefined;
  };

  const focusField = (errors: FieldErrors) => {
    const first = (Object.keys(FIELD_IDS) as FieldName[]).find(
      (name) => errors[name],
    );
    if (first) document.getElementById(FIELD_IDS[first])?.focus();
  };

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    const errors: FieldErrors = {
      storeId: validateField("storeId"),
      email: validateField("email"),
      name: validateField("name"),
    };
    if (Object.values(errors).some(Boolean)) {
      setFieldErrors(errors);
      focusField(errors);
      return;
    }
    createAccount.mutate(
      {
        storeId: Number(values.storeId),
        email: values.email.trim(),
        name: values.name.trim(),
      },
      {
        onSuccess: (account) => {
          close();
          onCreated(account);
        },
        onError: (error) => {
          if (error instanceof ApiError) {
            if (error.code === AUTH_ERROR_CODES.EMAIL_DUPLICATED) {
              const mapped = { email: "이미 사용 중인 이메일입니다." };
              setFieldErrors(mapped);
              focusField(mapped);
              return;
            }
            if (error.code === AUTH_ERROR_CODES.STORE_INACTIVE) {
              const mapped = {
                storeId: "비활성화된 가맹점입니다. 다른 가맹점을 선택해 주세요.",
              };
              setFieldErrors(mapped);
              focusField(mapped);
              return;
            }
            if (error.code === API_ERROR_CODES.VALIDATION_ERROR) {
              const serverErrors = error.fieldErrors();
              const mapped: FieldErrors = {
                storeId: serverErrors.storeId,
                email: serverErrors.email,
                name: serverErrors.name,
              };
              if (Object.values(mapped).some(Boolean)) {
                setFieldErrors(mapped);
                focusField(mapped);
                return;
              }
            }
          }
          // 404 RESOURCE_NOT_FOUND(없는·타 테넌트 가맹점) 포함 — 공통 문구로 흡수 (US-AUTH-04)
          setFormError(userMessageOf(error));
        },
      },
    );
  };

  return (
    <Modal
      open={open}
      title="점주 계정 등록"
      size="sm"
      closeDisabled={createAccount.isPending}
      onClose={close}
      footer={
        <>
          <Button
            variant="secondary"
            disabled={createAccount.isPending}
            onClick={close}
          >
            취소
          </Button>
          <Button
            variant="primary"
            type="submit"
            form="account-form"
            loading={createAccount.isPending}
          >
            등록
          </Button>
        </>
      }
    >
      <form
        id="account-form"
        onSubmit={handleSubmit}
        noValidate
        className="flex flex-col gap-4"
      >
        {formError && (
          <div
            role="alert"
            className="rounded-md bg-danger-bg px-3 py-2 text-body-md text-danger-text"
          >
            {formError}
          </div>
        )}
        <Select
          id={FIELD_IDS.storeId}
          label="소속 가맹점"
          value={values.storeId}
          error={fieldErrors.storeId}
          disabled={stores.isPending}
          onChange={(e) => handleChange("storeId", e.target.value)}
        >
          <option value="" disabled>
            {stores.isPending ? "불러오는 중…" : "가맹점 선택"}
          </option>
          {stores.data?.items.map((store) => (
            <option key={store.id} value={store.id}>
              {store.name}
            </option>
          ))}
        </Select>
        <Input
          id={FIELD_IDS.email}
          label="이메일"
          type="email"
          placeholder="owner@example.com"
          value={values.email}
          error={fieldErrors.email}
          onChange={(e) => handleChange("email", e.target.value)}
          onBlur={() =>
            setFieldErrors((c) => ({ ...c, email: validateField("email") }))
          }
        />
        <Input
          id={FIELD_IDS.name}
          label="이름"
          placeholder="박점주"
          value={values.name}
          error={fieldErrors.name}
          onChange={(e) => handleChange("name", e.target.value)}
          onBlur={() =>
            setFieldErrors((c) => ({ ...c, name: validateField("name") }))
          }
        />
      </form>
    </Modal>
  );
}
