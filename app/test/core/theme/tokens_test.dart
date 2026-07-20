import 'dart:convert';
import 'dart:io';

import 'package:app/core/theme/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

/// 디자인 토큰이 `design/design-system/tokens.json`과 일치하는지 검사한다.
///
/// tokens.json이 값의 단일 진실 공급원이므로(01-foundations §0), Dart 상수는
/// 그 복제본일 뿐이다. 디자인 시스템이 갱신되면 이 테스트가 먼저 깨져서 알려준다.
void main() {
  final tokensFile = File('../design/design-system/tokens.json');
  final tokens =
      jsonDecode(tokensFile.readAsStringSync()) as Map<String, dynamic>;

  /// `{core.color.blue.600}` 같은 참조를 실제 hex까지 따라간다.
  Color resolve(String ref) {
    var value = ref;
    while (value.startsWith('{') && value.endsWith('}')) {
      dynamic node = tokens;
      for (final key in value.substring(1, value.length - 1).split('.')) {
        node = (node as Map<String, dynamic>)[key];
      }
      value = node as String;
    }
    return Color(int.parse(value.replaceFirst('#', 'FF'), radix: 16));
  }

  Map<String, dynamic> section(List<String> path) {
    dynamic node = tokens;
    for (final key in path) {
      node = (node as Map<String, dynamic>)[key];
    }
    return node as Map<String, dynamic>;
  }

  test('tokens.json 파일이 존재한다', () {
    expect(tokensFile.existsSync(), isTrue,
        reason: '디자인 시스템 경로가 바뀌었다면 이 테스트 경로도 고쳐야 한다');
  });

  group('app.color', () {
    final appColor = section(['app', 'color']);

    final expected = <String, Color>{
      'primary': AppColors.primary,
      'primaryStrong': AppColors.primaryStrong,
      'primaryBg': AppColors.primaryBg,
      'pageBg': AppColors.pageBg,
      'surfaceAlt': AppColors.surfaceAlt,
      'border': AppColors.border,
      'textTitle': AppColors.textTitle,
      'textBody': AppColors.textBody,
      'textCaption': AppColors.textCaption,
      'textDisabled': AppColors.textDisabled,
      'scanScreenBg': AppColors.scanScreenBg,
      'scanScreenText': AppColors.scanScreenText,
    };

    expected.forEach((key, color) {
      test('app.color.$key', () {
        expect(color, resolve(appColor[key] as String));
      });
    });
  });

  group('semantic.color.feedback', () {
    final feedback = section(['semantic', 'color', 'feedback']);

    final expected = <String, FeedbackColor>{
      'success': AppFeedback.success,
      'warning': AppFeedback.warning,
      'danger': AppFeedback.danger,
      'info': AppFeedback.info,
    };

    expected.forEach((key, value) {
      test('semantic.$key', () {
        final json = feedback[key] as Map<String, dynamic>;
        expect(value.solid, resolve(json['solid'] as String));
        expect(value.text, resolve(json['text'] as String));
        expect(value.bg, resolve(json['bg'] as String));
      });
    });
  });

  group('발주 상태 7색 — 라벨·색이 토큰과 같다', () {
    final orderStatus = section(['semantic', 'color', 'orderStatus']);

    test('상태 개수가 7개다', () {
      expect(OrderStatus.values.length, 7);
      expect(orderStatus.keys.length, 7);
    });

    for (final status in OrderStatus.values) {
      test(status.code, () {
        final json = orderStatus[status.code] as Map<String, dynamic>;
        expect(json, isNotNull, reason: '${status.code}가 tokens.json에 없다');
        // 라벨은 화면마다 다르게 쓰지 않는다 (02-patterns §1).
        expect(status.label, json['label']);
        expect(status.dot, resolve(json['dot'] as String));
        expect(status.text, resolve(json['text'] as String));
        expect(status.bg, resolve(json['bg'] as String));
      });
    }
  });

  group('app.typography', () {
    final typography = section(['app', 'typography']);

    final expected = <String, TextStyle>{
      'display': AppText.display,
      'title': AppText.title,
      'heading': AppText.heading,
      'body': AppText.body,
      'bodyStrong': AppText.bodyStrong,
      'caption': AppText.caption,
      'numXl': AppText.numXl,
    };

    expected.forEach((key, style) {
      test('app.typography.$key', () {
        final json = typography[key] as Map<String, dynamic>;
        final size = (json['size'] as num).toDouble();
        expect(style.fontSize, size);
        expect(style.height, (json['lineHeight'] as num) / size);
        expect(style.fontWeight?.value, json['weight']);
        if (json['tabularNums'] == true) {
          expect(style.fontFeatures, contains(const FontFeature.tabularFigures()),
              reason: '숫자가 정렬되는 스타일은 tabular figures를 켠다');
        }
      });
    });

    test('앱 최소 글자 크기는 14 — 그보다 작은 스케일이 없다', () {
      for (final style in expected.values) {
        expect(style.fontSize, greaterThanOrEqualTo(14));
      }
    });
  });

  group('spacing · radius · touch', () {
    test('app.spacing 값이 토큰 배열과 같다', () {
      final spacing = (section(['app'])['spacing'] as List)
          .map((v) => (v as num).toDouble())
          .toList();
      expect(
        [
          AppSpace.xs,
          AppSpace.sm,
          AppSpace.md,
          AppSpace.lg,
          AppSpace.xl,
          AppSpace.xxl,
          AppSpace.xxxl,
          AppSpace.huge,
        ],
        spacing,
      );
    });

    test('app.radius', () {
      final radius = section(['app', 'radius']);
      expect(AppRadius.sm, (radius['sm'] as num).toDouble());
      expect(AppRadius.md, (radius['md'] as num).toDouble());
      expect(AppRadius.lg, (radius['lg'] as num).toDouble());
      expect(AppRadius.full, (radius['full'] as num).toDouble());
    });

    test('app.touch — 최소 터치 타깃 48dp', () {
      final touch = section(['app', 'touch']);
      expect(AppTouch.minTarget, (touch['minTarget'] as num).toDouble());
      expect(AppTouch.minGap, (touch['minGap'] as num).toDouble());
    });
  });
}
