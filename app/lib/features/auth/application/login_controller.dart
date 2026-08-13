import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'auth_controller.dart';
import 'submit_state.dart';

/// 로그인 화면의 제출 상태 컨트롤러 (US-AUTH-03).
///
/// 성공 후의 화면 이동은 여기서 하지 않는다 — [AuthController]의 상태가 바뀌면
/// 라우터 가드가 알아서 보낸다. 화면이 이동을 직접 지시하지 않는 편이,
/// 자동 로그인·비밀번호 설정 강제 이동과 규칙이 한 곳에 모인다.
class LoginController extends SubmitController {
  Future<void> submit({required String email, required String password}) {
    return submitting(
      () => ref
          .read(authControllerProvider.notifier)
          .login(email: email.trim(), password: password),
    );
  }
}

final loginControllerProvider =
    NotifierProvider.autoDispose<LoginController, SubmitState>(
      LoginController.new,
    );
