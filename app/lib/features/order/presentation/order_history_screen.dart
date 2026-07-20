import 'package:flutter/material.dart';

import '../../../core/widgets/status_views.dart';

/// 발주 내역 화면 (04-app-components §1.1 — 내역 탭, US-ORD-05).
///
/// 발주 카드 목록·상태 타임라인이 들어갈 자리. M1 범위 밖이라 지금은 껍데기다.
class OrderHistoryScreen extends StatelessWidget {
  const OrderHistoryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('발주 내역')),
      body: const EmptyView(
        icon: Icons.receipt_long,
        title: '발주 내역이 없습니다',
        description: '발주를 제출하면 여기에서 진행 상태를 볼 수 있습니다',
      ),
    );
  }
}
