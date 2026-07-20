import 'package:flutter/material.dart';

import '../../../core/widgets/status_views.dart';

/// 카탈로그 화면 뼈대 (US-CAT-01~04 조회 측, 발주 탭).
///
/// 검색 바·카테고리 칩·상품 행 카드(04-app-components §3.1)는 CAT 태스크에서 구현한다.
/// 지금은 빈 상태 뷰만 노출한다.
class CatalogScreen extends StatelessWidget {
  const CatalogScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('상품 카탈로그')),
      body: const EmptyView(
        icon: Icons.sell,
        title: '카탈로그를 준비 중입니다',
        description: '검색·카테고리 탐색은 CAT 태스크에서 붙습니다',
      ),
    );
  }
}
