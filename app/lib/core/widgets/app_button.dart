import 'package:flutter/material.dart';

import '../theme/tokens.dart';

/// 앱 버튼 변형 (04-app-components §2).
enum _Variant {
  /// h=52, 전폭, primary 배경 + 흰 텍스트 — 하단 고정 주 액션.
  cta,

  /// h=48, primary 배경 — 카드 안 주 액션.
  primary,

  /// h=48, surface + border — 보조 액션.
  secondary,

  /// h=48, danger 배경 — 파괴적 액션(확인 다이얼로그 세트).
  danger,
}

/// 앱 공통 버튼.
///
/// **오렌지+흰 텍스트 조합을 캡슐화하는 위젯이다** (04 §2 / 01-foundations §1.2):
/// 흰 텍스트 on `#EA580C`는 대비 3.2:1이라 16px semibold 이상에서만 허용되는데,
/// 그 조건을 사람이 매번 기억하는 대신 이 위젯 안에 가둔다. 화면 코드는
/// `AppColors.primary` 배경 + 흰 텍스트를 직접 조합하지 않는다.
///
/// 로딩 중에는 라벨을 유지한 채 스피너를 겹쳐 **버튼 폭이 변하지 않는다** (02-patterns §2.2).
class AppButton extends StatelessWidget {
  const AppButton.cta({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.loading = false,
  }) : _variant = _Variant.cta;

  const AppButton.primary({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.loading = false,
  }) : _variant = _Variant.primary;

  const AppButton.secondary({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.loading = false,
  }) : _variant = _Variant.secondary;

  const AppButton.danger({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.loading = false,
  }) : _variant = _Variant.danger;

  final String label;

  /// null이면 비활성. 로딩 중에도 눌리지 않는다.
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool loading;

  final _Variant _variant;

  bool get _isCta => _variant == _Variant.cta;

  @override
  Widget build(BuildContext context) {
    final enabled = onPressed != null && !loading;
    final height = _isCta ? AppTouch.ctaHeight : AppTouch.minTarget;

    final (Color bg, Color fg, BorderSide? side) = switch (_variant) {
      _Variant.cta || _Variant.primary => (
          enabled ? AppColors.primary : AppColors.surfaceMuted,
          enabled ? AppColors.textOnPrimary : AppColors.textDisabled,
          null,
        ),
      _Variant.secondary => (
          AppColors.surface,
          enabled ? AppColors.textBody : AppColors.textDisabled,
          BorderSide(
            color: enabled ? AppColors.borderStrong : AppColors.border,
          ),
        ),
      _Variant.danger => (
          enabled ? AppFeedback.danger.solid : AppColors.surfaceMuted,
          enabled ? AppColors.textOnPrimary : AppColors.textDisabled,
          null,
        ),
    };

    return SizedBox(
      width: _isCta ? double.infinity : null,
      height: height,
      child: Material(
        color: bg,
        borderRadius: BorderRadius.circular(AppRadius.md),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: enabled ? onPressed : null,
          child: Ink(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(AppRadius.md),
              border: side == null ? null : Border.fromBorderSide(side),
            ),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: AppSpace.lg),
              child: Stack(
                alignment: Alignment.center,
                children: [
                  // 라벨은 로딩 중에도 자리를 지킨다 — 폭 변화 금지.
                  Opacity(
                    opacity: loading ? 0 : 1,
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        if (icon != null) ...[
                          Icon(icon, size: AppIconSize.inline, color: fg),
                          const SizedBox(width: AppSpace.sm),
                        ],
                        Flexible(
                          child: Text(
                            label,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: AppText.bodyStrong.copyWith(color: fg),
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (loading)
                    SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: fg,
                      ),
                    ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// 텍스트 버튼 (04 §2) — h=48 터치 영역, `primaryStrong` 텍스트.
///
/// 본문 크기 오렌지는 `primary`(orange-600)가 아니라 `primaryStrong`(orange-700)이어야
/// 대비 4.5:1을 넘긴다 (01-foundations §1.2).
class AppTextButton extends StatelessWidget {
  const AppTextButton({
    super.key,
    required this.label,
    required this.onPressed,
  });

  final String label;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: AppTouch.minTarget,
      child: TextButton(
        onPressed: onPressed,
        child: Text(label),
      ),
    );
  }
}
