import 'dart:convert';

import 'package:app/core/network/api_client.dart';
import 'package:app/core/network/app_exception.dart';
import 'package:app/features/auth/data/auth_models.dart';
import 'package:app/features/auth/data/auth_repository.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_backend.dart';

/// AUTH API 요청·응답 정합 (api-spec 2.4).
void main() {
  late FakeBackend backend;
  late AuthRepository repository;

  setUp(() {
    backend = FakeBackend();
    ApiClient build() => ApiClient(
      dio: Dio(ApiClient.defaultOptions)..httpClientAdapter = backend.adapter,
    );
    repository = AuthRepository(anonymous: build(), authenticated: build());
  });

  Map<String, dynamic> bodyOf(RequestOptions options) =>
      jsonDecode(jsonEncode(options.data)) as Map<String, dynamic>;

  test('로그인 응답을 세션으로 파싱한다 (2.4.2)', () async {
    backend.stub(
      '/auth/login',
      () => okBody({
        'accessToken': 'access-1',
        'accessTokenExpiresIn': 1800,
        'refreshToken': 'refresh-1',
        'passwordSetupRequired': false,
        'user': {
          'id': 42,
          'email': 'owner@example.com',
          'name': '박점주',
          'role': 'STORE_OWNER',
          'tenantId': 1,
          'storeId': 7,
        },
      }),
    );

    final session = await repository.login(
      email: 'owner@example.com',
      password: 'pw1234',
    );

    expect(session.accessToken, 'access-1');
    expect(session.refreshToken, 'refresh-1');
    expect(session.passwordSetupRequired, false);
    expect(session.user.name, '박점주');
    expect(session.user.role, UserRole.storeOwner);
    expect(session.user.storeId, 7);
    expect(bodyOf(backend.requestsTo('/auth/login').single), {
      'email': 'owner@example.com',
      'password': 'pw1234',
    });
  });

  test('SYSTEM 계정처럼 tenantId·storeId가 null이어도 파싱된다 (2.4.2)', () async {
    backend.stub(
      '/auth/login',
      () => okBody({
        'accessToken': 'a',
        'refreshToken': 'r',
        'passwordSetupRequired': true,
        'user': {
          'id': 1,
          'email': 'sys@orderflow.io',
          'name': '시스템',
          'role': 'SYSTEM',
          'tenantId': null,
          'storeId': null,
        },
      }),
    );

    final session = await repository.login(email: 'sys', password: 'pw');

    expect(session.user.tenantId, isNull);
    expect(session.user.storeId, isNull);
    expect(session.passwordSetupRequired, true);
  });

  test('로그인 실패는 code를 담은 ApiException으로 올라온다 (2.5)', () async {
    backend.stub('/auth/login', () => errorBody('INVALID_CREDENTIALS', 401));

    await expectLater(
      () => repository.login(email: 'a@b.com', password: 'x'),
      throwsA(
        isA<ApiException>().having(
          (e) => e.code,
          'code',
          'INVALID_CREDENTIALS',
        ),
      ),
    );
  });

  test('재발급은 회전된 새 토큰 쌍을 돌려준다 (2.4.3)', () async {
    backend.stub(
      '/auth/refresh',
      () => okBody({
        'accessToken': 'access-2',
        'accessTokenExpiresIn': 1800,
        'refreshToken': 'refresh-2',
      }),
    );

    final tokens = await repository.refresh('refresh-1');

    expect(tokens.accessToken, 'access-2');
    expect(tokens.refreshToken, 'refresh-2');
    expect(bodyOf(backend.requestsTo('/auth/refresh').single), {
      'refreshToken': 'refresh-1',
    });
  });

  test('비밀번호 설정은 PUT으로 나가고 새 토큰 쌍을 받는다 (2.4.4)', () async {
    backend.stub(
      '/users/me/password',
      () => okBody({
        'accessToken': 'access-3',
        'refreshToken': 'refresh-3',
        'passwordSetupRequired': false,
      }),
    );

    final tokens = await repository.changePassword(
      currentPassword: 'temp1234',
      newPassword: 'myNewPass1',
    );

    final request = backend.requestsTo('/users/me/password').single;
    expect(request.method, 'PUT');
    expect(bodyOf(request), {
      'currentPassword': 'temp1234',
      'newPassword': 'myNewPass1',
    });
    expect(tokens.accessToken, 'access-3');
  });

  test('로그아웃은 204(본문 없음)를 받아도 정상 종료한다 (2.4.5)', () async {
    backend.stub('/auth/logout', () => jsonBody('', 204));

    await repository.logout('refresh-1');

    expect(bodyOf(backend.requestsTo('/auth/logout').single), {
      'refreshToken': 'refresh-1',
    });
  });
}
