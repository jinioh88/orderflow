import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/app_exception.dart';
import '../data/auth_models.dart';
import '../data/auth_repository.dart';
import '../data/auth_session_store.dart';

/// 앱 전체의 인증 상태.
///
/// 라우터 가드(`app_router.dart`)가 이 상태만 보고 화면을 정한다.
enum AuthStatus {
  /// 저장된 세션을 읽는 중 — 아직 로그인/카탈로그 어느 쪽으로도 보내면 안 된다.
  initializing,

  /// 세션 없음 → 로그인 화면.
  unauthenticated,

  /// 임시 비밀번호 상태(api-spec 2.3) → 비밀번호 설정 화면에 묶어 둔다.
  passwordSetupRequired,

  /// 정상 이용 가능.
  authenticated,
}

final class AuthState {
  const AuthState._({required this.status, this.session});

  const AuthState.initializing() : this._(status: AuthStatus.initializing);

  const AuthState.unauthenticated()
    : this._(status: AuthStatus.unauthenticated);

  factory AuthState.of(AuthSession session) => AuthState._(
    status: session.passwordSetupRequired
        ? AuthStatus.passwordSetupRequired
        : AuthStatus.authenticated,
    session: session,
  );

  final AuthStatus status;
  final AuthSession? session;

  String? get accessToken => session?.accessToken;
  AuthUser? get user => session?.user;
}

/// 세션 수명 주기 컨트롤러 — 복원 · 로그인 · 재발급 · 비밀번호 설정 · 로그아웃.
///
/// 화면(위젯)은 이 컨트롤러를 통해서만 세션을 다룬다 (app/CLAUDE.md 구현 원칙 6).
/// 폼의 제출 중·에러 표시 같은 화면 상태는 여기가 아니라 각 화면 컨트롤러가 갖는다.
class AuthController extends Notifier<AuthState> {
  /// 저장된 세션 복원이 끝나면 완료되는 Future — 테스트·부팅 대기용.
  late Future<void> initialized;

  @override
  AuthState build() {
    initialized = _restore();
    return const AuthState.initializing();
  }

  AuthSessionStore get _store => ref.read(authSessionStoreProvider);
  AuthRepository get _repository => ref.read(authRepositoryProvider);

  Future<void> _restore() async {
    // [AuthSessionStore.read]는 예외를 던지지 않지만, 여기서 한 번 더 막는다 —
    // 이 Future는 프로덕션에서 아무도 await하지 않으므로 예외가 나면
    // 상태가 initializing에 남아 부팅 화면에서 영구히 멈춘다.
    AuthSession? session;
    try {
      session = await _store.read();
    } catch (_) {
      session = null;
    }
    state = session == null
        ? const AuthState.unauthenticated()
        : AuthState.of(session);
  }

  /// 로그인 (2.4.2). 실패는 [AppException] 그대로 던진다 — 문구 변환은 화면 컨트롤러 몫이다.
  Future<void> login({required String email, required String password}) async {
    final session = await _repository.login(email: email, password: password);
    await _persist(session);
    state = AuthState.of(session);
  }

  /// 세션을 저장하되, **저장 실패로 로그인을 실패시키지 않는다.**
  ///
  /// 저장소가 죽은 기기에서도 이번 실행 동안은 정상적으로 앱을 쓸 수 있어야 한다
  /// (다음 기동 때 자동 로그인이 안 될 뿐이다). 서버는 이미 인증을 마쳤다.
  Future<void> _persist(AuthSession session) async {
    try {
      await _store.write(session);
    } catch (_) {
      // 무시 — 메모리 세션으로 계속 진행한다.
    }
  }

