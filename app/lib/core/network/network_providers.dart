import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'api_client.dart';

/// 전역 [ApiClient] Provider.
///
/// 토큰 공급자는 AUTH 단계에서 secure storage 기반 구현으로 교체한다.
/// 테스트에서는 이 Provider를 override해서 mock 클라이언트를 주입한다.
final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(readAccessToken: null);
});
