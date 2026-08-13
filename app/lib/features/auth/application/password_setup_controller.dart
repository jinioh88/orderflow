import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'auth_controller.dart';
import 'auth_error_message.dart';
import 'submit_state.dart';

/// 비밀번호 정책 (api-spec 2.3): 8~64자, 영문자 1자 이상 + 숫자 1자 이상.
///
/// 서버도 같은 규칙으로 검증하지만(`400 VALIDATION_ERROR`), 왕복 한 번을 아끼려고
/// 클라이언트에서 먼저 본다. **정본은 서버다** — 규칙이 갈리면 서버 응답을 그대로 보여준다.
abstract final class PasswordPolicy {
  static const minLength = 8;
  static const maxLength = 64;

  static final _letter = RegExp(r'[A-Za-z]');
  static final _digit = RegExp(r'[0-9]');

  /// 위반하면 사유, 통과하면 null (02-patterns §3.1 — "원인 + 해결" 톤).
  static String? validate(String password) {
    if (password.length < minLength || password.length > maxLength) {
      return '비밀번호는 $minLength~$maxLength자여야 합니다';
    }
    if (!_letter.hasMatch(password) || !_digit.hasMatch(password)) {
      return '영문자와 숫자를 각각 1자 이상 포함해야 합니다';
    }
    return null;
  }
}

/// 최초 로그인 비밀번호 설정 화면의 제출 상태 컨트롤러 (US-AUTH-02·03, api-spec 2.4.4).
class PasswordSetupController extends SubmitController {
  /// 이 화면의 `401 INVALID_CREDENTIALS`는 임시 비밀번호 불일치를 뜻한다 (2.4.4).
  @override
  AuthErrorContext get errorContext => AuthErrorContext.passwordSetup;

  Future<void> submit({
    required String currentPassword,
    required String newPassword,
  }) {
    return submitting(
      () => ref
          .read(authControllerProvider.notifier)
          .completePasswordSetup(
            currentPassword: currentPassword,
            newPassword: newPassword,
          ),
    );
  }
}

final passwordSetupControllerProvider =
    NotifierProvider.autoDispose<PasswordSetupController, SubmitState>(
      PasswordSetupController.new,
    );
