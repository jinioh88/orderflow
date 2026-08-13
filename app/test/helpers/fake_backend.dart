import 'dart:convert';
import 'dart:typed_data';

import 'package:app/core/network/api_client.dart';
import 'package:app/core/network/network_providers.dart';
import 'package:app/core/storage/key_value_store.dart';
import 'package:dio/dio.dart';
// Riverpod 3에서 `Override` 타입은 misc.dart로 옮겨졌다.
import 'package:flutter_riverpod/misc.dart';

/// dio 모킹 방식 (Phase 0 결정): 별도 패키지 없이 [HttpClientAdapter]를
/// 직접 구현한 가짜 어댑터를 쓴다. 요청이 실제 네트워크로 나가기 직전 지점을
/// 갈아끼우므로 인터셉터·응답 파싱 등 dio 파이프라인 전체가 테스트를 통과한다.
class FakeAdapter implements HttpClientAdapter {
  FakeAdapter(this.handler);

  final ResponseBody Function(RequestOptions options) handler;
  RequestOptions? lastRequest;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    lastRequest = options;
    return handler(options);
  }

  @override
  void close({bool force = false}) {}
}

ResponseBody jsonBody(String json, int status) => ResponseBody.fromString(
  json,
  status,
  headers: {
    Headers.contentTypeHeader: ['application/json'],
  },
);

/// api-spec 1.3의 성공 래퍼 `{data: ...}`.
ResponseBody okBody(Map<String, dynamic> data, {int status = 200}) =>
    jsonBody(jsonEncode({'data': data}), status);

/// api-spec 1.3의 실패 래퍼 `{error: {code, message}}`.
ResponseBody errorBody(
  String code,
  int status, {
  String message = '오류',
  List<Map<String, String>>? details,
}) => jsonBody(
  jsonEncode({
    'error': {'code': code, 'message': message, 'details': ?details},
  }),
  status,
);

ApiClient clientWith(FakeAdapter adapter, {AccessTokenReader? tokenReader}) {
  final dio = Dio(ApiClient.defaultOptions)..httpClientAdapter = adapter;
  return ApiClient(dio: dio, readAccessToken: tokenReader);
}

/// 경로별 응답을 등록해 두는 가짜 서버.
///
/// 핸들러는 요청과 **그 경로의 호출 횟수**(1부터)를 받는다 — "첫 호출은 401, 재시도는 200"
/// 같은 시나리오를 그대로 쓸 수 있게 하려는 것이다.
class FakeBackend {
  final _handlers = <String, ResponseBody Function(RequestOptions, int)>{};

  /// 실제로 나간 요청들 — 헤더·본문·순서를 검증하는 데 쓴다.
  final List<RequestOptions> requests = [];

  void on(String path, ResponseBody Function(RequestOptions, int) handler) {
    _handlers[path] = handler;
  }

  /// 호출 횟수와 무관하게 늘 같은 응답을 주는 짧은 형태.
  ///
  /// [ResponseBody]는 스트림이라 **한 번만 읽을 수 있다** — 인스턴스가 아니라
  /// 만드는 함수를 받아 호출마다 새로 만든다 (재시도 테스트가 여기에 걸린다).
  void stub(String path, ResponseBody Function() build) =>
      on(path, (_, _) => build());

  int callsTo(String path) => requests.where((r) => r.path == path).length;

  List<RequestOptions> requestsTo(String path) =>
      requests.where((r) => r.path == path).toList();

  /// 어댑터는 **하나만** 만든다 — 게터가 매번 새로 만들면 `lastRequest` 같은
  /// 인스턴스 상태가 읽는 쪽마다 달라져 테스트가 조용히 거짓말을 한다.
  late final FakeAdapter adapter = FakeAdapter((options) {
    requests.add(options);
    final handler = _handlers[options.path];
    if (handler == null) {
      // 스텁하지 않은 경로를 조용히 통과시키면 테스트가 거짓 통과한다.
      return errorBody(
        'NOT_STUBBED',
        599,
        message: '스텁되지 않은 요청: ${options.method} ${options.path}',
      );
    }
    return handler(options, callsTo(options.path));
  });

  /// dio 경계만 이 가짜 서버로 바꾸는 override — 인터셉터 배선은 실제 코드 그대로 쓴다.
  Override get override => dioFactoryProvider.overrideWithValue(
    () => Dio(ApiClient.defaultOptions)..httpClientAdapter = adapter,
  );
}

/// 점주 계정(페르소나 박점주) 픽스처 — 로그인 응답(2.4.2)과 로컬 저장 형식이 같은 모양이다.
Map<String, dynamic> storeOwnerUserJson() => {
  'id': 42,
  'email': 'owner@example.com',
  'name': '박점주',
  'role': 'STORE_OWNER',
  'tenantId': 1,
  'storeId': 7,
};

/// 로그인 응답 본문의 `data` (2.4.2).
Map<String, dynamic> loginResponseJson({
  bool passwordSetupRequired = false,
  String accessToken = 'access-1',
  String refreshToken = 'refresh-1',
}) => {
  'accessToken': accessToken,
  'accessTokenExpiresIn': 1800,
  'refreshToken': refreshToken,
  'passwordSetupRequired': passwordSetupRequired,
  'user': storeOwnerUserJson(),
};

/// secure storage에 저장돼 있는 세션 (자동 로그인 시나리오).
String storedSessionJson({
  String accessToken = 'stored-access',
  String refreshToken = 'stored-refresh',
  bool passwordSetupRequired = false,
}) => jsonEncode({
  'accessToken': accessToken,
  'refreshToken': refreshToken,
  'passwordSetupRequired': passwordSetupRequired,
  'user': storeOwnerUserJson(),
});

/// 저장소가 죽은 기기를 흉내낸다.
///
/// 실제로 `flutter_secure_storage`는 Keystore 손상·기기 백업 복원 후 복호화 실패 등으로
/// `PlatformException`을 던진다. 앱은 그 상황에서도 부팅되고 로그인돼야 한다.
class FailingKeyValueStore implements KeyValueStore {
  FailingKeyValueStore({this.failRead = true, this.failWrite = true});

  final bool failRead;
  final bool failWrite;
  final Map<String, String> values = {};

  @override
  Future<String?> read(String key) async {
    if (failRead) throw StateError('secure storage read 실패');
    return values[key];
  }

  @override
  Future<void> write(String key, String value) async {
    if (failWrite) throw StateError('secure storage write 실패');
    values[key] = value;
  }

  @override
  Future<void> delete(String key) async => values.remove(key);
}

/// secure storage 대체 — 플랫폼 채널이 없는 단위 테스트용.
class InMemoryKeyValueStore implements KeyValueStore {
  InMemoryKeyValueStore([Map<String, String>? initial])
    : values = {...?initial};

  final Map<String, String> values;

  @override
  Future<String?> read(String key) async => values[key];

  @override
  Future<void> write(String key, String value) async => values[key] = value;

  @override
  Future<void> delete(String key) async => values.remove(key);
}
