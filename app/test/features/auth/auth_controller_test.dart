import 'package:app/core/storage/key_value_store.dart';
import 'package:app/features/auth/application/auth_controller.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_backend.dart';

/// 세션 수명 주기 — 복원 · 로그인 · 재발급 · 비밀번호 설정 · 로그아웃.
void main() {
  late FakeBackend backend;
  late InMemoryKeyValueStore storage;

  setUp(() {
    backend = FakeBackend();
    storage = InMemoryKeyValueStore();
  });

  ProviderContainer containerWith({KeyValueStore? store}) {
    return ProviderContainer.test(
      overrides: [
        backend.override,
        keyValueStoreProvider.overrideWithValue(store ?? storage),
      ],
    );
  }

  Map<String, dynamic> loginResponse({bool passwordSetupRequired = false}) =>
      loginResponseJson(passwordSetupRequired: passwordSetupRequired);

  /// 저장소에 이미 세션이 있는 상태로 만든다 (앱 재시작 시나리오).
  void seedStoredSession({bool passwordSetupRequired = false}) {
    storage.values['auth.session'] = storedSessionJson(
      passwordSetupRequired: passwordSetupRequired,
    );
  }

  group('세션 복원 (자동 로그인)', () {
    test('저장된 세션이 없으면 미인증', () async {
      final container = containerWith();

      expect(
        container.read(authControllerProvider).status,
        AuthStatus.initializing,
      );
      await container.read(authControllerProvider.notifier).initialized;

      expect(
        container.read(authControllerProvider).status,
        AuthStatus.unauthenticated,
      );
    });

    test('저장된 세션이 있으면 로그인 상태로 복원한다', () async {
      seedStoredSession();
      final container = containerWith();

      await container.read(authControllerProvider.notifier).initialized;

      final state = container.read(authControllerProvider);
      expect(state.status, AuthStatus.authenticated);
      expect(state.accessToken, 'stored-access');
      expect(state.user?.name, '박점주');
    });

    test('임시 비밀번호 상태로 저장돼 있으면 비밀번호 설정 상태로 복원한다', () async {
      seedStoredSession(passwordSetupRequired: true);
      final container = containerWith();

      await container.read(authControllerProvider.notifier).initialized;

      expect(
        container.read(authControllerProvider).status,
        AuthStatus.passwordSetupRequired,
      );
    });

    test('저장소 읽기가 실패해도 부팅이 멈추지 않고 미인증으로 시작한다', () async {
      // Keystore 손상·백업 복원 실패 등으로 secure storage가 예외를 던지는 기기.
      final container = containerWith(store: FailingKeyValueStore());

      await container.read(authControllerProvider.notifier).initialized;

      expect(
        container.read(authControllerProvider).status,
        AuthStatus.unauthenticated,
        reason: 'initializing에 갇히면 스플래시에서 영영 못 나온다',
      );
    });

    test('저장 값이 깨져 있으면 지우고 미인증으로 시작한다', () async {
      storage.values['auth.session'] = '{깨진 JSON';
      final container = containerWith();

      await container.read(authControllerProvider.notifier).initialized;

      expect(
        container.read(authControllerProvider).status,
        AuthStatus.unauthenticated,
      );
      expect(storage.values, isEmpty);
    });
  });

  group('로그인', () {
    test('성공하면 세션을 저장하고 인증 상태가 된다', () async {
      backend.stub('/auth/login', () => okBody(loginResponse()));
      final container = containerWith();
      await container.read(authControllerProvider.notifier).initialized;

      await container
          .read(authControllerProvider.notifier)
          .login(email: 'owner@example.com', password: 'pw1234');

      expect(
        container.read(authControllerProvider).status,
        AuthStatus.authenticated,
      );
      expect(storage.values['auth.session'], contains('refresh-1'));
    });

    test('임시 비밀번호 상태면 비밀번호 설정 상태가 된다 (2.3)', () async {
      backend.stub(
        '/auth/login',
        () => okBody(loginResponse(passwordSetupRequired: true)),
      );
      final container = containerWith();
      await container.read(authControllerProvider.notifier).initialized;

      await container
          .read(authControllerProvider.notifier)
          .login(email: 'owner@example.com', password: 'temp1234');

      expect(
        container.read(authControllerProvider).status,
        AuthStatus.passwordSetupRequired,
      );
    });

    test('세션 저장에 실패해도 이번 실행에서는 로그인 상태로 진입한다', () async {
      backend.stub('/auth/login', () => okBody(loginResponse()));
      final container = containerWith(
        store: FailingKeyValueStore(failRead: false),
      );
      await container.read(authControllerProvider.notifier).initialized;

      await container
          .read(authControllerProvider.notifier)
          .login(email: 'owner@example.com', password: 'pw1234');

      expect(
        container.read(authControllerProvider).status,
        AuthStatus.authenticated,
        reason: '서버 인증은 이미 끝났다 — 저장 실패는 자동 로그인만 포기하면 된다',
      );
    });

    test('실패하면 예외가 올라오고 세션은 저장되지 않는다', () async {
      backend.stub('/auth/login', () => errorBody('INVALID_CREDENTIALS', 401));
      final container = containerWith();
      await container.read(authControllerProvider.notifier).initialized;

      await expectLater(
        container
            .read(authControllerProvider.notifier)
            .login(email: 'a@b.com', password: 'x'),
        throwsA(anything),
      );
      expect(
        container.read(authControllerProvider).status,
        AuthStatus.unauthenticated,
      );
      expect(storage.values, isEmpty);
    });
  });

  group('토큰 재발급 (2.4.3)', () {
    test('성공하면 회전된 토큰 쌍으로 교체하고 저장한다', () async {
      seedStoredSession();
      backend.stub(
        '/auth/refresh',
        () => okBody({'accessToken': 'access-2', 'refreshToken': 'refresh-2'}),
      );
      final container = containerWith();
      await container.read(authControllerProvider.notifier).initialized;

      final token = await container
          .read(authControllerProvider.notifier)
          .refreshAccessToken();

      expect(token, 'access-2');
      expect(container.read(authControllerProvider).accessToken, 'access-2');
      expect(storage.values['auth.session'], contains('refresh-2'));
    });

    test('리프레시 토큰이 무효하면 세션을 정리한다 (재로그인 유도)', () async {
      seedStoredSession();
      backend.stub(
        '/auth/refresh',
        () => errorBody('INVALID_REFRESH_TOKEN', 401),
      );
      final container = containerWith();
      await container.read(authControllerProvider.notifier).initialized;

      final token = await container
          .read(authControllerProvider.notifier)
          .refreshAccessToken();

      expect(token, isNull);
      expect(
        container.read(authControllerProvider).status,
        AuthStatus.unauthenticated,
      );
      expect(storage.values, isEmpty);
    });

    test('계정 비활성 등 4xx 응답에도 세션을 정리한다', () async {
      seedStoredSession();
      backend.stub('/auth/refresh', () => errorBody('ACCOUNT_INACTIVE', 403));
      final container = containerWith();
      await container.read(authControllerProvider.notifier).initialized;

      final token = await container
          .read(authControllerProvider.notifier)
          .refreshAccessToken();

      expect(token, isNull);
      expect(
        container.read(authControllerProvider).status,
        AuthStatus.unauthenticated,
        reason: '재발급이 거절된 세션은 어느 화면에서도 쓸 수 없다',
      );
    });

    test('서버 오류(5xx)로는 세션을 지우지 않는다', () async {
      seedStoredSession();
      backend.stub('/auth/refresh', () => errorBody('INTERNAL_ERROR', 500));
      final container = containerWith();
      await container.read(authControllerProvider.notifier).initialized;

      final token = await container
          .read(authControllerProvider.notifier)
          .refreshAccessToken();

      expect(token, isNull);
      expect(
        container.read(authControllerProvider).status,
        AuthStatus.authenticated,
      );
    });

    test('응답 형식이 스펙과 달라도 예외를 던지지 않는다 (인터셉터 안에서 호출된다)', () async {
      seedStoredSession();
      // refreshToken 누락 — 계약 위반 응답.
      backend.stub('/auth/refresh', () => okBody({'accessToken': 'a'}));
      final container = containerWith();
      await container.read(authControllerProvider.notifier).initialized;

      final token = await container
          .read(authControllerProvider.notifier)
          .refreshAccessToken();

      expect(token, isNull);
      expect(
        container.read(authControllerProvider).status,
        AuthStatus.authenticated,
      );
    });

    test('네트워크 실패로는 세션을 지우지 않는다', () async {
      seedStoredSession();
      backend.on(
        '/auth/refresh',
        (options, _) => throw DioException.connectionError(
          requestOptions: options,
          reason: 'offline',
        ),
      );
      final container = containerWith();
      await container.read(authControllerProvider.notifier).initialized;

      final token = await container
          .read(authControllerProvider.notifier)
          .refreshAccessToken();

      expect(token, isNull);
      expect(
        container.read(authControllerProvider).status,
        AuthStatus.authenticated,
        reason: '연결이 끊겼을 뿐 세션은 유효할 수 있다',
      );
      expect(storage.values, isNotEmpty);
    });
  });

  test('비밀번호 설정에 성공하면 임시 상태가 풀리고 새 토큰으로 교체된다 (2.4.4)', () async {
    seedStoredSession(passwordSetupRequired: true);
    backend.stub(
      '/users/me/password',
      () => okBody({
        'accessToken': 'access-9',
        'refreshToken': 'refresh-9',
        'passwordSetupRequired': false,
      }),
    );
    final container = containerWith();
    await container.read(authControllerProvider.notifier).initialized;

    await container
        .read(authControllerProvider.notifier)
        .completePasswordSetup(
          currentPassword: 'temp1234',
          newPassword: 'myNewPass1',
        );

    final state = container.read(authControllerProvider);
    expect(state.status, AuthStatus.authenticated);
    expect(state.accessToken, 'access-9');
    expect(storage.values['auth.session'], contains('refresh-9'));
    // 임시 상태에서도 Authorization 헤더는 붙어야 한다 (2.4.4는 인증 필요).
    expect(
      backend.requestsTo('/users/me/password').single.headers['Authorization'],
      'Bearer stored-access',
    );
  });

  test('403 PASSWORD_SETUP_REQUIRED를 받으면 임시 상태로 전환한다 (2.3)', () async {
    // 본사가 임시 비밀번호를 재발급한 뒤(2.4.10) 업무 API가 거절되는 상황.
    seedStoredSession();
    final container = containerWith();
    await container.read(authControllerProvider.notifier).initialized;

    await container
        .read(authControllerProvider.notifier)
        .markPasswordSetupRequired();

    expect(
      container.read(authControllerProvider).status,
      AuthStatus.passwordSetupRequired,
    );
    expect(
      storage.values['auth.session'],
      contains('"passwordSetupRequired":true'),
    );
  });

  group('로그아웃 (2.4.5)', () {
    test('서버 무효화 후 로컬 세션을 지운다', () async {
      seedStoredSession();
      backend.stub('/auth/logout', () => jsonBody('', 204));
      final container = containerWith();
      await container.read(authControllerProvider.notifier).initialized;

      await container.read(authControllerProvider.notifier).logout();

      expect(
        container.read(authControllerProvider).status,
        AuthStatus.unauthenticated,
      );
      expect(storage.values, isEmpty);
      expect(backend.callsTo('/auth/logout'), 1);
    });

    test('로그아웃 중 토큰이 회전되면 회전된 토큰까지 무효화한다', () async {
      // 액세스 토큰이 만료된 채로 로그아웃한 상황: 첫 요청이 401 → 재발급(회전) → 재시도.
      seedStoredSession();
      backend.stub(
        '/auth/refresh',
        () => okBody({'accessToken': 'access-2', 'refreshToken': 'refresh-2'}),
      );
      backend.on(
        '/auth/logout',
        (options, calls) =>
            calls == 1 ? errorBody('TOKEN_EXPIRED', 401) : jsonBody('', 204),
      );
      final container = containerWith();
      await container.read(authControllerProvider.notifier).initialized;

      await container.read(authControllerProvider.notifier).logout();

      final revoked = backend
          .requestsTo('/auth/logout')
          .map((r) => (r.data as Map<String, dynamic>)['refreshToken'])
          .toSet();
      expect(
        revoked,
        containsAll(<String>['stored-refresh', 'refresh-2']),
        reason: '회전으로 새로 발급된 refresh-2가 서버에 살아남으면 안 된다',
      );
      expect(
        container.read(authControllerProvider).status,
        AuthStatus.unauthenticated,
      );
    });

    test('저장소 삭제가 실패해도 미인증 상태로 전환된다', () async {
      backend.stub('/auth/logout', () => jsonBody('', 204));
      final store = FailingKeyValueStore(failRead: false, failWrite: false);
      final container = containerWith(store: store);
      await container.read(authControllerProvider.notifier).initialized;
      backend.stub('/auth/login', () => okBody(loginResponse()));
      await container
          .read(authControllerProvider.notifier)
          .login(email: 'owner@example.com', password: 'pw');

      await container.read(authControllerProvider.notifier).logout();

      expect(
        container.read(authControllerProvider).status,
        AuthStatus.unauthenticated,
      );
    });

    test('서버 호출이 실패해도 로컬 세션은 지운다', () async {
      seedStoredSession();
      backend.on(
        '/auth/logout',
        (options, _) => throw DioException.connectionError(
          requestOptions: options,
          reason: 'offline',
        ),
      );
      final container = containerWith();
      await container.read(authControllerProvider.notifier).initialized;

      await container.read(authControllerProvider.notifier).logout();

      expect(
        container.read(authControllerProvider).status,
        AuthStatus.unauthenticated,
      );
      expect(storage.values, isEmpty);
    });
  });
}
