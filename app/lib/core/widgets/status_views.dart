// 로딩 · 에러 · 빈 상태 공통 뷰 (02-patterns §2).
//
// 모든 화면이 같은 모양의 상태 표시를 쓰게 해서 "명확한 상태 표시" 원칙을 지킨다.

import 'package:flutter/material.dart';

import '../theme/tokens.dart';
import 'app_button.dart';

/// 300ms 미만에 끝나는 응답에는 로딩을 표시하지 않는다 — 깜빡임 방지(02-patterns §2.2).
const _loadingDelay = Duration(milliseconds: 300);

/// 스피너 로딩.
///
/// 목록·카드의 **최초 로드에는 스피너 대신 [SkeletonList]를 쓴다.** 이 위젯은
/// 스켈레톤 형상을 만들 수 없는 경우(전체 화면 전환 등)의 폴백이다.
class LoadingView extends StatelessWidget {
  const LoadingView({super.key});

  @override
  Widget build(BuildContext context) {
    return const DelayedVisibility(
      child: Center(child: CircularProgressIndicator()),
    );
  }
}

/// 300ms 지연 후에 자식을 노출한다.
///
/// 빠른 응답에서 로딩 표시가 한 프레임 번쩍이는 것을 막는다.
class DelayedVisibility extends StatefulWidget {
  const DelayedVisibility({
    super.key,
    required this.child,
    this.delay = _loadingDelay,
  });

  final Widget child;
  final Duration delay;

  @override
  State<DelayedVisibility> createState() => _DelayedVisibilityState();
}

class _DelayedVisibilityState extends State<DelayedVisibility> {
  late final Future<void> _delay = Future<void>.delayed(widget.delay);

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<void>(
      future: _delay,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const SizedBox.shrink();
        }
        return widget.child;
      },
    );
  }
}

/// 목록 스켈레톤 — 실제 레이아웃과 같은 형상의 회색 블록 (02-patterns §2.2).
///
/// 1.5초 주기 셔머. 최초 로드에만 쓰고, 재조회(이미 데이터가 있는 경우)에는
/// 기존 데이터를 유지한 채 미세 스피너를 쓴다 — 화면 전체를 로딩으로 덮지 않는다.
class SkeletonList extends StatelessWidget {
  const SkeletonList({super.key, this.itemCount = 6, this.itemHeight = 72});

  final int itemCount;
  final double itemHeight;

  @override
  Widget build(BuildContext context) {
    return DelayedVisibility(
      child: ListView.separated(
        padding: const EdgeInsets.all(AppSpace.lg),
        itemCount: itemCount,
        separatorBuilder: (_, _) => const SizedBox(height: AppSpace.md),
        itemBuilder: (_, _) => SkeletonBox(height: itemHeight),
      ),
    );
  }
}

/// 셔머가 흐르는 회색 블록.
class SkeletonBox extends StatefulWidget {
  const SkeletonBox({
    super.key,
    required this.height,
    this.width = double.infinity,
  });

  final double height;
  final double width;

  @override
  State<SkeletonBox> createState() => _SkeletonBoxState();
}

class _SkeletonBoxState extends State<SkeletonBox>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1500),
  )..repeat();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ExcludeSemantics(
      child: AnimatedBuilder(
        animation: _controller,
        builder: (context, _) {
          // -1 → 2 로 흐르는 그라디언트 (블록 폭의 3배를 훑고 지나간다).
          final shift = _controller.value * 3 - 1;
          return Container(
            height: widget.height,
            width: widget.width,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(AppRadius.md),
              gradient: LinearGradient(
                begin: Alignment(shift - 1, 0),
                end: Alignment(shift + 1, 0),
                colors: const [
                  AppColors.surfaceMuted,
                  AppColors.surfaceAlt,
                  AppColors.surfaceMuted,
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

/// 화면 단위 실패 (02-patterns §2.3).
///
/// **"다시 시도" 버튼은 필수다** — 재시도 수단 없는 에러 화면을 만들지 않는다.
/// 그래서 [onRetry]가 선택 인자가 아니다.
class ErrorView extends StatelessWidget {
  const ErrorView({
    super.key,
    required this.message,
    required this.onRetry,
    this.title = '불러오지 못했습니다',
  });

  final String title;
  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return _CenteredState(
      icon: Icons.error_outline,
      iconColor: AppFeedback.danger.solid,
      title: title,
      description: message,
      action: AppButton.secondary(
        label: '다시 시도',
        icon: Icons.refresh,
        onPressed: onRetry,
      ),
    );
  }
}

/// 빈 상태 (02-patterns §2.1) — 아이콘 + 제목 1줄 + 설명 1줄 + (가능하면) 액션.
///
/// **필터 결과 없음을 "데이터 없음"처럼 보여주지 않는다.** 조건 때문에 비었다면
/// 제목에서 그 사실을 밝히고 [action]에 "필터 초기화"를 준다.
class EmptyView extends StatelessWidget {
  const EmptyView({
    super.key,
    required this.title,
    this.description,
    this.icon = Icons.inbox_outlined,
    this.action,
  });

  final String title;
  final String? description;
  final IconData icon;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return _CenteredState(
      icon: icon,
      iconColor: AppColors.inactive,
      title: title,
      description: description,
      action: action,
    );
  }
}

class _CenteredState extends StatelessWidget {
  const _CenteredState({
    required this.icon,
    required this.iconColor,
    required this.title,
    this.description,
    this.action,
  });

  final IconData icon;
  final Color iconColor;
  final String title;
  final String? description;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(AppSpace.xl),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: AppIconSize.emptyState, color: iconColor),
            const SizedBox(height: AppSpace.lg),
            Text(
              title,
              textAlign: TextAlign.center,
              style: AppText.heading.copyWith(color: AppColors.textTitle),
            ),
            if (description != null) ...[
              const SizedBox(height: AppSpace.sm),
              Text(
                description!,
                textAlign: TextAlign.center,
                style: AppText.caption.copyWith(color: AppColors.textCaption),
              ),
            ],
            if (action != null) ...[
              const SizedBox(height: AppSpace.xl),
              action!,
            ],
          ],
        ),
      ),
    );
  }
}
