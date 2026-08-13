import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/tokens.dart';
import '../../../core/widgets/app_button.dart';
import '../../auth/application/auth_controller.dart';
import '../../auth/application/logout_controller.dart';

/// 더보기 화면 (04-app-components §1.1 — 더보기 탭).
///
/// 입고 확인(M4)·재고(v1.1)·알림 설정이 들어갈 자리. 지금은 계정 정보와 로그아웃만 있다.
/// 입고 확인의 **주 진입로는 홈의 '오늘 도착 예정' 카드**이고 여기는 보조 진입로다.
class MoreScreen extends ConsumerWidget {
  const MoreScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authControllerProvider).user;

    return Scaffold(
      appBar: AppBar(title: const Text('더보기')),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(AppSpace.lg),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (user != null) ...[
                Text(
                  user.name,
                  style: AppText.heading.copyWith(color: AppColors.textTitle),
                ),
                const SizedBox(height: AppSpace.xs),
                Text(
                  user.email,
                  style: AppText.caption.copyWith(color: AppColors.textCaption),
                ),
                const SizedBox(height: AppSpace.xl),
              ],
              Text(
                '입고 확인·알림 설정이 이 자리에 들어갑니다',
                style: AppText.caption.copyWith(color: AppColors.textCaption),
              ),
              const Spacer(),
              AppButton.secondary(
                label: '로그아웃',
                icon: Icons.logout,
                loading: ref.watch(logoutControllerProvider).submitting,
                onPressed: () =>
                    ref.read(logoutControllerProvider.notifier).submit(),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
