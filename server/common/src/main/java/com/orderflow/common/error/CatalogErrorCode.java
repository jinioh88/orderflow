package com.orderflow.common.error;

/**
 * CAT 에픽 에러 코드 — api-spec.md 3.4와 1:1로 대응한다.
 */
public enum CatalogErrorCode implements ErrorCode {

    PRODUCT_CODE_DUPLICATED(409, "이미 사용 중인 품목코드입니다."),
    BARCODE_DUPLICATED(409, "이미 사용 중인 바코드입니다."),
    ALREADY_LIMITED(409, "이미 한정 품목으로 지정된 상품입니다."),
    NOT_LIMITED(409, "한정 품목이 아닌 상품입니다."),
    EXCEL_FILE_INVALID(400, "엑셀 파일을 처리할 수 없습니다."),
    EXCEL_VALIDATION_FAILED(400, "엑셀 검증에 실패했습니다.");

    private final int status;
    private final String message;

    CatalogErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public int status() {
        return status;
    }
}
