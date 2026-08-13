import 'package:app/app.dart';
import 'package:app/core/storage/key_value_store.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'helpers/fake_backend.dart';

/// 앱 셸 뼈대 — 하단 탭 4개와 탭 전환.
///
/// 로그인 흐름 자체는 `features/auth/auth_flow_test.dart`에서 본다. 여기서는
/// 저장된 세션으로 바로 들어가서(자동 로그인) 셸만 확인한다.
void main() {
  Future<void> pumpLoggedInApp(WidgetTester tester) async {
    final storage = InMemoryKeyValueStore({
      'auth.session': storedSessionJson(),
    });

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          FakeBackend().override,
          keyValueStoreProvider.overrideWithValue(storage),
        ],
        child: const OrderFlowApp(),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('뼈대: 발주 탭(카탈로그)에서 시작하고 하단 탭 4개가 보인다', (tester) async {
    await pumpLoggedInApp(tester);

    expect(find.text('상품 카탈로그'), findsOneWidget);
    for (final label in ['홈', '발주', '내역', '더보기']) {
      expect(find.text(label), findsOneWidget, reason: '$label 탭이 보여야 한다');
    }
  });

  testWidgets('하단 탭: 탭을 누르면 해당 화면으로 전환된다', (tester) async {
    await pumpLoggedInApp(tester);

    await tester.tap(find.text('내역'));
    await tester.pumpAndSettle();
    expect(find.text('발주 내역'), findsOneWidget);

    await tester.tap(find.text('홈'));
    await tester.pumpAndSettle();
    expect(find.text('발주 내역'), findsNothing);
  });
}
