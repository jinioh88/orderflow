import 'package:app/core/theme/app_theme.dart';
import 'package:app/core/theme/tokens.dart';
import 'package:app/core/widgets/app_button.dart';
import 'package:app/core/widgets/app_text_field.dart';
import 'package:app/core/widgets/order_status_badge.dart';
import 'package:app/core/widgets/quantity_stepper.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

Widget wrap(Widget child) => MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: Center(child: child)),
    );

void main() {
  group('상태 뱃지 — 위젯 1개가 7상태 + 전송 대기를 커버한다 (04 §8)', () {
    for (final status in OrderStatus.values) {
      testWidgets('${status.code}는 토큰 라벨과 배경색으로 그려진다', (tester) async {
        await tester.pumpWidget(wrap(OrderStatusBadge(status)));

        // 색 단독 금지 — 라벨을 항상 동반한다 (01-foundations §1.4).
        expect(find.text(status.label), findsOneWidget);

        final container = tester.widget<Container>(
          find
              .descendant(
                of: find.byType(OrderStatusBadge),
                matching: find.byType(Container),
              )
              .first,
        );
        final decoration = container.decoration! as BoxDecoration;
        expect(decoration.color, status.bg);
      });
    }

    testWidgets('전송 대기는 dot 대신 schedule_send 아이콘으로 구분된다', (tester) async {
      await tester.pumpWidget(wrap(const OrderStatusBadge.pendingSend()));

      expect(find.text('전송 대기'), findsOneWidget);
      expect(find.byIcon(Icons.schedule_send), findsOneWidget);
    });

    testWidgets('lg 변형은 높이 32', (tester) async {
      await tester.pumpWidget(
        wrap(const OrderStatusBadge(OrderStatus.approved, size: BadgeSize.lg)),
      );

      expect(tester.getSize(find.byType(OrderStatusBadge)).height, 32);
    });
  });

  group('버튼 (04 §2)', () {
    testWidgets('CTA는 높이 52, 기본 버튼은 48', (tester) async {
      await tester.pumpWidget(
        wrap(
          Column(
            children: [
              AppButton.cta(label: '발주 제출', onPressed: () {}),
              AppButton.secondary(label: '취소', onPressed: () {}),
            ],
          ),
        ),
      );

      expect(tester.getSize(find.byType(AppButton).first).height,
          AppTouch.ctaHeight);
      expect(
          tester.getSize(find.byType(AppButton).last).height, AppTouch.minTarget);
    });

    testWidgets('로딩 중에는 라벨을 유지하고 폭이 변하지 않으며 눌리지 않는다', (tester) async {
      var taps = 0;

      await tester.pumpWidget(
        wrap(
          SizedBox(
            width: 200,
            child: AppButton.primary(label: '저장', onPressed: () => taps++),
          ),
        ),
      );
      final idleWidth = tester.getSize(find.byType(AppButton)).width;

      await tester.pumpWidget(
        wrap(
          SizedBox(
            width: 200,
            child: AppButton.primary(
              label: '저장',
              onPressed: () => taps++,
              loading: true,
            ),
          ),
        ),
      );

      expect(find.text('저장'), findsOneWidget, reason: '라벨은 유지된다');
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
      expect(tester.getSize(find.byType(AppButton)).width, idleWidth);

      await tester.tap(find.byType(AppButton));
      await tester.pump();
      expect(taps, 0, reason: '로딩 중에는 눌리지 않는다');
    });

    testWidgets('onPressed가 null이면 비활성', (tester) async {
      await tester.pumpWidget(
        wrap(const AppButton.cta(label: '제출', onPressed: null)),
      );

      final inkWell = tester.widget<InkWell>(find.byType(InkWell));
      expect(inkWell.onTap, isNull);
    });
  });

  group('수량 스테퍼 (04 §2)', () {
    testWidgets('버튼 몸체는 44dp — 터치 타깃 최소치를 넘는다', (tester) async {
      await tester.pumpWidget(
        wrap(QuantityStepper(value: 3, onChanged: (_) {})),
      );

      final minusSize = tester.getSize(find.byIcon(Icons.remove).first);
      expect(minusSize.width, greaterThan(0));
      expect(
        tester.getSize(find.byType(QuantityStepper)).height,
        greaterThanOrEqualTo(AppTouch.minTarget),
      );
    });

    testWidgets('+ / − 로 값이 증감한다', (tester) async {
      var value = 3;
      await tester.pumpWidget(
        wrap(QuantityStepper(value: value, onChanged: (v) => value = v)),
      );

      await tester.tap(find.byIcon(Icons.add));
      expect(value, 4);

      await tester.tap(find.byIcon(Icons.remove));
      expect(value, 2, reason: 'value는 위젯 밖 상태라 3에서 다시 감소한다');
    });

    testWidgets('길게 누르면 200ms 간격으로 연속 증가한다', (tester) async {
      var value = 0;
      await tester.pumpWidget(
        wrap(
          StatefulBuilder(
            builder: (context, setState) => QuantityStepper(
              value: value,
              onChanged: (v) => setState(() => value = v),
            ),
          ),
        ),
      );

      final gesture = await tester.startGesture(
        tester.getCenter(find.byIcon(Icons.add)),
      );
      await tester.pump(const Duration(milliseconds: 600)); // 롱프레스 인식
      for (var i = 0; i < 3; i++) {
        await tester.pump(const Duration(milliseconds: 200)); // 연속 증감 1틱씩
      }
      await gesture.up();
      await tester.pumpAndSettle();

      expect(value, greaterThanOrEqualTo(3), reason: '연속 증감이 동작해야 한다');
    });

    testWidgets('최소값에서 −는 onBelowMin으로 빠진다 (장바구니 제거)', (tester) async {
      var removed = false;
      await tester.pumpWidget(
        wrap(
          QuantityStepper(
            value: 1,
            minValue: 1,
            onChanged: (_) {},
            onBelowMin: () => removed = true,
          ),
        ),
      );

      await tester.tap(find.byIcon(Icons.remove));
      expect(removed, isTrue);
    });

    testWidgets('상한(한정 품목 잔여 수량)에 도달하면 +가 비활성', (tester) async {
      var value = 5;
      await tester.pumpWidget(
        wrap(
          QuantityStepper(
            value: 5,
            maxValue: 5,
            onChanged: (v) => value = v,
          ),
        ),
      );

      await tester.tap(find.byIcon(Icons.add));
      expect(value, 5);
    });
  });

  group('입력 필드 (02-patterns §3)', () {
    testWidgets('라벨은 인풋 위에 별도로 그려진다 (플로팅 라벨 금지)', (tester) async {
      await tester.pumpWidget(wrap(const AppTextField(label: '이메일')));

      final labelY = tester.getTopLeft(find.text('이메일')).dy;
      final fieldY = tester.getTopLeft(find.byType(TextFormField)).dy;
      expect(labelY, lessThan(fieldY));
    });

    testWidgets('타이핑 중에는 검증하지 않고, 블러 시 1차 검증한다', (tester) async {
      await tester.pumpWidget(
        wrap(
          Column(
            children: [
              AppTextField(
                label: '이메일',
                validator: (v) =>
                    (v ?? '').contains('@') ? null : '이메일 형식이 아닙니다',
              ),
              const AppTextField(label: '비밀번호'),
            ],
          ),
        ),
      );

      await tester.enterText(find.byType(TextFormField).first, '틀린값');
      await tester.pump();
      expect(find.text('이메일 형식이 아닙니다'), findsNothing,
          reason: '입력이 끝나기 전에 혼내지 않는다');

      // 다른 필드로 포커스를 옮겨 블러 발생.
      await tester.tap(find.byType(TextFormField).last);
      await tester.pumpAndSettle();
      expect(find.text('이메일 형식이 아닙니다'), findsOneWidget);
    });

    testWidgets('에러 상태에서 재입력을 시작하면 즉시 에러가 해제된다', (tester) async {
      await tester.pumpWidget(
        wrap(
          Column(
            children: [
              AppTextField(
                label: '이메일',
                validator: (v) =>
                    (v ?? '').contains('@') ? null : '이메일 형식이 아닙니다',
              ),
              const AppTextField(label: '비밀번호'),
            ],
          ),
        ),
      );

      await tester.enterText(find.byType(TextFormField).first, '틀린값');
      await tester.tap(find.byType(TextFormField).last);
      await tester.pumpAndSettle();
      expect(find.text('이메일 형식이 아닙니다'), findsOneWidget);

      await tester.enterText(find.byType(TextFormField).first, '틀린값2');
      await tester.pump();
      expect(find.text('이메일 형식이 아닙니다'), findsNothing);
    });

    testWidgets('제출 시에는 Form 전체가 검증된다', (tester) async {
      final formKey = GlobalKey<FormState>();

      await tester.pumpWidget(
        wrap(
          Form(
            key: formKey,
            child: AppTextField(
              label: '이메일',
              validator: (v) => (v ?? '').isEmpty ? '이메일을 입력하세요' : null,
            ),
          ),
        ),
      );

      expect(formKey.currentState!.validate(), isFalse);
      await tester.pump();
      expect(find.text('이메일을 입력하세요'), findsOneWidget);
    });

    testWidgets('단위 서픽스는 인풋 안 우측에 고정된다', (tester) async {
      await tester.pumpWidget(
        wrap(const AppTextField(label: '수량', unitSuffix: '박스', numeric: true)),
      );

      expect(find.text('박스'), findsOneWidget);
    });
  });
}
