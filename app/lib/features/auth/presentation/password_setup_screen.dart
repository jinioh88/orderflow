import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/tokens.dart';
import '../../../core/widgets/app_banner.dart';
import '../../../core/widgets/app_button.dart';
import '../../../core/widgets/app_text_field.dart';
import '../application/auth_controller.dart';
import '../application/logout_controller.dart';
import '../application/password_setup_controller.dart';

/// 최초 로그인 비밀번호 설정 화면 (US-AUTH-02·03, api-spec 2.3·2.4.4).
///
/// 임시 비밀번호 상태에서는 이 화면 외에 아무 데도 갈 수 없다:
/// - 라우터 가드가 다른 경로 진입을 되돌린다 (`app_router.dart`)
/// - [PopScope]로 시스템 뒤로 가기도 막는다
/// - 빠져나갈 유일한 출구는 로그아웃(2.3에서 임시 상태에도 허용된 API)
class PasswordSetupScreen extends ConsumerStatefulWidget {
  const PasswordSetupScreen({super.key});

  @override
  ConsumerState<PasswordSetupScreen> createState() =>
      _PasswordSetupScreenState();
}

class _PasswordSetupScreenState extends ConsumerState<PasswordSetupScreen> {
  final _formKey = GlobalKey<FormState>();
  final _currentController = TextEditingController();
  final _newController = TextEditingController();
  final _confirmController = TextEditingController();

  @override
  void dispose() {
    _currentController.dispose();
    _newController.dispose();
    _confirmController.dispose();
    super.dispose();
  }

  void _submit() {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    FocusScope.of(context).unfocus();
    ref
        .read(passwordSetupControllerProvider.notifier)
        .submit(
          currentPassword: _currentController.text,
          newPassword: _newController.text,
        );
  }

  String? _validateCurrent(String? value) {
    if ((value ?? '').isEmpty) return '임시 비밀번호를 입력하세요';
    return null;
  }

  String? _validateNew(String? value) {
    final password = value ?? '';
    if (password.isEmpty) return '새 비밀번호를 입력하세요';
    final violation = PasswordPolicy.validate(password);
    if (violation != null) return violation;
    // 스펙 2.4.4 — 새 비밀번호는 현재 비밀번호와 달라야 한다.
    if (password == _currentController.text) {
      return '임시 비밀번호와 다른 비밀번호를 입력하세요';
    }
    return null;
  }

  String? _validateConfirm(String? value) {
    if ((value ?? '') != _newController.text) {
      return '새 비밀번호와 일치하지 않습니다';
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    final submit = ref.watch(passwordSetupControllerProvider);
    final loggingOut = ref.watch(logoutControllerProvider).submitting;
    final name = ref.watch(authControllerProvider).user?.name;

    return PopScope(
      canPop: false,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('비밀번호 설정'),
          automaticallyImplyLeading: false,
          actions: [
            TextButton(
              onPressed: submit.submitting || loggingOut
                  ? null
                  : () => ref.read(logoutControllerProvider.notifier).submit(),
              child: Text(loggingOut ? '로그아웃 중…' : '로그아웃'),
            ),
          ],
        ),
        body: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpace.lg,
              vertical: AppSpace.xl,
            ),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    name == null ? '비밀번호를 설정하세요' : '$name 님, 비밀번호를 설정하세요',
                    style: AppText.title.copyWith(color: AppColors.textTitle),
                  ),
                  const SizedBox(height: AppSpace.sm),
                  // 안내문은 1문장 (02-patterns §5.4 — 앱은 시간 압박을 존중).
                  Text(
                    '임시 비밀번호는 최초 로그인에만 쓸 수 있습니다.',
                    style: AppText.caption.copyWith(
                      color: AppColors.textCaption,
                    ),
                  ),
                  const SizedBox(height: AppSpace.xl),
                  if (submit.hasError) ...[
                    AppBanner.danger(message: submit.errorMessage!),
                    const SizedBox(height: AppSpace.lg),
                  ],
                  AppTextField(
                    label: '임시 비밀번호',
                    controller: _currentController,
                    obscureText: true,
                    textInputAction: TextInputAction.next,
                    enabled: !submit.submitting,
                    validator: _validateCurrent,
                  ),
                  const SizedBox(height: AppSpace.lg),
                  AppTextField(
                    label: '새 비밀번호',
                    controller: _newController,
                    obscureText: true,
                    textInputAction: TextInputAction.next,
                    autofillHints: const [AutofillHints.newPassword],
                    enabled: !submit.submitting,
                    helperText:
                        '영문자와 숫자를 포함한 '
                        '${PasswordPolicy.minLength}~${PasswordPolicy.maxLength}자',
                    validator: _validateNew,
                  ),
                  const SizedBox(height: AppSpace.lg),
                  AppTextField(
                    label: '새 비밀번호 확인',
                    controller: _confirmController,
                    obscureText: true,
                    textInputAction: TextInputAction.done,
                    enabled: !submit.submitting,
                    validator: _validateConfirm,
                    onSubmitted: (_) => _submit(),
                  ),
                  const SizedBox(height: AppSpace.xl),
                  AppButton.cta(
                    label: '비밀번호 설정',
                    loading: submit.submitting,
                    onPressed: _submit,
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
