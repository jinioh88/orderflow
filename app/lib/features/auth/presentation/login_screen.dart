import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/tokens.dart';
import '../../../core/widgets/app_banner.dart';
import '../../../core/widgets/app_button.dart';
import '../../../core/widgets/app_text_field.dart';
import '../application/login_controller.dart';

/// 로그인 화면 (US-AUTH-03).
///
/// 디자인 시스템: 04-app-components §2(버튼·인풋) + 02-patterns §3(폼).
/// 라벨은 인풋 위, 검증은 블러/제출 시점 — 그 규칙은 [AppTextField]가 갖고 있다.
///
/// 이 위젯은 입력 수집과 표시만 한다. 로그인 호출·에러 문구 변환은
/// [LoginController]가, 성공 후 화면 이동은 라우터 가드가 맡는다
/// (app/CLAUDE.md 구현 원칙 6).
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _emailFocus = FocusNode();
  final _passwordFocus = FocusNode();

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _emailFocus.dispose();
    _passwordFocus.dispose();
    super.dispose();
  }

  void _submit() {
    // 제출 시 전체 검증 (02-patterns §3).
    if (!(_formKey.currentState?.validate() ?? false)) {
      // 첫 에러 필드로 포커스 (02-patterns §3).
      if (_validateEmail(_emailController.text) != null) {
        _emailFocus.requestFocus();
      } else {
        _passwordFocus.requestFocus();
      }
      return;
    }
    // 키보드를 내려서 에러 배너·로딩이 가려지지 않게 한다.
    FocusScope.of(context).unfocus();
    ref
        .read(loginControllerProvider.notifier)
        .submit(
          email: _emailController.text,
          password: _passwordController.text,
        );
  }

  /// 검증 메시지는 "원인 + 해결" 톤 (02-patterns §3.1).
  String? _validateEmail(String? value) {
    final email = value?.trim() ?? '';
    if (email.isEmpty) return '이메일을 입력하세요';
    if (!email.contains('@') || !email.contains('.')) {
      return '이메일 형식이 아닙니다 (예: name@company.com)';
    }
    return null;
  }

  String? _validatePassword(String? value) {
    if ((value ?? '').isEmpty) return '비밀번호를 입력하세요';
    return null;
  }

  @override
  Widget build(BuildContext context) {
    final submit = ref.watch(loginControllerProvider);

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpace.lg,
              vertical: AppSpace.xxl,
            ),
            child: Form(
              key: _formKey,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    'OrderFlow',
                    textAlign: TextAlign.center,
                    style: AppText.display.copyWith(
                      color: AppColors.primaryStrong,
                    ),
                  ),
                  const SizedBox(height: AppSpace.sm),
                  Text(
                    '가맹점 발주',
                    textAlign: TextAlign.center,
                    style: AppText.body.copyWith(color: AppColors.textCaption),
                  ),
                  const SizedBox(height: AppSpace.xxxl),
                  // 서버가 준 전역 오류(자격 증명 불일치·계정 중지 등)는 폼 상단 배너에
                  // 둔다 — 특정 필드의 문제가 아니기 때문이다 (02-patterns §3.1).
                  if (submit.hasError) ...[
                    AppBanner.danger(message: submit.errorMessage!),
                    const SizedBox(height: AppSpace.lg),
                  ],
                  AppTextField(
                    label: '이메일',
                    controller: _emailController,
                    focusNode: _emailFocus,
                    hint: 'name@company.com',
                    keyboardType: TextInputType.emailAddress,
                    textInputAction: TextInputAction.next,
                    autofillHints: const [AutofillHints.username],
                    enabled: !submit.submitting,
                    validator: _validateEmail,
                    onSubmitted: (_) => _passwordFocus.requestFocus(),
                  ),
                  const SizedBox(height: AppSpace.lg),
                  AppTextField(
                    label: '비밀번호',
                    controller: _passwordController,
                    focusNode: _passwordFocus,
                    obscureText: true,
                    textInputAction: TextInputAction.done,
                    autofillHints: const [AutofillHints.password],
                    enabled: !submit.submitting,
                    validator: _validatePassword,
                    onSubmitted: (_) => _submit(),
                  ),
                  const SizedBox(height: AppSpace.xl),
                  AppButton.cta(
                    label: '로그인',
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
