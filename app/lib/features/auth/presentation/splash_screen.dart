import 'package:flutter/material.dart';

import '../../../core/theme/tokens.dart';
import '../../../core/widgets/status_views.dart';

/// 부팅 화면 — 저장된 세션을 읽는 아주 짧은 순간에만 보인다.
///
/// 이 화면이 필요한 이유: 세션 복원이 끝나기 전에 로그인 화면을 먼저 그리면,
/// 자동 로그인된 사용자에게 로그인 폼이 한 번 번쩍인다.
/// 스피너는 300ms 뒤에야 나타난다([DelayedVisibility]) — 보통은 로고만 스치고 지나간다.
class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'OrderFlow',
              style: AppText.display.copyWith(color: AppColors.primaryStrong),
            ),
            const SizedBox(height: AppSpace.xl),
            const SizedBox(
              height: AppIconSize.normal,
              width: AppIconSize.normal,
              child: DelayedVisibility(
                child: CircularProgressIndicator(strokeWidth: 2),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
