import 'package:app/core/network/api_client.dart';
import 'package:app/core/network/app_exception.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_backend.dart';

/// `401 TOKEN_EXPIRED` → 재발급 → **원 요청 재시도** (api-spec 2.5).
void main() {
  /// 액세스 토큰을 들고 있는 가짜 세션 — 재발급 콜백이 토큰을 갈아끼운다.
  late String token;
  late int refreshCalls;
  late FakeBackend backend;

  setUp(() {
    token = 'old-token';
    refreshCalls = 0;
    backend = FakeBackend();
  });

  ApiClient clientThatRefreshes({Future<String?> Function()? refresher}) {
    final dio = Dio(ApiClient.defaultOptions)
      ..httpClientAdapter = backend.adapter;
    return ApiClient(
      dio: dio,
      readAccessToken: () async => token,
      refreshAccessToken:
          refresher ??
          () async {
            refreshCalls++;
            token = 'new-token';
            return token;
          },
    );
  }

  /// 만료된 토큰으로 온 요청은 401, 새 토큰이면 200.
  void stubProductsExpiringOnce() {
    backend.on('/products', (options, _) {
      final auth = options.headers['Authorization'];
      return auth == 'Bearer new-token'
          ? okBody({'items': <dynamic>[]})
          : errorBody('TOKEN_EXPIRED', 401);
    });
  }

  test('TOKEN_EXPIRED를 받으면 재발급 후 원 요청을 재시도한다', () async {
    stubProductsExpiringOnce();

    final data = await clientThatRefreshes().get('/products');

    expect(data, {'items': <dynamic>[]});
    expect(refreshCalls, 1);
    expect(backend.callsTo('/products'), 2, reason: '최초 요청 + 재시도');
    expect(
      backend.requestsTo('/products').last.headers['Authorization'],
      'Bearer new-token',
      reason: '재시도는 새 토큰으로 나가야 한다',
    );
  });

  test('동시에 여러 요청이 만료를 만나도 재발급은 한 번만 나간다 (single-flight)', () async {
    stubProductsExpiringOnce();
    backend.on('/orders', (options, _) {
      return options.headers['Authorization'] == 'Bearer new-token'
          ? okBody({'items': <dynamic>[]})
          : errorBody('TOKEN_EXPIRED', 401);
    });

    // 재발급이 진행되는 동안 두 번째 요청도 401을 만나도록 일부러 늦춘다.
    final client = clientThatRefreshes(
      refresher: () async {
        refreshCalls++;
        await Future<void>.delayed(const Duration(milliseconds: 20));
        token = 'new-token';
        return token;
      },
    );
    await Future.wait([client.get('/products'), client.get('/orders')]);

    // 리프레시 토큰은 회전되므로(2.2) 병렬 재발급은 서로를 무효화한다.
    expect(refreshCalls, 1);
    expect(backend.callsTo('/products'), 2);
    expect(backend.callsTo('/orders'), 2);
  });

  test('다른 요청이 이미 토큰을 갱신했다면 재발급 없이 새 토큰으로 재시도한다', () async {
    stubProductsExpiringOnce();
    // 이 요청이 나간 뒤(만료된 토큰) 다른 요청이 재발급을 끝낸 상황을 만든다.
    backend.on('/products', (options, calls) {
      if (calls == 1) {
        token = 'new-token'; // 다른 요청이 방금 재발급을 끝냄
        return errorBody('TOKEN_EXPIRED', 401);
      }
      return okBody({'items': <dynamic>[]});
    });

    await clientThatRefreshes().get('/products');

    expect(refreshCalls, 0, reason: '이미 갱신된 토큰이 있으면 재발급하지 않는다');
    expect(backend.callsTo('/products'), 2);
  });

  test('재발급이 실패하면 원래의 401을 그대로 올려보낸다', () async {
    backend.stub('/products', () => errorBody('TOKEN_EXPIRED', 401));

    final client = clientThatRefreshes(
      refresher: () async {
        refreshCalls++;
        return null; // 리프레시 토큰도 무효 — 세션 정리는 컨트롤러가 한다.
      },
    );

    await expectLater(
      () => client.get('/products'),
      throwsA(
        isA<ApiException>().having((e) => e.code, 'code', 'TOKEN_EXPIRED'),
      ),
    );
    expect(refreshCalls, 1);
    expect(backend.callsTo('/products'), 1, reason: '재시도하지 않는다');
  });

  test('재시도한 요청이 또 만료로 응답해도 재발급 루프에 빠지지 않는다', () async {
    // 새 토큰으로도 계속 401을 주는 서버 (비정상 상황).
    backend.stub('/products', () => errorBody('TOKEN_EXPIRED', 401));

    await expectLater(
      () => clientThatRefreshes().get('/products'),
      throwsA(isA<ApiException>()),
    );
    expect(refreshCalls, 1);
    expect(backend.callsTo('/products'), 2, reason: '최초 + 재시도 1회로 끝');
  });

  test('403 PASSWORD_SETUP_REQUIRED는 재시도하지 않고 상태 전환만 알린다 (2.3)', () async {
    backend.stub('/products', () => errorBody('PASSWORD_SETUP_REQUIRED', 403));
    var notified = 0;

    final dio = Dio(ApiClient.defaultOptions)
      ..httpClientAdapter = backend.adapter;
    final client = ApiClient(
      dio: dio,
      readAccessToken: () async => token,
      refreshAccessToken: () async {
        refreshCalls++;
        return 'new-token';
      },
      onPasswordSetupRequired: () async => notified++,
    );

    await expectLater(
      () => client.get('/products'),
      throwsA(
        isA<ApiException>().having(
          (e) => e.code,
          'code',
          'PASSWORD_SETUP_REQUIRED',
        ),
      ),
    );
    expect(notified, 1);
    expect(refreshCalls, 0);
    expect(backend.callsTo('/products'), 1);
  });

  test('401이어도 코드가 TOKEN_EXPIRED가 아니면 재발급하지 않는다', () async {
    backend.stub('/auth/login', () => errorBody('INVALID_CREDENTIALS', 401));

    await expectLater(
      () => clientThatRefreshes().post('/auth/login', body: {}),
      throwsA(
        isA<ApiException>().having(
          (e) => e.code,
          'code',
          'INVALID_CREDENTIALS',
        ),
      ),
    );
    expect(refreshCalls, 0);
  });
}
