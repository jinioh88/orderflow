import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/auth/application/auth_controller.dart';
import 'api_client.dart';

/// [Dio] 인스턴스 팩토리.
///
/// 클라이언트마다 인터셉터가 다르므로 인스턴스를 공유하지 않고 매번 새로 만든다.
/// 테스트는 **이 Provider 하나만** override해서 가짜 어댑터를 꽂는다 — 토큰 주입·재발급
/// 인터셉터 배선은 실제 코드 그대로 두고 네트워크 경계만 바꾸기 위한 이음매다.
final dioFactoryProvider = Provider<Dio Function()>((ref) {
  return () => Dio(ApiClient.defaultOptions);
});

/// 인증 헤더가 붙지 않는 클라이언트 — 익명 API 전용(로그인·토큰 재발급, api-spec 2.4.2·2.4.3).
///
/// 재발급 요청까지 [apiClientProvider]로 보내면 "재발급 중 401 → 다시 재발급"이 가능해진다.
/// 클라이언트를 분리해 그 경로를 구조적으로 없앤다.
final anonymousApiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(dio: ref.watch(dioFactoryProvider)());
});

/// 인증이 필요한 API용 클라이언트.
///
/// 토큰 읽기·재발급을 **호출 시점에** `ref.read`로 가져온다. 생성 시점에 인증 컨트롤러를
/// 읽으면 `authController → repository → apiClient → authController` 순환이 생긴다.
final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(
    dio: ref.watch(dioFactoryProvider)(),
    readAccessToken: () async => ref.read(authControllerProvider).accessToken,
    refreshAccessToken: () =>
        ref.read(authControllerProvider.notifier).refreshAccessToken(),
    onPasswordSetupRequired: () =>
        ref.read(authControllerProvider.notifier).markPasswordSetupRequired(),
  );
});
