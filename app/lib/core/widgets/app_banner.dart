import 'package:flutter/material.dart';

import '../theme/tokens.dart';

/// 배너 — "사건"인 토스트와 달리 **상태가 해소될 때까지 유지되는 고지** (02-patterns §4.5).
///
/// 지금 쓰이는 곳은 폼 상단의 전역 에러다 (02-patterns §3.1 — 필드 오류는 필드 아래,
/// 전역 오류는 폼 상단 배너). 마감 임박/이월 안내(04 §4.2)·오프라인(04 §7)도
/// 같은 형태를 쓰므로 톤만 바꿔 재사용한다.
class AppBanner extends StatelessWidget {
  const AppBanner.danger({super.key, required this.message})
    : _tone = AppFeedback.danger,
      _icon = Icons.error_outline;

  const AppBanner.info({super.key, required this.message})
    : _tone = AppFeedback.info,
      _icon = Icons.info_outline;

  const AppBanner.warning({super.key, required this.message})
    : _tone = AppFeedback.warning,
      _icon = Icons.schedule;

  final String message;
  final FeedbackColor _tone;
  final IconData _icon;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpace.md,
        vertical: AppSpace.md,
      ),
      decoration: BoxDecoration(
        color: _tone.bg,
        borderRadius: BorderRadius.circular(AppRadius.sm),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(_icon, size: AppIconSize.inline, color: _tone.solid),
          const SizedBox(width: AppSpace.sm),
          Expanded(
            child: Text(
              message,
              style: AppText.caption.copyWith(color: _tone.text),
            ),
          ),
        ],
      ),
    );
  }
}
