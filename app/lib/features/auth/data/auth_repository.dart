import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/network_providers.dart';
import 'auth_models.dart';

/// AUTH API 호출 (api-spec 2.4).
///
/// 클라이언트를 둘로 나눠 받는다:
/// - [anonymous] — 로그인·토큰 재발급(2.4.2·2.4.3). 스펙상 익명 API이고, 만료된 토큰을
///   붙이거나 재발급 인터셉터가 자기 자신에게 재귀하는 일을 구조적으로 막는다.
/// - [authenticated] — 비밀번호 설정·로그아웃(2.4.4·2.4.5). Authorization 헤더가 필요하다.
final class AuthRepository {
  const AuthRepository({required this.anonymous, required this.authenticated});

  final ApiClient anonymous;
  final ApiClient authenticated;

  /// 2.4.2 로그인. 실패는 `401 INVALID_CREDENTIALS` / `403 ACCOUNT_INACTIVE`.
  Future<AuthSession> login({
    required String email,
    required String password,
  }) async {
    final data = await anonymous.post(
      '/auth/login',
      body: {'email': email, 'password': password},
    );
    return AuthSession.fromJson(data as Map<String, dynamic>);
  }

  /// 2.4.3 토큰 재발급. 성공 시 회전된 **새 토큰 쌍**이 온다.
  /// 실패는 `401 INVALID_REFRESH_TOKEN` — 재로그인 대상이다.
  Future<TokenPair> refresh(String refreshToken) async {
    final data = await anonymous.post(
      '/auth/refresh',
      body: {'refreshToken': refreshToken},
    );
    return TokenPair.fromJson(data as Map<String, dynamic>);
  }

  /// 2.4.4 비밀번호 설정/변경. 성공 시 기존 리프레시 토큰이 전부 무효화되고
  /// 새 토큰 쌍이 응답에 담겨 온다 — 호출자는 반드시 이 토큰으로 교체해야 한다.
  Future<TokenPair> changePassword({
    required String currentPassword,
    required String newPassword,
  }) async {
    final data = await authenticated.put(
      '/users/me/password',
      body: {'currentPassword': currentPassword, 'newPassword': newPassword},
    );
    return TokenPair.fromJson(data as Map<String, dynamic>);
  }

  /// 2.4.5 로그아웃 — 해당 리프레시 토큰 1개를 무효화한다. 이미 무효해도 204(멱등).
  Future<void> logout(String refreshToken) =>
      authenticated.post('/auth/logout', body: {'refreshToken': refreshToken});
}

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(
    anonymous: ref.watch(anonymousApiClientProvider),
    authenticated: ref.watch(apiClientProvider),
  );
});
