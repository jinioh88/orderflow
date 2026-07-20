import 'package:flutter/material.dart';

import '../../../core/widgets/status_views.dart';

/// 더보기 화면 (04-app-components §1.1 — 더보기 탭).
///
/// 입고 확인(M4)·재고(v1.1)·알림 설정·계정이 들어갈 자리.
/// 입고 확인의 **주 진입로는 홈의 '오늘 도착 예정' 카드**이고 여기는 보조 진입로다.
class MoreScreen extends StatelessWidget {
  const MoreScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('더보기')),
      body: const EmptyView(
        icon: Icons.menu,
        title: '더보기 메뉴는 준비 중입니다',
        description: '입고 확인·알림 설정·계정이 이 자리에 들어갑니다',
      ),
    );
  }
}
