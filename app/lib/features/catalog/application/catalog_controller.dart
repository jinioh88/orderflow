import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/app_exception.dart';
import '../data/product.dart';
import '../data/product_repository.dart';

/// 목록 화면의 큰 상태 (02-patterns §2).
///
/// "이미 데이터가 있는 상태에서의 재조회"는 [ready]로 유지한다 — 화면 전체를 로딩으로
/// 덮지 않기 위해서다(§2.2). 그 경우 [CatalogState.refreshing]이 켜진다.
enum CatalogPhase { loading, ready, error }

final class CatalogState {
  const CatalogState({
    this.phase = CatalogPhase.loading,
    this.items = const [],
    this.keyword = '',
    this.errorMessage,
    this.refreshing = false,
    this.loadingMore = false,
    this.hasMore = false,
  });

  final CatalogPhase phase;
  final List<Product> items;

  /// 목록에 **반영된** 검색어. 입력 중인 문자열이 아니라 마지막으로 조회에 쓴 값이다.
  final String keyword;

  final String? errorMessage;

  /// 데이터를 유지한 채 다시 조회하는 중 (우상단 미세 스피너).
  final bool refreshing;

  /// 다음 페이지를 이어 받는 중 (목록 하단 스피너).
  final bool loadingMore;

  /// 다음 페이지가 남아 있는지.
  final bool hasMore;

  bool get isEmpty => items.isEmpty;

  /// 검색 결과가 없는 것과 카탈로그 자체가 빈 것을 화면이 구분해 보여줘야 한다(§2.1).
  bool get isFiltered => keyword.isNotEmpty;

  CatalogState copyWith({
    CatalogPhase? phase,
    List<Product>? items,
    String? keyword,
    String? errorMessage,
    bool? refreshing,
    bool? loadingMore,
    bool? hasMore,
  }) => CatalogState(
    phase: phase ?? this.phase,
    items: items ?? this.items,
    keyword: keyword ?? this.keyword,
    // 에러는 다음 상태로 끌고 가지 않는다 — 남기려면 매번 명시해야 한다.
    errorMessage: errorMessage,
    refreshing: refreshing ?? this.refreshing,
    loadingMore: loadingMore ?? this.loadingMore,
    hasMore: hasMore ?? this.hasMore,
  );
}

/// 카탈로그 목록 컨트롤러 (US-CAT-01 조회 측).
///
/// 화면은 스크롤 위치와 입력만 알려주고, 무엇을 언제 부를지는 전부 여기서 정한다
/// (app/CLAUDE.md 구현 원칙 6).
///
/// **판매중지 품목은 목록에 넣지 않는다** — `status=ON_SALE`로만 조회한다
/// (04-app-components §3.1 "판매중지 품목은 목록에서 제외", 사용자 결정 2026-08-13).
/// M2에서 이 화면이 그대로 발주 화면이 되므로, 담을 수 없는 품목을 애초에 보여주지 않는다.
class CatalogController extends Notifier<CatalogState> {
  /// 타이핑이 멈춘 뒤 조회까지 기다리는 시간.
  ///
  /// 한 글자마다 요청을 보내면 마감 직전 느린 회선에서 응답이 뒤엉킨다.
  static const searchDebounce = Duration(milliseconds: 300);

  Timer? _debounce;

  /// 조회 세대 번호. 늦게 도착한 응답이 최신 결과를 덮어쓰는 것을 막는다
  /// (검색어를 빠르게 바꾸면 이전 요청이 나중에 도착할 수 있다).
  int _generation = 0;

  int _nextPage = 0;

  @override
  CatalogState build() {
    ref.onDispose(() => _debounce?.cancel());
    // 첫 조회는 화면이 붙은 뒤에 시작한다 — build 안에서 상태를 바꾸면 안 된다.
    Future.microtask(load);
    return const CatalogState();
  }

  /// 첫 페이지 조회. [keyword]를 주면 검색어를 바꿔 다시 조회한다.
  Future<void> load({String? keyword}) async {
    final query = keyword ?? state.keyword;
    final generation = ++_generation;
    final hadItems = state.items.isNotEmpty;

    state = state.copyWith(
      // 이미 보여줄 데이터가 있으면 화면을 비우지 않는다 (§2.2 재조회).
      phase: hadItems ? CatalogPhase.ready : CatalogPhase.loading,
      refreshing: hadItems,
      keyword: query,
      loadingMore: false,
    );

    try {
      final page = await ref
          .read(productRepositoryProvider)
          .list(keyword: query, status: ProductStatus.onSale);
      if (!_isCurrent(generation)) return;

      _nextPage = 1;
      state = state.copyWith(
        phase: CatalogPhase.ready,
        items: page.items,
        refreshing: false,
        hasMore: !page.isLast,
      );
    } catch (e) {
      if (!_isCurrent(generation)) return;
      state = state.copyWith(
        phase: CatalogPhase.error,
        items: const [],
        refreshing: false,
        hasMore: false,
        errorMessage: _messageOf(e),
      );
    }
  }

  /// 검색어 입력 — 디바운스 후 첫 페이지부터 다시 조회한다.
  void search(String keyword) {
    final query = keyword.trim();
    _debounce?.cancel();
    if (query == state.keyword) return;
    _debounce = Timer(searchDebounce, () => load(keyword: query));
  }

  /// 다음 페이지 이어받기 (무한 스크롤).
  ///
  /// 중복 호출·마지막 페이지·에러 상태에서는 아무것도 하지 않는다 — 스크롤 이벤트가
  /// 연달아 들어와도 요청이 겹치지 않게 하는 것이 이 화면의 유일한 동시성 방어다.
  Future<void> loadMore() async {
    if (state.loadingMore || !state.hasMore || state.phase != CatalogPhase.ready) {
      return;
    }
    final generation = _generation;
    state = state.copyWith(loadingMore: true);

    try {
      final page = await ref
          .read(productRepositoryProvider)
          .list(
            keyword: state.keyword,
            status: ProductStatus.onSale,
            page: _nextPage,
          );
      if (!_isCurrent(generation)) return;

      _nextPage += 1;
      state = state.copyWith(
        items: [...state.items, ...page.items],
        loadingMore: false,
        hasMore: !page.isLast,
      );
    } catch (e) {
      if (!_isCurrent(generation)) return;
      // 이어받기 실패는 화면을 죽이지 않는다 (§2.3 부분 실패) — 이미 받은 목록은
      // 그대로 두고 하단에만 재시도 여지를 남긴다.
      state = state.copyWith(loadingMore: false, errorMessage: _messageOf(e));
    }
  }

  /// 검색어를 지우고 처음부터 다시 조회한다 (빈 상태의 "검색어 지우기").
  Future<void> clearKeyword() => load(keyword: '');

  bool _isCurrent(int generation) => generation == _generation && ref.mounted;

  /// 목록 화면의 에러는 코드별로 사용자 행동이 갈리지 않는다 — 어느 쪽이든 "다시 시도"다.
  /// 그래서 AUTH처럼 코드별 문구 분기를 두지 않고 예외의 표시용 메시지를 그대로 쓴다.
  String _messageOf(Object error) => error is AppException
      ? error.message
      : '목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요';
}

final catalogControllerProvider =
    NotifierProvider<CatalogController, CatalogState>(CatalogController.new);
