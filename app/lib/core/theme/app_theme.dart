import 'package:flutter/material.dart';

/// 앱 공통 테마.
///
/// 페르소나(영업 마감 후 지친 점주)를 위한 원칙: 큰 터치 타깃, 명확한 상태 표시.
/// 터치 타깃 최소 높이를 [minTouchTarget]으로 통일한다.
abstract final class AppTheme {
  /// 주요 조작 요소(버튼·입력창)의 최소 높이.
  static const double minTouchTarget = 56;

  static ThemeData light() {
    final scheme = ColorScheme.fromSeed(seedColor: const Color(0xFF1B5E20));
    return ThemeData(
      colorScheme: scheme,
      visualDensity: VisualDensity.standard,
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size.fromHeight(minTouchTarget),
          textStyle: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size.fromHeight(minTouchTarget),
          textStyle: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
        ),
      ),
      inputDecorationTheme: const InputDecorationTheme(
        border: OutlineInputBorder(),
        contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 18),
      ),
      listTileTheme: const ListTileThemeData(minVerticalPadding: 14),
    );
  }
}
