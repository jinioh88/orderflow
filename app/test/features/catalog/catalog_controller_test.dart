import 'package:app/core/storage/key_value_store.dart';
import 'package:app/features/catalog/application/catalog_controller.dart';
import 'package:app/features/catalog/data/product_repository.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_backend.dart';

/// 카탈로그 목록의 상태 전이 — 최초 로드 · 페이징 · 검색 (US-CAT-01).
void main() {
  late FakeBackend backend;
  late InMemoryKeyValueStore storage;

  setUp(() {
    backend = FakeBackend();
    storage = InMemoryKeyValueStore();
  });

  ProviderContainer containerWith() => ProviderContainer.test(
    overrides: [
      backend.override,
      keyValueStoreProvider.overrideWithValue(storage),
    ],
  );

  Map<String, dynamic> productJson(int id, {String name = '상품'}) => {
    'id': id,
    'productCode': 'P-$id',
    'name': '$name$id',
    'barcode': '880$id',
    'category': '냉동식품',
    'orderUnit': 'BOX',
    'unitPrice': 12000,
    'limited': false,
    'availableQty': null,
    'status': 'ON_SALE',
  };

  Map<String, dynamic> pageOf(
    List<int> ids, {
    int number = 0,
    int totalPages = 1,
    String name = '상품',
  }) => {
    'items': [for (final id in ids) productJson(id, name: name)],
    'page': {
      'number': number,
      'size': 20,
      'totalElements': ids.length,
      'totalPages': totalPages,
    },
  };

  /// 컨트롤러의 첫 조회가 끝날 때까지 기다린다 (build에서 microtask로 시작한다).
  Future<CatalogState> loaded(ProviderContainer container) async {
    container.read(catalogControllerProvider);
    while (container.read(catalogControllerProvider).phase ==
        CatalogPhase.loading) {
      await Future<void>.delayed(Duration.zero);
    }
    return container.read(catalogControllerProvider);
  }

  group('최초 로드', () {
    test('로드 성공 시 목록을 채우고 ready가 된다', () async {
      backend.stub('/products', () => okBody(pageOf([1, 2, 3])));

      final state = await loaded(containerWith());

      expect(state.phase, CatalogPhase.ready);
      expect(state.items.map((p) => p.id), [1, 2, 3]);
      expect(state.hasMore, isFalse);
      expect(state.errorMessage, isNull);
    });

    test('판매중지 품목은 아예 조회하지 않는다 (04 §3.1)', () async {
      backend.stub('/products', () => okBody(pageOf([1])));

      await loaded(containerWith());

      expect(
        backend.requestsTo('/products').single.queryParameters['status'],
        'ON_SALE',
      );
    });

    test('실패하면 error 상태와 문구를 남긴다', () async {
      backend.stub(
        '/products',
        () => errorBody('INTERNAL_ERROR', 500, message: '일시적인 오류'),
      );

      final state = await loaded(containerWith());

      expect(state.phase, CatalogPhase.error);
      expect(state.items, isEmpty);
      expect(state.errorMessage, '일시적인 오류');
    });

    test('여러 페이지가 있으면 hasMore가 켜진다', () async {
      backend.stub(
        '/products',
        () => okBody(pageOf([1, 2], totalPages: 3)),
      );

      final state = await loaded(containerWith());

      expect(state.hasMore, isTrue);
    });
  });

  group('무한 스크롤 (페이징)', () {
    test('다음 페이지를 이어 붙이고 마지막이면 hasMore를 끈다', () async {
      backend.on(
        '/products',
        (options, calls) => okBody(
          calls == 1
              ? pageOf([1, 2], number: 0, totalPages: 2)
              : pageOf([3, 4], number: 1, totalPages: 2),
        ),
      );
      final container = containerWith();
      await loaded(container);

      await container.read(catalogControllerProvider.notifier).loadMore();
      final state = container.read(catalogControllerProvider);

      expect(state.items.map((p) => p.id), [1, 2, 3, 4]);
      expect(state.hasMore, isFalse);
      expect(backend.requestsTo('/products')[1].queryParameters['page'], 1);
    });

    test('마지막 페이지에서는 더 부르지 않는다', () async {
      backend.stub('/products', () => okBody(pageOf([1])));
      final container = containerWith();
      await loaded(container);

      await container.read(catalogControllerProvider.notifier).loadMore();

      expect(backend.callsTo('/products'), 1);
    });

    test('이어받기 실패는 이미 받은 목록을 지우지 않는다 (02 §2.3 부분 실패)', () async {
      backend.on(
        '/products',
        (options, calls) => calls == 1
            ? okBody(pageOf([1, 2], totalPages: 2))
            : errorBody('INTERNAL_ERROR', 500, message: '일시적인 오류'),
      );
      final container = containerWith();
      await loaded(container);

      await container.read(catalogControllerProvider.notifier).loadMore();
      final state = container.read(catalogControllerProvider);

      expect(state.phase, CatalogPhase.ready);
      expect(state.items.map((p) => p.id), [1, 2]);
      expect(state.loadingMore, isFalse);
      expect(state.errorMessage, '일시적인 오류');
    });

    test('검색으로 목록이 바뀌면 페이지 번호가 0부터 다시 시작한다', () async {
      backend.on(
        '/products',
        (options, calls) => okBody(pageOf([calls], totalPages: 2)),
      );
      final container = containerWith();
      await loaded(container);
      // 1페이지까지 받아 둔 상태에서 검색어를 바꾼다.
      await container.read(catalogControllerProvider.notifier).loadMore();

      await container.read(catalogControllerProvider.notifier).load(
        keyword: '만두',
      );
      await container.read(catalogControllerProvider.notifier).loadMore();

      final pages = backend
          .requestsTo('/products')
          .map((r) => r.queryParameters['page'])
          .toList();
      expect(pages, [0, 1, 0, 1]);
    });
  });

  group('검색', () {
    test('디바운스 시간이 지나야 조회한다', () async {
      backend.stub('/products', () => okBody(pageOf([1])));
      final container = containerWith();
      await loaded(container);

      container.read(catalogControllerProvider.notifier).search('만두');
      expect(backend.callsTo('/products'), 1, reason: '아직 조회하면 안 된다');

      await Future<void>.delayed(
        CatalogController.searchDebounce + const Duration(milliseconds: 50),
      );

      expect(backend.callsTo('/products'), 2);
      expect(
        backend.requestsTo('/products').last.queryParameters['keyword'],
        '만두',
      );
    });

    test('연속 입력은 마지막 것 한 번만 조회한다', () async {
      backend.stub('/products', () => okBody(pageOf([1])));
      final container = containerWith();
      await loaded(container);
      final controller = container.read(catalogControllerProvider.notifier);

      controller.search('만');
      controller.search('만두');
      controller.search('만두 1kg');
      await Future<void>.delayed(
        CatalogController.searchDebounce + const Duration(milliseconds: 50),
      );

      expect(backend.callsTo('/products'), 2);
      expect(
        backend.requestsTo('/products').last.queryParameters['keyword'],
        '만두 1kg',
      );
    });

    test('같은 검색어를 다시 입력하면 조회하지 않는다', () async {
      backend.stub('/products', () => okBody(pageOf([1])));
      final container = containerWith();
      await loaded(container);
      final controller = container.read(catalogControllerProvider.notifier);

      controller.search('만두');
      await Future<void>.delayed(
        CatalogController.searchDebounce + const Duration(milliseconds: 50),
      );
      controller.search('만두');
      await Future<void>.delayed(
        CatalogController.searchDebounce + const Duration(milliseconds: 50),
      );

      expect(backend.callsTo('/products'), 2);
    });

    test('검색 중에는 기존 목록을 지우지 않는다 (02 §2.2 재조회)', () async {
      backend.on(
        '/products',
        (options, calls) => okBody(pageOf(calls == 1 ? [1, 2] : [3])),
      );
      final container = containerWith();
      await loaded(container);

      final future = container.read(catalogControllerProvider.notifier).load(
        keyword: '만두',
      );
      final during = container.read(catalogControllerProvider);

      expect(during.phase, CatalogPhase.ready, reason: '화면을 로딩으로 덮지 않는다');
      expect(during.items.map((p) => p.id), [1, 2]);
      expect(during.refreshing, isTrue);

      await future;
      expect(container.read(catalogControllerProvider).refreshing, isFalse);
    });

    test('검색어를 지우면 필터 없는 목록으로 돌아간다', () async {
      backend.stub('/products', () => okBody(pageOf([1])));
      final container = containerWith();
      await loaded(container);
      final controller = container.read(catalogControllerProvider.notifier);

      await controller.load(keyword: '만두');
      await controller.clearKeyword();

      expect(container.read(catalogControllerProvider).keyword, '');
      expect(
        backend.requestsTo('/products').last.queryParameters.containsKey(
          'keyword',
        ),
        isFalse,
      );
    });

    test('늦게 도착한 이전 검색 응답이 최신 결과를 덮어쓰지 않는다', () async {
      // 첫 검색은 느리게, 두 번째 검색은 즉시 응답한다.
      backend.on('/products', (options, calls) => okBody(pageOf([calls])));
      final container = containerWith();
      await loaded(container);
      final controller = container.read(catalogControllerProvider.notifier);

      final slow = controller.load(keyword: '느린');
      final fast = controller.load(keyword: '빠른');
      await Future.wait([slow, fast]);

      final state = container.read(catalogControllerProvider);
      expect(state.keyword, '빠른');
      expect(state.items.map((p) => p.id), [3], reason: '3번째 호출 = 마지막 검색');
    });
  });

  test('기본 정렬은 가나다순이다 (스펙 3.2.2 허용 필드)', () async {
    backend.stub('/products', () => okBody(pageOf([1])));

    await loaded(containerWith());

    expect(
      backend.requestsTo('/products').single.queryParameters['sort'],
      ProductRepository.defaultSort,
    );
  });
}
