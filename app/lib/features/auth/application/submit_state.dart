import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'auth_error_message.dart';

/// 폼 제출의 화면 상태 (제출 중 · 전역 에러 문구).
///
/// 세션 자체는 `AuthController`가 갖고, 화면에만 필요한 상태를 여기서 분리한다
/// (app/CLAUDE.md 구현 원칙 6 — 위젯은 렌더링과 입력 전달만).
final class SubmitState {
  const SubmitState({this.submitting = false, this.errorMessage});

  final bool submitting;

  /// 폼 상단 배너에 띄울 전역 에러 (02-patterns §3.1 — 필드 오류는 필드 아래, 전역은 상단).
  final String? errorMessage;

  bool get hasError => errorMessage != null;
}

/// "한 번에 하나만, 실패하면 문구로" — 인증 화면들의 공통 제출 절차.
///
/// 로그인·비밀번호 설정·로그아웃이 모두 같은 모양이라 절차를 여기 한 곳에 둔다.
/// 하위 클래스는 [run]에 실제 동작만 채운다.
abstract class SubmitController extends Notifier<SubmitState> {
  @override
  SubmitState build() => const SubmitState();

  /// 에러 문구를 만들 때 쓰는 화면 맥락 (같은 코드라도 화면마다 원인이 다르다).
  AuthErrorContext get errorContext => AuthErrorContext.login;

  /// 중복 제출을 막고, 진행 중 상태·실패 문구를 관리하며 [action]을 실행한다.
  ///
  /// 성공하면 대개 라우터가 화면을 바꾸고 이 컨트롤러(autoDispose)는 폐기된다 —
  /// 폐기된 뒤 상태를 쓰면 예외가 나므로 [Ref.mounted]로 확인하고 쓴다.
  Future<void> submitting(Future<void> Function() action) async {
    if (state.submitting) return;
    state = const SubmitState(submitting: true);
    try {
      await action();
      if (ref.mounted) state = const SubmitState();
    } catch (e) {
      if (ref.mounted) {
        state = SubmitState(
          errorMessage: authErrorMessage(e, context: errorContext),
        );
      }
    }
  }
}
