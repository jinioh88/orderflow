import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/tokens.dart';
import '../../../core/widgets/app_button.dart';
import '../../../core/widgets/status_views.dart';
import '../application/catalog_controller.dart';
import 'catalog_search_bar.dart';
import 'product_row_card.dart';

/// 카탈로그 화면 (US-CAT-01 조회 측, 발주 탭).
///
/// 04-app-components §3.1 기준 — 상단 고정 검색 바 + 상품 행 카드 목록.
/// 카테고리 칩은 카테고리 목록 API가 없어 보류 상태다
/// (`수정요청/20260813-app-product-categories.md`, 사용자 결정 2026-08-13).
class CatalogScreen extends ConsumerWidget {
  const CatalogScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(catalogControllerProvider);
    final controller = ref.read(catalogControllerProvider.notifier);

    return Scaffold(
      appBar: AppBar(
        title: const Text('상품 카탈로그'),
        // 재조회 중임을 데이터 위에 겹쳐 알린다 — 화면을 로딩으로 덮지 않는다(02 §2.2).
        actions: [
          if (state.refreshing)
            const Padding(
              padding: EdgeInsets.only(right: AppSpace.lg),
              child: Center(
                child: SizedBox(
                  width: AppIconSize.inline,
                  height: AppIconSize.inline,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
              ),
            ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(
              AppSpace.lg,
              AppSpace.sm,
              AppSpace.lg,
              AppSpace.md,
            ),
            child: CatalogSearchBar(
              initialText: state.keyword,
              onChanged: controller.search,
            ),
          ),
          Expanded(child: _CatalogBody(state: state, controller: controller)),
        ],
      ),
    );
  }
}

class _CatalogBody extends StatelessWidget {
  const _CatalogBody({required this.state, required this.controller});

  final CatalogState state;
  final CatalogController controller;

  @override
  Widget build(BuildContext context) {
    return switch (state.phase) {
      CatalogPhase.loading => const SkeletonList(),
      CatalogPhase.error => ErrorView(
        message: state.errorMessage ?? '',
        onRetry: controller.load,
      ),
      CatalogPhase.ready when state.isEmpty => _EmptyCatalog(
        isFiltered: state.isFiltered,
        onClearKeyword: controller.clearKeyword,
      ),
      CatalogPhase.ready => _ProductList(
        state: state,
        onLoadMore: controller.loadMore,
      ),
    };
  }
}

/// 빈 상태 (02-patterns §2.1).
///
/// **검색 결과 없음을 "데이터 없음"처럼 보여주지 않는다** — 검색 때문에 비었으면
/// 그 사실을 제목에서 밝히고 검색어를 지울 수단을 준다.
class _EmptyCatalog extends StatelessWidget {
  const _EmptyCatalog({required this.isFiltered, required this.onClearKeyword});

  final bool isFiltered;
  final VoidCallback onClearKeyword;

  @override
  Widget build(BuildContext context) {
    if (!isFiltered) {
      return const EmptyView(
        icon: Icons.sell_outlined,
        title: '등록된 상품이 없습니다',
        description: '본사에서 상품을 등록하면 여기에 표시됩니다',
      );
    }
    return EmptyView(
      icon: Icons.search_off,
      title: '검색 결과가 없습니다',
      description: '품명·품목코드·바코드로 다시 찾아보세요',
      action: AppButton.secondary(
        label: '검색어 지우기',
        icon: Icons.close,
        onPressed: onClearKeyword,
      ),
    );
  }
}

class _ProductList extends StatefulWidget {
  const _ProductList({required this.state, required this.onLoadMore});

  final CatalogState state;
  final VoidCallback onLoadMore;

  @override
  State<_ProductList> createState() => _ProductListState();
}

class _ProductListState extends State<_ProductList> {
  final _scrollController = ScrollController();

  /// 바닥에 닿기 전에 미리 다음 페이지를 부른다 — 마감 직전에 빈 화면을 기다리게 하지 않는다.
  static const _preloadExtent = 400.0;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    final position = _scrollController.position;
    if (position.pixels >= position.maxScrollExtent - _preloadExtent) {
      // 중복 호출 방어는 컨트롤러가 한다 (loadingMore·hasMore 확인).
      widget.onLoadMore();
    }
  }

  @override
  Widget build(BuildContext context) {
    final items = widget.state.items;
    // 목록 끝의 진행 표시 한 칸.
    final footerCount = widget.state.hasMore ? 1 : 0;

    return ListView.separated(
      controller: _scrollController,
      padding: const EdgeInsets.fromLTRB(
        AppSpace.lg,
        0,
        AppSpace.lg,
        AppSpace.xl,
      ),
      itemCount: items.length + footerCount,
      separatorBuilder: (_, _) => const SizedBox(height: AppSpace.md),
      itemBuilder: (context, index) {
        if (index < items.length) return ProductRowCard(items[index]);
        return const _LoadMoreIndicator();
      },
    );
  }
}

class _LoadMoreIndicator extends StatelessWidget {
  const _LoadMoreIndicator();

  @override
  Widget build(BuildContext context) {
    return const Padding(
      padding: EdgeInsets.symmetric(vertical: AppSpace.lg),
      child: Center(
        child: SizedBox(
          width: AppIconSize.inline,
          height: AppIconSize.inline,
          child: CircularProgressIndicator(strokeWidth: 2),
        ),
      ),
    );
  }
}
