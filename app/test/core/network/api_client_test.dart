import 'dart:typed_data';

import 'package:app/core/network/api_client.dart';
import 'package:app/core/network/app_exception.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

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

ApiClient clientWith(FakeAdapter adapter, {AccessTokenReader? tokenReader}) {
  final dio = Dio(ApiClient.defaultOptions)..httpClientAdapter = adapter;
  return ApiClient(dio: dio, readAccessToken: tokenReader);
}

void main() {
  group('응답 래퍼 언래핑 (api-spec 1.3)', () {
    test('2xx {data} 래퍼를 벗겨 data 값만 돌려준다', () async {
      final adapter =
          FakeAdapter((_) => jsonBody('{"data": {"id": 1, "name": "김치만두 1kg"}}', 200));

      final data = await clientWith(adapter).get('/products/1');

      expect(data, {'id': 1, 'name': '김치만두 1kg'});
    });

    test('204 No Content는 null을 돌려준다', () async {
      final adapter = FakeAdapter((_) => jsonBody('', 204));

      final data = await clientWith(adapter).delete('/products/1');

      expect(data, isNull);
    });

    test('data 래퍼가 없는 2xx는 계약 위반으로 예외를 던진다', () async {
      final adapter = FakeAdapter((_) => jsonBody('{"id": 1}', 200));

      expect(
        () => clientWith(adapter).get('/products/1'),
        throwsA(isA<ApiException>()
            .having((e) => e.code, 'code', 'MALFORMED_RESPONSE')),
      );
    });
  });

  group('에러 응답 매핑 (api-spec 1.4)', () {
    test('400 VALIDATION_ERROR — code·message·details를 매핑한다', () async {
      final adapter = FakeAdapter((_) => jsonBody('''
        {"error": {"code": "VALIDATION_ERROR", "message": "입력값 검증에 실패했습니다.",
          "details": [{"field": "name", "reason": "필수 입력입니다."}]}}
      ''', 400));

      await expectLater(
        () => clientWith(adapter).post('/orders', body: {}),
        throwsA(isA<ApiException>()
            .having((e) => e.statusCode, 'statusCode', 400)
            .having((e) => e.code, 'code', 'VALIDATION_ERROR')
            .having((e) => e.details, 'details', hasLength(1))
            .having((e) => e.details.first.field, 'details.field', 'name')),
      );
    });

    test('401 TOKEN_EXPIRED — isTokenExpired로 판별된다', () async {
      final adapter = FakeAdapter((_) =>
          jsonBody('{"error": {"code": "TOKEN_EXPIRED", "message": "만료"}}', 401));

      await expectLater(
        () => clientWith(adapter).get('/me'),
        throwsA(isA<ApiException>()
            .having((e) => e.isTokenExpired, 'isTokenExpired', true)),
      );
    });

    test('error 래퍼 없는 실패 응답(게이트웨이 오류 등)도 ApiException으로 감싼다',
        () async {
      final adapter = FakeAdapter((_) => ResponseBody.fromString(
            'bad gateway',
            502,
            headers: {
              Headers.contentTypeHeader: ['text/html'],
            },
          ));

      await expectLater(
        () => clientWith(adapter).get('/products'),
        throwsA(isA<ApiException>()
            .having((e) => e.statusCode, 'statusCode', 502)
            .having((e) => e.code, 'code', 'UNKNOWN')),
      );
    });

    test('연결 실패는 NetworkException으로 매핑한다', () async {
      final adapter = FakeAdapter((options) => throw DioException.connectionError(
          requestOptions: options, reason: 'refused'));

      await expectLater(
        () => clientWith(adapter).get('/products'),
        throwsA(isA<NetworkException>()),
      );
    });
  });

  group('Authorization 헤더 (api-spec 1.2)', () {
    test('토큰이 있으면 Bearer 헤더를 붙인다', () async {
      final adapter = FakeAdapter((_) => jsonBody('{"data": {}}', 200));

      await clientWith(adapter, tokenReader: () async => 'token-123').get('/me');

      expect(adapter.lastRequest?.headers['Authorization'], 'Bearer token-123');
    });

    test('토큰이 없으면 헤더를 붙이지 않는다 (익명 API)', () async {
      final adapter = FakeAdapter((_) => jsonBody('{"data": {}}', 200));

      await clientWith(adapter).post('/auth/login', body: {});

      expect(adapter.lastRequest?.headers.containsKey('Authorization'), false);
    });
  });
}
