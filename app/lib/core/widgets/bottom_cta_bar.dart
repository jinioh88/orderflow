import 'package:flutter/material.dart';

import '../theme/tokens.dart';

/// 하단 고정 CTA 바 (04-app-components §1.2).
///
/// 주 액션이 있는 화면(장바구니, 검수 등)의 하단에 고정한다.
/// `surface` + 상단 border + shadow-2, 패딩 12/16. 버튼 1개(전폭) 또는 요약 + 버튼.
///
/// `Scaffold.bottomNavigationBar` 또는 `Scaffold.persistentFooterButtons` 대신
/// 이 위젯을 쓰면 세이프에어리어·그림자·패딩이 화면마다 어긋나지 않는다.
class BottomCtaBar extends StatelessWidget {
  const BottomCtaBar({super.key, required this.child, this.summary});

  /// 주 액션 버튼 — 보통 `AppButton.cta`.
  final Widget child;

  /// 버튼 위에 놓는 요약 줄 (예: `12품목 · ₩1,240,000`).
  final Widget? summary;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.surface,
        border: Border(top: BorderSide(color: AppColors.border)),
        // shadow-2를 위쪽으로 뒤집어 쓴다 — 바가 화면 아래에 붙어 있어
        // 아래로 지는 그림자는 보이지 않는다 (styleguide-app.html의 .ctabar와 동일).
        boxShadow: AppShadow.level2Up,
      ),
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpace.lg,
            vertical: AppSpace.md,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (summary != null) ...[
                summary!,
                const SizedBox(height: AppSpace.md),
              ],
              child,
            ],
          ),
        ),
      ),
    );
  }
}
