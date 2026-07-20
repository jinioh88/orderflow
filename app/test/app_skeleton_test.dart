import 'package:app/app.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('뼈대: 로그인 후 발주 탭으로 이동하고 하단 탭 4개가 보인다', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: OrderFlowApp()));

    expect(find.text('OrderFlow'), findsOneWidget);
    expect(find.text('이메일'), findsOneWidget);

    await tester.enterText(
      find.byType(TextFormField).first,
      'owner@example.com',
    );
    await tester.enterText(find.byType(TextFormField).last, 'pw');
    await tester.tap(find.text('로그인'));
    await tester.pumpAndSettle();

    expect(find.text('상품 카탈로그'), findsOneWidget);
    for (final label in ['홈', '발주', '내역', '더보기']) {
      expect(find.text(label), findsOneWidget, reason: '$label 탭이 보여야 한다');
    }
  });

  testWidgets('로그인: 검증에 실패하면 이동하지 않고 에러 메시지를 보여준다', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: OrderFlowApp()));

    await tester.enterText(find.byType(TextFormField).first, '이메일아님');
    await tester.tap(find.text('로그인'));
    await tester.pumpAndSettle();

    expect(find.textContaining('이메일 형식이 아닙니다'), findsOneWidget);
    expect(find.text('비밀번호를 입력하세요'), findsOneWidget);
    expect(find.text('상품 카탈로그'), findsNothing);
  });

  testWidgets('하단 탭: 탭을 누르면 해당 화면으로 전환된다', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: OrderFlowApp()));

    await tester.enterText(
      find.byType(TextFormField).first,
      'owner@example.com',
    );
    await tester.enterText(find.byType(TextFormField).last, 'pw');
    await tester.tap(find.text('로그인'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('내역'));
    await tester.pumpAndSettle();

    expect(find.text('발주 내역'), findsOneWidget);
  });
}
