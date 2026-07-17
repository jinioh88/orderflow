import 'package:flutter/material.dart';

import '../../../core/widgets/status_views.dart';

/// 카탈로그 화면 뼈대 (US-CAT-01~04 조회 측).
///
/// Phase 0에서는 빈 상태 뷰만 노출한다. 목록·검색·카테고리는 CAT 태스크에서 구현한다.
class CatalogScreen extends StatelessWidget {
  const CatalogScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('상품 카탈로그')),
      body: const EmptyView(message: '카탈로그 화면 (CAT 태스크에서 구현)'),
    );
  }
}
