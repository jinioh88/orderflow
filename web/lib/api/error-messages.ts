import { API_ERROR_CODES, ApiError, CLIENT_ERROR_CODES } from "./types";

// 공통 코드(1.4)에 대한 표시 문구. 서버 message는 계약이 아니므로(1.3)
// 사용자 노출 문구는 클라이언트가 소유한다. 도메인 코드는 각 feature에서 확장.
const MESSAGES: Record<string, string> = {
  [API_ERROR_CODES.INVALID_REQUEST]: "잘못된 요청입니다.",
  [API_ERROR_CODES.VALIDATION_ERROR]: "입력값을 확인해 주세요.",
  [API_ERROR_CODES.UNAUTHORIZED]: "로그인이 필요합니다.",
  [API_ERROR_CODES.TOKEN_EXPIRED]: "로그인이 만료되었습니다. 다시 로그인해 주세요.",
  [API_ERROR_CODES.FORBIDDEN]: "이 작업을 수행할 권한이 없습니다.",
  [API_ERROR_CODES.RESOURCE_NOT_FOUND]: "요청한 데이터를 찾을 수 없습니다.",
  [API_ERROR_CODES.METHOD_NOT_ALLOWED]: "허용되지 않은 요청입니다.",
  [API_ERROR_CODES.CONFLICT]: "요청을 처리할 수 없는 상태입니다.",
  [API_ERROR_CODES.INTERNAL_ERROR]:
    "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
  [CLIENT_ERROR_CODES.NETWORK_ERROR]:
    "서버에 연결할 수 없습니다. 네트워크 상태를 확인해 주세요.",
  [CLIENT_ERROR_CODES.UNEXPECTED_RESPONSE]:
    "서버 응답을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.",
};

export function userMessageOf(error: unknown): string {
  if (error instanceof ApiError) {
    return MESSAGES[error.code] ?? "요청 처리 중 오류가 발생했습니다.";
  }
  return "알 수 없는 오류가 발생했습니다.";
}
