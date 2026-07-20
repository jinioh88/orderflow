import 'package:flutter/material.dart';

import '../../../core/widgets/status_views.dart';

/// 홈 화면 (04-app-components §1.1 — 홈 탭).
///
/// 마감 카운트다운·오늘 발주 요약·템플릿 바로가기·'오늘 도착 예정' 카드가 들어갈 자리.
/// M1 범위 밖이라 지금은 탭 구조 확인용 껍데기다 (ORD/GRN 태스크에서 구현).
class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('홈')),
      body: const EmptyView(
        icon: Icons.alarm,
        title: '홈 화면은 준비 중입니다',
        description: '마감 카운트다운과 발주 요약이 이 자리에 들어갑니다',
      ),
    );
  }
}