  /// 액세스 토큰 재발급 (2.4.3). [ApiClient]의 `401 TOKEN_EXPIRED` 인터셉터가 호출한다.
  ///
  /// - 성공: 회전된 새 토큰 쌍을 저장하고 새 액세스 토큰을 돌려준다.
  /// - 4xx(무효·만료·계정 비활성): 세션을 정리한다 → 라우터 가드가 로그인 화면으로 보낸다.
  ///   재발급이 거절된 세션은 어떤 화면에서도 쓸 수 없으므로 붙잡고 있을 이유가 없다.
  /// - 5xx·네트워크 실패: **세션을 지우지 않는다.** 서버·연결 문제일 뿐 세션은 유효할 수 있다.
  ///
  /// **어떤 경우에도 예외를 던지지 않는다** — 호출자가 dio 인터셉터 안이라, 여기서 던진
  /// 예외는 원 요청의 실패 원인을 "네트워크 오류"로 둔갑시킨다.
  Future<String?> refreshAccessToken() async {
    final session = state.session;
    if (session == null) return null;

    try {
      final tokens = await _repository.refresh(session.refreshToken);
      final refreshed = session.copyWithTokens(tokens);
      await _persist(refreshed);
      state = AuthState.of(refreshed);
      return refreshed.accessToken;
    } on ApiException catch (e) {
      if (e.statusCode >= 400 && e.statusCode < 500) await _clear();
      return null;
    } catch (_) {
      // 네트워크 실패, 예상 밖 응답 형식 등 — 세션은 그대로 두고 재발급만 실패 처리한다.
      return null;
    }
  }

  /// 임시 비밀번호 상태로의 강제 전환 (2.3).
  ///
  /// 본사가 임시 비밀번호를 재발급하면(2.4.10) 서버는 그때부터 업무 API를
  /// `403 PASSWORD_SETUP_REQUIRED`로 거절하는데, 앱이 들고 있는 세션 플래그는 아직 false다.
  /// 그 응답을 본 [ApiClient]가 이 메서드를 불러 상태를 맞추면 라우터가 설정 화면으로 보낸다.
  Future<void> markPasswordSetupRequired() async {
    final session = state.session;
    if (session == null || session.passwordSetupRequired) return;

    final updated = session.copyWith(passwordSetupRequired: true);
    await _persist(updated);
    state = AuthState.of(updated);
  }

  /// 비밀번호 설정 (2.4.4). 성공 시 임시 상태가 풀리고 새 토큰 쌍으로 교체된다.
  Future<void> completePasswordSetup({
    required String currentPassword,
    required String newPassword,
  }) async {
    final session = state.session;
    // 세션 없이 이 화면에 있을 수 없다. 조용히 성공한 척하면 화면이 멈춘 것처럼 보이므로,
    // 세션을 정리해 로그인 화면으로 되돌린다.
    if (session == null) {
      await _clear();
      return;
    }

    final tokens = await _repository.changePassword(
      currentPassword: currentPassword,
      newPassword: newPassword,
    );
    final updated = session.copyWithTokens(
      tokens,
      passwordSetupRequired: false,
    );
    await _persist(updated);
    state = AuthState.of(updated);
  }

  /// 로그아웃 (2.4.5).
  ///
  /// 서버 호출이 실패해도 **로컬 세션은 반드시 지운다** — 사용자가 로그아웃을 눌렀는데
  /// 네트워크 때문에 로그인 상태로 남는 것이 더 나쁘다. 서버 쪽 토큰은 TTL(14일)로 만료된다.
  Future<void> logout() async {
    final session = state.session;
    if (session != null) {
      await _revoke(session.refreshToken);

      // 요청 중 액세스 토큰이 만료돼 재발급이 끼어들었다면(2.5) 리프레시 토큰이
      // **회전**되어(2.2) 방금 보낸 토큰은 이미 죽었고 새 토큰이 서버에 살아 있다.
      // 그 새 토큰까지 무효화해야 로그아웃이 실제로 끝난다.
      final rotated = state.session?.refreshToken;
      if (rotated != null && rotated != session.refreshToken) {
        await _revoke(rotated);
      }
    }
    await _clear();
  }

  Future<void> _revoke(String refreshToken) async {
    try {
      await _repository.logout(refreshToken);
    } catch (_) {
      // 무시 — 호출자가 로컬 세션을 지운다.
    }
  }

  Future<void> _clear() async {
    await _store.clear();
    state = const AuthState.unauthenticated();
  }
}

final authControllerProvider = NotifierProvider<AuthController, AuthState>(
  AuthController.new,
);
