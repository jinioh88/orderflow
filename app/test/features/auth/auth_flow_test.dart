import 'package:app/app.dart';
import 'package:app/core/storage/key_value_store.dart';
import 'package:app/core/widgets/app_button.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_backend.dart';

/// 로그인 → 화면 전환까지의 흐름 (US-AUTH-03, api-spec 2.4.2·2.4.4).
///
/// 화면 이동은 라우터 가드가 인증 상태만 보고 결정하므로, 여기서 검증하는 것은
/// "상태가 바뀌면 올바른 화면으로 간다"는 계약이다.
void main() {
  late FakeBackend backend;
  late InMemoryKeyValueStore storage;

  setUp(() {
    backend = FakeBackend();
    storage = InMemoryKeyValueStore();
  });

  Map<String, dynamic> loginResponse({bool passwordSetupRequired = false}) =>
      loginResponseJson(passwordSetupRequired: passwordSetupRequired);

  Future<void> pumpApp(WidgetTester tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          backend.override,
          keyValueStoreProvider.overrideWithValue(storage),
        ],
        child: const OrderFlowApp(),
      ),
    );
    await tester.pumpAndSettle();
  }

  Future<void> enterCredentials(
    WidgetTester tester, {
    String email = 'owner@example.com',
    String password = 'pw1234',
  }) async {
    await tester.enterText(find.byType(TextFormField).first, email);
    await tester.enterText(find.byType(TextFormField).last, password);
  }

  testWidgets('저장된 세션이 없으면 로그인 화면에서 시작한다', (tester) async {
    await pumpApp(tester);

    expect(find.text('OrderFlow'), findsOneWidget);
    expect(find.text('이메일'), findsOneWidget);
  });

  testWidgets('로그인에 성공하면 발주 탭(카탈로그)으로 들어간다', (tester) async {
    backend.stub('/auth/login', () => okBody(loginResponse()));
    await pumpApp(tester);

    await enterCredentials(tester);
    await tester.tap(find.text('로그인'));
    await tester.pumpAndSettle();

    expect(find.text('상품 카탈로그'), findsOneWidget);
    for (final label in ['홈', '발주', '내역', '더보기']) {
      expect(find.text(label), findsOneWidget, reason: '$label 탭이 보여야 한다');
    }
    // 세션이 저장돼야 다음 기동에서 자동 로그인된다.
    expect(storage.values['auth.session'], contains('refresh-1'));
  });

  testWidgets('자격 증명이 틀리면 화면에 남고 폼 상단에 사유를 보여준다', (tester) async {
    backend.stub('/auth/login', () => errorBody('INVALID_CREDENTIALS', 401));
    await pumpApp(tester);

    await enterCredentials(tester, password: 'wrong');
    await tester.tap(find.text('로그인'));
    await tester.pumpAndSettle();

    expect(find.text('이메일 또는 비밀번호가 올바르지 않습니다'), findsOneWidget);
    expect(find.text('상품 카탈로그'), findsNothing);
  });

  testWidgets('계정이 중지되면(403) 그 사유를 보여준다', (tester) async {
    backend.stub('/auth/login', () => errorBody('ACCOUNT_INACTIVE', 403));
    await pumpApp(tester);

    await enterCredentials(tester);
    await tester.tap(find.text('로그인'));
    await tester.pumpAndSettle();

    expect(find.text('사용이 중지된 계정입니다. 본사에 문의하세요'), findsOneWidget);
  });

  testWidgets('입력 검증에 실패하면 요청을 보내지 않는다', (tester) async {
    await pumpApp(tester);

    await tester.enterText(find.byType(TextFormField).first, '이메일아님');
    await tester.tap(find.text('로그인'));
    await tester.pumpAndSettle();

    expect(find.textContaining('이메일 형식이 아닙니다'), findsOneWidget);
    expect(find.text('비밀번호를 입력하세요'), findsOneWidget);
    expect(backend.callsTo('/auth/login'), 0);
  });

  testWidgets('저장된 세션이 있으면 로그인 화면 없이 바로 들어간다 (자동 로그인)', (tester) async {
    storage.values['auth.session'] = storedSessionJson();

    await pumpApp(tester);

    expect(find.text('상품 카탈로그'), findsOneWidget);
    expect(find.text('이메일'), findsNothing);
  });

  group('최초 로그인 비밀번호 설정 (US-AUTH-02·03)', () {
    setUp(() {
      backend.stub(
        '/auth/login',
        () => okBody(loginResponse(passwordSetupRequired: true)),
      );
    });

    Future<void> loginAsTemporaryUser(WidgetTester tester) async {
      await pumpApp(tester);
      await enterCredentials(tester, password: 'temp1234');
      await tester.tap(find.text('로그인'));
      await tester.pumpAndSettle();
    }

    testWidgets('임시 비밀번호 상태면 비밀번호 설정 화면으로 보낸다', (tester) async {
      await loginAsTemporaryUser(tester);

      expect(find.text('박점주 님, 비밀번호를 설정하세요'), findsOneWidget);
      expect(find.text('상품 카탈로그'), findsNothing);
    });

    testWidgets('뒤로 가기로 빠져나갈 수 없다', (tester) async {
      await loginAsTemporaryUser(tester);

      // 시스템 뒤로 가기(안드로이드 백 버튼)를 흉내낸다.
      await tester.binding.handlePopRoute();
      await tester.pumpAndSettle();

      expect(find.text('박점주 님, 비밀번호를 설정하세요'), findsOneWidget);
    });

    testWidgets('정책 위반 비밀번호는 제출 전에 막는다 (2.3)', (tester) async {
      await loginAsTemporaryUser(tester);

      await tester.enterText(find.byType(TextFormField).at(0), 'temp1234');
      await tester.enterText(find.byType(TextFormField).at(1), 'short1');
      await tester.enterText(find.byType(TextFormField).at(2), 'short1');
      await tester.tap(find.widgetWithText(AppButton, '비밀번호 설정'));
      await tester.pumpAndSettle();

      expect(find.text('비밀번호는 8~64자여야 합니다'), findsOneWidget);
      expect(backend.callsTo('/users/me/password'), 0);
    });

    testWidgets('확인란이 다르면 막는다', (tester) async {
      await loginAsTemporaryUser(tester);

      await tester.enterText(find.byType(TextFormField).at(0), 'temp1234');
      await tester.enterText(find.byType(TextFormField).at(1), 'myNewPass1');
      await tester.enterText(find.byType(TextFormField).at(2), 'myNewPass2');
      await tester.tap(find.widgetWithText(AppButton, '비밀번호 설정'));
      await tester.pumpAndSettle();

      expect(find.text('새 비밀번호와 일치하지 않습니다'), findsOneWidget);
      expect(backend.callsTo('/users/me/password'), 0);
    });

    testWidgets('임시 비밀번호가 틀리면 이 화면에 맞는 문구를 보여준다 (2.4.4)', (tester) async {
      // 같은 401 INVALID_CREDENTIALS라도 이 화면에는 이메일 입력란이 없다.
      backend.stub(
        '/users/me/password',
        () => errorBody('INVALID_CREDENTIALS', 401),
      );
      await loginAsTemporaryUser(tester);

      await tester.enterText(find.byType(TextFormField).at(0), 'wrongTemp1');
      await tester.enterText(find.byType(TextFormField).at(1), 'myNewPass1');
      await tester.enterText(find.byType(TextFormField).at(2), 'myNewPass1');
      await tester.tap(find.widgetWithText(AppButton, '비밀번호 설정'));
      await tester.pumpAndSettle();

      expect(find.textContaining('임시 비밀번호가 올바르지 않습니다'), findsOneWidget);
      expect(find.textContaining('이메일 또는 비밀번호'), findsNothing);
    });

    testWidgets('설정을 마치면 임시 상태가 풀리고 카탈로그로 들어간다', (tester) async {
      backend.stub(
        '/users/me/password',
        () => okBody({
          'accessToken': 'access-9',
          'refreshToken': 'refresh-9',
          'passwordSetupRequired': false,
        }),
      );
      await loginAsTemporaryUser(tester);

      await tester.enterText(find.byType(TextFormField).at(0), 'temp1234');
      await tester.enterText(find.byType(TextFormField).at(1), 'myNewPass1');
      await tester.enterText(find.byType(TextFormField).at(2), 'myNewPass1');
      await tester.tap(find.widgetWithText(AppButton, '비밀번호 설정'));
      await tester.pumpAndSettle();

      expect(find.text('상품 카탈로그'), findsOneWidget);
      expect(storage.values['auth.session'], contains('refresh-9'));
    });
  });

  testWidgets('더보기 탭에서 로그아웃하면 로그인 화면으로 돌아간다', (tester) async {
    backend.stub('/auth/login', () => okBody(loginResponse()));
    backend.stub('/auth/logout', () => jsonBody('', 204));
    await pumpApp(tester);
    await enterCredentials(tester);
    await tester.tap(find.text('로그인'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('더보기'));
    await tester.pumpAndSettle();
    expect(find.text('owner@example.com'), findsOneWidget);

    await tester.tap(find.text('로그아웃'));
    await tester.pumpAndSettle();

    expect(find.text('이메일'), findsOneWidget);
    expect(storage.values, isEmpty);
  });
}
