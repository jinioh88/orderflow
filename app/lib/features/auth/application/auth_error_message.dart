import '../../../core/network/app_exception.dart';

/// 같은 에러 코드라도 화면에 따라 사용자가 고쳐야 할 것이 다르다.
///
/// `401 INVALID_CREDENTIALS`가 대표적이다 — 로그인에서는 "이메일/비밀번호 불일치"지만,
/// 비밀번호 설정(2.4.4)에서는 "currentPassword 불일치"다. 이메일 입력란이 없는 화면에
/// 이메일을 탓하는 문구를 띄우지 않기 위해 호출 맥락을 받는다.
enum AuthErrorContext { login, passwordSetup }

/// 에러 → 화면에 띄울 문구.
///
/// api-spec 1.3: **분기는 HTTP 상태 + code로만** 한다. 서버 `message`는 표시용이라
/// 문구가 계약이 아니므로, 사용자 행동이 갈리는 코드는 앱이 직접 문구를 정한다.
/// 문구 톤은 02-patterns §3.1("원인 + 해결") · §5(마이크로카피).
String authErrorMessage(
  Object error, {
  AuthErrorContext context = AuthErrorContext.login,
}) {
  if (error is ApiException) {
    switch (error.code) {
      case 'INVALID_CREDENTIALS':
        return switch (context) {
          // 스펙 2.4.2 — 이메일/비밀번호 중 무엇이 틀렸는지 서버가 구분하지 않는다(계정 존재 비노출).
          AuthErrorContext.login => '이메일 또는 비밀번호가 올바르지 않습니다',
          // 스펙 2.4.4 — 이 화면에서 같은 코드는 현재(임시) 비밀번호 불일치를 뜻한다.
          AuthErrorContext.passwordSetup =>
            '임시 비밀번호가 올바르지 않습니다. 본사에서 받은 값을 다시 확인하세요',
        };
      case 'ACCOUNT_INACTIVE':
        return '사용이 중지된 계정입니다. 본사에 문의하세요';
      case 'INVALID_REFRESH_TOKEN':
      case 'UNAUTHORIZED':
        return '로그인이 만료되었습니다. 다시 로그인해 주세요';
      case 'PASSWORD_SETUP_REQUIRED':
        // 라우터가 곧 비밀번호 설정 화면으로 보내지만, 그 사이 화면에 뜰 수 있다.
        return '비밀번호를 새로 설정해야 합니다';
      case 'VALIDATION_ERROR':
        // 필드 오류가 오면 첫 사유를 그대로 보여준다 (비밀번호 정책 위반 등 — 2.3).
        return error.details.isEmpty
            ? error.message
            : error.details.first.reason;
      default:
        return error.message;
    }
  }
  if (error is AppException) return error.message;
  return '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요';
}
