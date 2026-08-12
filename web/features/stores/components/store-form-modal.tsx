"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { userMessageOf } from "@/lib/api/error-messages";
import { API_ERROR_CODES, ApiError } from "@/lib/api/types";
import { useCreateStore } from "../hooks/use-store-mutations";

/**
 * 가맹점 등록 모달 (US-AUTH-02, 스펙 2.4.6). 필수는 name뿐이라 선택 항목에 "(선택)" 표기 (02 §3).
 * 수정 API는 스펙에 없어 등록 전용이다 (사용자 확정 2026-08-12 — 스펙 범위만 구현).
 */

const FIELD_IDS = { name: "store-name", address: "store-address" } as const;
type FieldName = keyof typeof FIELD_IDS;
type FieldErrors = Partial<Record<FieldName, string>>;

export function StoreFormModal({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  const createStore = useCreateStore();
  const [values, setValues] = useState({ name: "", address: "" });
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);

  const reset = () => {
    setValues({ name: "", address: "" });
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

  const validateName = (value: string) =>
    value.trim() ? undefined : "가맹점명을 입력해 주세요.";

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    const nameError = validateName(values.name);
    if (nameError) {
      setFieldErrors({ name: nameError });
      document.getElementById(FIELD_IDS.name)?.focus();
      return;
    }
    createStore.mutate(
      {
        name: values.name.trim(),
        ...(values.address.trim() ? { address: values.address.trim() } : {}),
      },
      {
        onSuccess: close,
        onError: (error) => {
          if (
            error instanceof ApiError &&
            error.code === API_ERROR_CODES.VALIDATION_ERROR
          ) {
            const serverErrors = error.fieldErrors();
            if (serverErrors.name || serverErrors.address) {
              setFieldErrors({
                name: serverErrors.name,
                address: serverErrors.address,
              });
              return;
            }
          }
          setFormError(userMessageOf(error));
        },
      },
    );
  };

  return (
    <Modal
      open={open}
      title="가맹점 등록"
      size="sm"
      closeDisabled={createStore.isPending}
      onClose={close}
      footer={
        <>
          <Button
            variant="secondary"
            disabled={createStore.isPending}
            onClick={close}
          >
            취소
          </Button>
          <Button
            variant="primary"
            type="submit"
            form="store-form"
            loading={createStore.isPending}
          >
            등록
          </Button>
        </>
      }
    >
      <form
        id="store-form"
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
        <Input
          id={FIELD_IDS.name}
          label="가맹점명"
          placeholder="강남역점"
          value={values.name}
          error={fieldErrors.name}
          onChange={(e) => handleChange("name", e.target.value)}
          onBlur={() =>
            setFieldErrors((c) => ({ ...c, name: validateName(values.name) }))
          }
        />
        <Input
          id={FIELD_IDS.address}
          label="주소"
          hint="(선택)"
          placeholder="서울 강남구 …"
          value={values.address}
          error={fieldErrors.address}
          onChange={(e) => handleChange("address", e.target.value)}
        />
      </form>
    </Modal>
  );
}
