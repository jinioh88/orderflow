import 'dart:async';

import 'package:flutter/material.dart';

import '../theme/tokens.dart';

/// 수량 스테퍼 `[−] 12 [+]` (04-app-components §2).
///
/// - 버튼 몸체 44×44, 좌우 여백을 포함한 터치 영역은 48 (01-foundations §6).
/// - 값은 app.num-xl(24/700 tabular) — 자릿수가 바뀌어도 폭이 흔들리지 않는다.
/// - **길게 누르면 200ms 간격으로 연속 증감**한다. 박스 20개를 20번 탭하게 두지 않는다.
/// - 값 탭은 [onValueTap]으로 숫자 직접 입력(키패드)에 연결한다.
/// - [minValue]에서 `−`를 누르면 [onBelowMin]이 불린다 — 장바구니에서는 '제거'가 된다.
class QuantityStepper extends StatefulWidget {
  const QuantityStepper({
    super.key,
    required this.value,
    required this.onChanged,
    this.minValue = 0,
    this.maxValue,
    this.onBelowMin,
    this.onValueTap,
    this.enabled = true,
  });

  final int value;
  final ValueChanged<int> onChanged;
  final int minValue;

  /// 한정 품목의 잔여 수량 등 상한 (US-ORD-06). null이면 상한 없음.
  final int? maxValue;

  /// [minValue]에서 감소를 시도했을 때. null이면 감소 버튼이 비활성.
  final VoidCallback? onBelowMin;

  final VoidCallback? onValueTap;
  final bool enabled;

  @override
  State<QuantityStepper> createState() => _QuantityStepperState();
}

class _QuantityStepperState extends State<QuantityStepper> {
  static const _repeatInterval = Duration(milliseconds: 200);

  Timer? _repeatTimer;

  @override
  void dispose() {
    _repeatTimer?.cancel();
    super.dispose();
  }

  bool get _canDecrease =>
      widget.enabled &&
      (widget.value > widget.minValue || widget.onBelowMin != null);

  bool get _canIncrease =>
      widget.enabled &&
      (widget.maxValue == null || widget.value < widget.maxValue!);

  void _decrease() {
    if (widget.value > widget.minValue) {
      widget.onChanged(widget.value - 1);
    } else {
      _stopRepeat();
      widget.onBelowMin?.call();
    }
  }

  void _increase() {
    if (!_canIncrease) {
      _stopRepeat();
      return;
    }
    widget.onChanged(widget.value + 1);
  }

  void _startRepeat(VoidCallback step) {
    _repeatTimer?.cancel();
    _repeatTimer = Timer.periodic(_repeatInterval, (_) => step());
  }

  void _stopRepeat() {
    _repeatTimer?.cancel();
    _repeatTimer = null;
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        _StepperButton(
          icon: Icons.remove,
          semanticLabel: '수량 감소',
          enabled: _canDecrease,
          onPressed: _decrease,
          onLongPressStart: () => _startRepeat(_decrease),
          onLongPressEnd: _stopRepeat,
        ),
        // 값 폭을 고정해 증감할 때 버튼이 좌우로 밀리지 않게 한다.
        ConstrainedBox(
          constraints: const BoxConstraints(minWidth: 56),
          child: InkWell(
            onTap: widget.enabled ? widget.onValueTap : null,
            borderRadius: BorderRadius.circular(AppRadius.sm),
            child: SizedBox(
              height: AppTouch.minTarget,
              child: Center(
                child: Text(
                  '${widget.value}',
                  style: AppText.numXl.copyWith(
                    color: widget.enabled
                        ? AppColors.textTitle
                        : AppColors.textDisabled,
                  ),
                ),
              ),
            ),
          ),
        ),
        _StepperButton(
          icon: Icons.add,
          semanticLabel: '수량 증가',
          enabled: _canIncrease,
          onPressed: _increase,
          onLongPressStart: () => _startRepeat(_increase),
          onLongPressEnd: _stopRepeat,
        ),
      ],
    );
  }
}

class _StepperButton extends StatelessWidget {
  const _StepperButton({
    required this.icon,
    required this.semanticLabel,
    required this.enabled,
    required this.onPressed,
    required this.onLongPressStart,
    required this.onLongPressEnd,
  });

  final IconData icon;
  final String semanticLabel;
  final bool enabled;
  final VoidCallback onPressed;
  final VoidCallback onLongPressStart;
  final VoidCallback onLongPressEnd;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onLongPressStart: enabled ? (_) => onLongPressStart() : null,
      onLongPressEnd: enabled ? (_) => onLongPressEnd() : null,
      onLongPressCancel: enabled ? onLongPressEnd : null,
      child: Semantics(
        button: true,
        label: semanticLabel,
        enabled: enabled,
        child: InkResponse(
          onTap: enabled ? onPressed : null,
          // 몸체 44 + 여백으로 터치 영역 48 (01-foundations §6).
          radius: AppTouch.minTarget / 2,
          child: Container(
            width: AppTouch.stepperButton,
            height: AppTouch.stepperButton,
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(AppRadius.md),
              border: Border.all(color: AppColors.borderStrong),
            ),
            child: Icon(
              icon,
              size: AppIconSize.normal,
              color: enabled ? AppColors.textBody : AppColors.textDisabled,
            ),
          ),
        ),
      ),
    );
  }
}
