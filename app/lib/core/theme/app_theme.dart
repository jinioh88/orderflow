import 'package:flutter/material.dart';

import 'tokens.dart';

/// 앱 공통 테마 — [tokens.dart]의 토큰을 `ThemeData`로 옮긴 것.
///
/// 페르소나(영업 마감 후 지친 40대 점주)를 위한 원칙: 큰 글자·큰 타깃·명확한 상태.
/// 개별 위젯은 여기서 정한 기본값을 신뢰하고, 화면 코드는 색·크기를 직접 지정하지 않는다.
abstract final class AppTheme {
  static ThemeData light() {
    final colorScheme = ColorScheme(
      brightness: Brightness.light,
      primary: AppColors.primary,
      onPrimary: AppColors.textOnPrimary,
      primaryContainer: AppColors.primaryBg,
      onPrimaryContainer: AppColors.primaryStrong,
      secondary: AppColors.primaryStrong,
      onSecondary: AppColors.textOnPrimary,
      error: AppFeedback.danger.solid,
      onError: AppColors.textOnPrimary,
      errorContainer: AppFeedback.danger.bg,
      onErrorContainer: AppFeedback.danger.text,
      surface: AppColors.surface,
      onSurface: AppColors.textBody,
      surfaceContainerLowest: AppColors.surface,
      surfaceContainerLow: AppColors.surfaceAlt,
      surfaceContainer: AppColors.surfaceAlt,
      surfaceContainerHigh: AppColors.surfaceMuted,
      surfaceContainerHighest: AppColors.surfaceMuted,
      onSurfaceVariant: AppColors.textCaption,
      outline: AppColors.border,
      outlineVariant: AppColors.borderStrong,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      fontFamily: AppText.fontFamily,
      scaffoldBackgroundColor: AppColors.pageBg,
      textTheme: _textTheme,
      appBarTheme: const AppBarTheme(
        toolbarHeight: 56,
        backgroundColor: AppColors.surface,
        foregroundColor: AppColors.textTitle,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        titleTextStyle: TextStyle(
          fontFamily: AppText.fontFamily,
          fontSize: 22,
          height: 30 / 22,
          fontWeight: FontWeight.w600,
          color: AppColors.textTitle,
        ),
        iconTheme: IconThemeData(
          color: AppColors.textTitle,
          size: AppIconSize.normal,
        ),
      ),
      iconTheme: const IconThemeData(
        color: AppColors.textBody,
        size: AppIconSize.normal,
      ),
      dividerTheme: const DividerThemeData(
        color: AppColors.border,
        thickness: 1,
        space: 1,
      ),
      // 인풋: h=48, radius-sm(8), app.body(16), 포커스 보더 app.primary (04 §2).
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: AppColors.surface,
        constraints: const BoxConstraints(minHeight: AppTouch.minTarget),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: AppSpace.md,
          vertical: AppSpace.md,
        ),
        hintStyle: AppText.body.copyWith(color: AppColors.textDisabled),
        // 라벨은 인풋 위에 별도로 그린다 (02-patterns §3 — 플로팅 라벨 금지).
        border: _inputBorder(AppColors.border),
        enabledBorder: _inputBorder(AppColors.border),
        focusedBorder: _inputBorder(AppColors.primary, width: 2),
        errorBorder: _inputBorder(AppFeedback.danger.solid),
        focusedErrorBorder: _inputBorder(AppFeedback.danger.solid, width: 2),
        disabledBorder: _inputBorder(AppColors.border),
        errorStyle: AppText.caption.copyWith(color: AppFeedback.danger.text),
        suffixStyle: AppText.body.copyWith(color: AppColors.textCaption),
      ),
      // 버튼 기본형은 AppButton 위젯이 캡슐화한다 (04 §2 — 오렌지+흰 텍스트 조합 캡슐화).
      // 여기 테마는 Flutter 기본 버튼을 쓰는 경우의 안전망이다.
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size.fromHeight(AppTouch.minTarget),
          textStyle: AppText.bodyStrong,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppRadius.md),
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size.fromHeight(AppTouch.minTarget),
          foregroundColor: AppColors.textBody,
          textStyle: AppText.bodyStrong,
          side: const BorderSide(color: AppColors.border),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppRadius.md),
          ),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          minimumSize: const Size(AppTouch.minTarget, AppTouch.minTarget),
          foregroundColor: AppColors.primaryStrong,
          textStyle: AppText.bodyStrong,
        ),
      ),
      cardTheme: CardThemeData(
        color: AppColors.surface,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppRadius.lg),
          side: const BorderSide(color: AppColors.border),
        ),
      ),
      bottomSheetTheme: const BottomSheetThemeData(
        backgroundColor: AppColors.surface,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(
            top: Radius.circular(AppRadius.lg),
          ),
        ),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: AppColors.surface,
        surfaceTintColor: Colors.transparent,
        titleTextStyle: AppText.heading,
        contentTextStyle: AppText.body,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppRadius.lg),
        ),
      ),
      // 하단 탭: 높이 56, 활성 app.primary / 비활성 textCaption (04 §1.1).
      navigationBarTheme: NavigationBarThemeData(
        height: 56,
        backgroundColor: AppColors.surface,
        surfaceTintColor: Colors.transparent,
        indicatorColor: Colors.transparent,
        elevation: 0,
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        iconTheme: WidgetStateProperty.resolveWith(
          (states) => IconThemeData(
            size: AppIconSize.normal,
            color: states.contains(WidgetState.selected)
                ? AppColors.primary
                : AppColors.textCaption,
          ),
        ),
        labelTextStyle: WidgetStateProperty.resolveWith(
          (states) => TextStyle(
            fontFamily: AppText.fontFamily,
            fontSize: 12,
            height: 16 / 12,
            fontWeight: FontWeight.w600,
            color: states.contains(WidgetState.selected)
                ? AppColors.primary
                : AppColors.textCaption,
          ),
        ),
      ),
      snackBarTheme: SnackBarThemeData(
        backgroundColor: AppColors.surface,
        contentTextStyle: AppText.body,
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppRadius.md),
        ),
      ),
      progressIndicatorTheme: const ProgressIndicatorThemeData(
        color: AppColors.primary,
      ),
    );
  }

  static OutlineInputBorder _inputBorder(Color color, {double width = 1}) {
    return OutlineInputBorder(
      borderRadius: BorderRadius.circular(AppRadius.sm),
      borderSide: BorderSide(color: color, width: width),
    );
  }

  /// app 타이포 스케일 → Material `TextTheme` 매핑.
  ///
  /// Material 위젯이 기본으로 집어가는 슬롯에 앱 스케일을 꽂아, 화면 코드가
  /// 스타일을 지정하지 않아도 앱 스케일이 나오게 한다.
  static final TextTheme _textTheme = TextTheme(
    displaySmall: AppText.display.copyWith(color: AppColors.textTitle),
    headlineMedium: AppText.display.copyWith(color: AppColors.textTitle),
    headlineSmall: AppText.title.copyWith(color: AppColors.textTitle),
    titleLarge: AppText.title.copyWith(color: AppColors.textTitle),
    titleMedium: AppText.heading.copyWith(color: AppColors.textTitle),
    titleSmall: AppText.bodyStrong.copyWith(color: AppColors.textTitle),
    bodyLarge: AppText.body.copyWith(color: AppColors.textBody),
    bodyMedium: AppText.caption.copyWith(color: AppColors.textCaption),
    bodySmall: AppText.caption.copyWith(color: AppColors.textCaption),
    labelLarge: AppText.bodyStrong,
    labelMedium: AppText.caption.copyWith(color: AppColors.textCaption),
    labelSmall: AppText.caption.copyWith(color: AppColors.textCaption),
  );
}
