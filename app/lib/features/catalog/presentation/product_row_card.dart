import 'package:flutter/material.dart';

import '../../../core/format/krw.dart';
import '../../../core/theme/tokens.dart';
import '../data/product.dart';

/// 상품 행 카드 (04-app-components §3.1).
///
/// - **리스트형이지 그리드가 아니다** — 품명 가독이 우선이라 한 행에 하나만 놓는다.
/// - 높이 최소 72. 품명은 1줄 말줄임, 단가·발주 단위는 caption.
/// - 한정 품목은 품명 옆 `한정` 미니 뱃지(warning) + 잔여 수량.
///
/// 수량 스테퍼·[담기] 버튼은 M2(ORD)에서 붙는다 — M1 카탈로그는 조회만 한다.
class ProductRowCard extends StatelessWidget {
  const ProductRowCard(this.product, {super.key});

  final Product product;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      // 뱃지·잔여 수량이 시각 요소로만 전달되지 않게 한 줄로 읽어 준다.
      label: [
        product.name,
        if (product.limited) '한정 품목',
        '${formatKrw(product.unitPrice)} ${product.orderUnit}당',
        if (product.availableQty != null) '잔여 ${formatCount(product.availableQty!)}',
      ].join(', '),
      child: ExcludeSemantics(
        child: Container(
          constraints: const BoxConstraints(minHeight: 72),
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpace.lg,
            vertical: AppSpace.md,
          ),
          decoration: BoxDecoration(
            color: AppColors.surface,
            borderRadius: BorderRadius.circular(AppRadius.md),
            border: Border.all(color: AppColors.border),
          ),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Row(
                      children: [
                        Flexible(
                          child: Text(
                            product.name,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: AppText.bodyStrong.copyWith(
                              color: AppColors.textTitle,
                            ),
                          ),
                        ),
                        if (product.limited) ...[
                          const SizedBox(width: AppSpace.sm),
                          const _LimitedBadge(),
                        ],
                      ],
                    ),
                    const SizedBox(height: AppSpace.xs),
                    Text(
                      _priceLine,
                      style: AppText.tabular(
                        AppText.caption,
                      ).copyWith(color: AppColors.textCaption),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// `₩12,000 / BOX` — 한정 품목이면 잔여 수량을 같은 줄에 잇는다.
  ///
  /// 잔여를 별도 줄로 빼면 한정 품목만 행 높이가 달라져 목록 스캔이 흔들린다.
  String get _priceLine {
    final base = '${formatKrw(product.unitPrice)} / ${product.orderUnit}';
    final remaining = product.availableQty;
    return remaining == null ? base : '$base · 잔여 ${formatCount(remaining)}';
  }
}

/// 한정 품목 미니 뱃지 (04 §3.1 — warning 계열).
///
/// 발주 상태 뱃지([OrderStatusBadge])와 달리 dot이 없다. 상태 전이가 아니라
/// 품목의 성질을 나타내는 표시라 같은 어휘를 쓰지 않는다.
class _LimitedBadge extends StatelessWidget {
  const _LimitedBadge();

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 20,
      padding: const EdgeInsets.symmetric(horizontal: AppSpace.sm),
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: AppFeedback.warning.bg,
        borderRadius: BorderRadius.circular(AppRadius.full),
      ),
      child: Text(
        '한정',
        style: AppText.caption.copyWith(
          fontWeight: FontWeight.w500,
          color: AppFeedback.warning.text,
          height: 1,
        ),
      ),
    );
  }
}
