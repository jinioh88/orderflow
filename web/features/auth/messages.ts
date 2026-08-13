import { extendErrorMessages } from "@/lib/api/error-messages";
import { AUTH_CLIENT_ERROR_CODES, AUTH_ERROR_CODES } from "./types";

/** AUTH 에러 코드(2.5)의 사용자 표시 문구. auth-context 로드 시 1회 등록된다 */
export function registerAuthMessages() {
  extendErrorMessages({
    [AUTH_ERROR_CODES.INVALID_CREDENTIALS]:
      "이메일 또는 비밀번호가 올바르지 않습니다.",
    [AUTH_ERROR_CODES.INVALID_REFRESH_TOKEN]:
      "로그인이 만료되었습니다. 다시 로그인해 주세요.",
    [AUTH_ERROR_CODES.ACCOUNT_INACTIVE]:
      "사용이 중지된 계정입니다. 본사 관리자에게 문의해 주세요.",
    [AUTH_ERROR_CODES.PASSWORD_SETUP_REQUIRED]:
      "비밀번호를 설정한 뒤 이용할 수 있습니다.",
    [AUTH_ERROR_CODES.EMAIL_DUPLICATED]: "이미 사용 중인 이메일입니다.",
    [AUTH_ERROR_CODES.STORE_INACTIVE]: "비활성화된 가맹점입니다.",
    [AUTH_CLIENT_ERROR_CODES.WEB_ACCESS_DENIED]:
      "이 계정으로는 관리자 화면에 로그인할 수 없습니다. 가맹점 발주는 모바일 앱을 이용해 주세요.",
  });
}
