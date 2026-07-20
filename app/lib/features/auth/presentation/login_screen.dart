import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/tokens.dart';
import '../../../core/widgets/app_button.dart';
import '../../../core/widgets/app_shell.dart';
import '../../../core/widgets/app_text_field.dart';

/// 로그인 화면 (US-AUTH-03).
///
/// 디자인 시스템: 04-app-components §2(버튼·인풋) + 02-patterns §3(폼).
/// 라벨은 인풋 위, 검증은 블러/제출 시점 — 그 규칙은 [AppTextField]가 갖고 있다.
///
/// AuthRepository 연동·에러 코드 분기·토큰 저장은 AUTH 태스크에서 붙인다.
/// 지금은 화면 골격이라 제출이 곧바로 발주 탭으로 이동한다.
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _submit() {
    // 제출 시 전체 검증 (02-patterns §3).
    if (!(_formKey.currentState?.validate() ?? false)) return;
    context.go(AppTab.order.path);
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
                  AppTextField(
                    label: '이메일',
                    controller: _emailController,
                    hint: 'name@company.com',
                    keyboardType: TextInputType.emailAddress,
                    textInputAction: TextInputAction.next,
                    autofillHints: const [AutofillHints.username],
                    validator: _validateEmail,
                  ),
                  const SizedBox(height: AppSpace.lg),
                  AppTextField(
                    label: '비밀번호',
                    controller: _passwordController,
                    obscureText: true,
                    textInputAction: TextInputAction.done,
                    autofillHints: const [AutofillHints.password],
                    validator: _validatePassword,
                    onSubmitted: (_) => _submit(),
                  ),
                  const SizedBox(height: AppSpace.xl),
                  AppButton.cta(label: '로그인', onPressed: _submit),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
