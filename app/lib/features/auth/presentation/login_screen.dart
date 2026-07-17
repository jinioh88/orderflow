import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

/// 로그인 화면 뼈대 (US-AUTH-03).
///
/// Phase 0에서는 라우팅 동작 확인용 껍데기만 둔다.
/// 입력 검증·AuthRepository 연동·에러 표시는 AUTH 태스크에서 구현한다.
class LoginScreen extends StatelessWidget {
  const LoginScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'OrderFlow',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.headlineLarge?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
              ),
              const SizedBox(height: 8),
              Text(
                '가맹점 발주',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 40),
              const TextField(
                keyboardType: TextInputType.emailAddress,
                decoration: InputDecoration(labelText: '이메일'),
              ),
              const SizedBox(height: 16),
              const TextField(
                obscureText: true,
                decoration: InputDecoration(labelText: '비밀번호'),
              ),
              const SizedBox(height: 24),
              FilledButton(
                // Phase 0 뼈대: 라우팅 확인용 임시 이동. 실제 로그인은 AUTH 태스크에서.
                onPressed: () => context.go('/catalog'),
                child: const Text('로그인'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
