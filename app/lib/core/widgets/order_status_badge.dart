import 'package:flutter/material.dart';

import '../theme/tokens.dart';

/// 뱃지 크기 변형 (02-patterns §1.1).
enum BadgeSize {
  /// 앱 목록 행 — dot 6, 높이 24, 14/500.
  md,

  /// 앱 카드 헤더 — dot 8, 높이 32, app.body-strong(16).
  lg,
}

/// 발주 상태 뱃지 — **7상태 + 로컬 '전송 대기' 변형을 이 위젯 하나가 커버한다**
/// (04-app-components §8 인수 조건).
///
/// - 라벨은 [OrderStatus]의 `label`을 그대로 쓴다. 화면마다 다른 표현 금지(02-patterns §1).
/// - **dot 단독 표현 금지** — 색맹 대비를 위해 항상 라벨을 동반한다(01-foundations §1.4).
///   그래서 이 위젯에는 "라벨 숨김" 옵션이 없다.
/// - '전송 대기'는 서버 상태가 아니므로 dot 대신 `schedule_send` 아이콘으로
///   7상태와 시각적으로 구분한다 (04 §7).
class OrderStatusBadge extends StatelessWidget {
  /// 서버가 준 발주 상태.
  const OrderStatusBadge(this.status, {super.key, this.size = BadgeSize.md})
      : _pendingSend = false;

  /// 오프라인 큐에 쌓여 아직 전송되지 않은 로컬 발주 (US-ORD-07).
  const OrderStatusBadge.pendingSend({super.key, this.size = BadgeSize.md})
      : status = null,
        _pendingSend = true;

  final OrderStatus? status;
  final BadgeSize size;
  final bool _pendingSend;

  @override
  Widget build(BuildContext context) {
    final isLarge = size == BadgeSize.lg;
    final label = _pendingSend ? '전송 대기' : status!.label;
    final textColor = _pendingSend ? AppColors.textCaption : status!.text;
    final bgColor = _pendingSend ? AppColors.surfaceMuted : status!.bg;

    final textStyle = (isLarge ? AppText.bodyStrong : AppText.caption).copyWith(
      fontWeight: isLarge ? FontWeight.w600 : FontWeight.w500,
      color: textColor,
      height: 1,
    );

    return Semantics(
      label: '발주 상태 $label',
      child: Container(
        height: isLarge ? 32 : 24,
        padding: EdgeInsets.symmetric(horizontal: isLarge ? 12 : 10),
        decoration: BoxDecoration(
          color: bgColor,
          borderRadius: BorderRadius.circular(AppRadius.full),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (_pendingSend)
              Icon(
                Icons.schedule_send,
                size: isLarge ? 16 : 14,
                color: textColor,
              )
            else
              Container(
                width: isLarge ? 8 : 6,
                height: isLarge ? 8 : 6,
                decoration: BoxDecoration(
                  color: status!.dot,
                  shape: BoxShape.circle,
                ),
              ),
            SizedBox(width: isLarge ? AppSpace.sm : 6),
            Text(label, style: textStyle),
          ],
        ),
      ),
    );
  }
}
