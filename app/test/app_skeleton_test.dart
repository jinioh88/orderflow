import 'package:app/app.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('뼈대: 로그인 화면에서 시작해 카탈로그로 라우팅된다', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: OrderFlowApp()));

    expect(find.text('OrderFlow'), findsOneWidget);
    expect(find.text('이메일'), findsOneWidget);

    await tester.tap(find.text('로그인'));
    await tester.pumpAndSettle();

    expect(find.text('상품 카탈로그'), findsOneWidget);
  });
}
