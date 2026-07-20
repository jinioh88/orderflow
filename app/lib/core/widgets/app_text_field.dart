import 'package:flutter/material.dart';

import '../theme/tokens.dart';

/// 앱 공통 입력 필드 (04-app-components §2 · 02-patterns §3).
///
/// - **라벨은 인풋 위**에 별도 텍스트로 그린다 — 플로팅 라벨 금지(40대 가독성·스캔 용이).
/// - 높이 48, radius-sm(8), 본문 16.
/// - 단위가 있는 입력은 [unitSuffix]로 인풋 안 우측에 단위를 고정 표시한다.
/// - **검증은 블러 시 1차, 제출 시 전체** — 타이핑 중 실시간 에러를 띄우지 않는다.
///   에러 상태에서 다시 입력하기 시작하면 즉시 에러를 해제한다.
class AppTextField extends StatefulWidget {
  const AppTextField({
    super.key,
    required this.label,
    this.controller,
    this.hint,
    this.helperText,
    this.optional = false,
    this.obscureText = false,
    this.keyboardType,
    this.textInputAction,
    this.autofillHints,
    this.enabled = true,
    this.validator,
    this.onSubmitted,
    this.unitSuffix,
    this.numeric = false,
    this.focusNode,
  });

  final String label;
  final TextEditingController? controller;
  final String? hint;

  /// 인풋 아래 보조 설명. 에러가 있으면 에러 메시지가 대신 표시된다.
  final String? helperText;

  /// 필수 항목이 과반인 폼에서는 선택 항목에만 `(선택)`을 붙인다 (02-patterns §3).
  final bool optional;

  final bool obscureText;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final Iterable<String>? autofillHints;
  final bool enabled;

  /// 반환값이 null이 아니면 에러 메시지로 표시된다. 메시지 톤은 "원인 + 해결"(02-patterns §3.1).
  final String? Function(String?)? validator;

  final ValueChanged<String>? onSubmitted;

  /// 인풋 안 우측에 고정 표시할 단위 (예: `박스`, `원`).
  final String? unitSuffix;

  /// 수량·금액 입력 — 우측 정렬 + tabular figures (01-foundations §5).
  final bool numeric;

  final FocusNode? focusNode;

  @override
  State<AppTextField> createState() => _AppTextFieldState();
}

class _AppTextFieldState extends State<AppTextField> {
  final _fieldKey = GlobalKey<FormFieldState<String>>();
  late final FocusNode _focusNode;
  late final bool _ownsFocusNode;

  /// 재입력으로 에러를 해제할 때만 잠깐 켜지는 플래그.
  ///
  /// `validate()`는 동기 호출이라, 이 플래그가 켜져 있는 동안 실행되는 검증은
  /// 우리가 방금 일으킨 "해제용" 검증뿐이다. 제출 시 `Form.validate()`가
  /// 부르는 검증은 영향을 받지 않는다.
  bool _clearingError = false;

  @override
  void initState() {
    super.initState();
    _ownsFocusNode = widget.focusNode == null;
    _focusNode = widget.focusNode ?? FocusNode();
    _focusNode.addListener(_onFocusChange);
  }

  @override
  void dispose() {
    _focusNode.removeListener(_onFocusChange);
    if (_ownsFocusNode) _focusNode.dispose();
    super.dispose();
  }

  /// 블러 시 1차 검증.
  void _onFocusChange() {
    if (!_focusNode.hasFocus) _fieldKey.currentState?.validate();
  }

  /// 에러가 떠 있는 상태에서 재입력을 시작하면 즉시 에러를 해제한다.
  /// (타이핑 중에 다시 혼내지 않는다 — 02-patterns §3)
  void _onChanged(String value) {
    final field = _fieldKey.currentState;
    if (field == null || !field.hasError) return;
    _clearingError = true;
    field.validate();
    _clearingError = false;
  }

  @override
  Widget build(BuildContext context) {
    final labelStyle = AppText.caption.copyWith(
      fontWeight: FontWeight.w500,
      color: widget.enabled ? AppColors.textCaption : AppColors.textDisabled,
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: AppSpace.xs),
          child: Text.rich(
            TextSpan(
              text: widget.label,
              children: widget.optional
                  ? [
                      TextSpan(
                        text: ' (선택)',
                        style: labelStyle.copyWith(
                          color: AppColors.textDisabled,
                        ),
                      ),
                    ]
                  : null,
            ),
            style: labelStyle,
          ),
        ),
        TextFormField(
          key: _fieldKey,
          controller: widget.controller,
          focusNode: _focusNode,
          enabled: widget.enabled,
          obscureText: widget.obscureText,
          keyboardType: widget.keyboardType,
          textInputAction: widget.textInputAction,
          autofillHints: widget.autofillHints,
          onFieldSubmitted: widget.onSubmitted,
          onChanged: _onChanged,
          validator: (value) =>
              _clearingError ? null : widget.validator?.call(value),
          // 타이핑 중 실시간 검증 금지 — 블러(_onFocusChange)와 제출 시에만 검증한다.
          autovalidateMode: AutovalidateMode.disabled,
          textAlign: widget.numeric ? TextAlign.right : TextAlign.start,
          style: widget.numeric
              ? AppText.tabular(AppText.body).copyWith(
                  color: AppColors.textBody,
                )
              : AppText.body.copyWith(color: AppColors.textBody),
          decoration: InputDecoration(
            hintText: widget.hint,
            helperText: widget.helperText,
            helperStyle: AppText.caption.copyWith(
              color: AppColors.textCaption,
            ),
            // `suffixText`는 포커스·입력 값이 있을 때만 그려진다. 단위는 빈 입력에서도
            // 항상 보여야 하므로(02-patterns §3) 직접 위젯으로 붙인다.
            suffixIcon: widget.unitSuffix == null
                ? null
                : Padding(
                    padding: const EdgeInsets.only(right: AppSpace.md),
                    child: Text(
                      widget.unitSuffix!,
                      style: AppText.body.copyWith(
                        color: AppColors.textCaption,
                      ),
                    ),
                  ),
            suffixIconConstraints: const BoxConstraints(minHeight: 0),
          ),
        ),
      ],
    );
  }
}
