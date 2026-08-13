import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'auth_controller.dart';
import 'submit_state.dart';

/// 로그아웃 버튼의 진행 상태 컨트롤러 (api-spec 2.4.5).
///
/// 로그아웃은 서버 왕복(느린 회선에서는 수 초)이 있는 동작이다. 진행 표시가 없으면
/// 버튼이 먹통으로 보여 사용자가 계속 다시 누르고, 그만큼 요청이 중복으로 나간다.
///
/// 실패해도 [AuthController.logout]이 로컬 세션은 반드시 지우므로 에러 문구는 뜨지 않는다.
class LogoutController extends SubmitController {
  Future<void> submit() =>
      submitting(() => ref.read(authControllerProvider.notifier).logout());
}

final logoutControllerProvider =
    NotifierProvider.autoDispose<LogoutController, SubmitState>(
      LogoutController.new,
    );
