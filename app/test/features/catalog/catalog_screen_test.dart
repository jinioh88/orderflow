import 'package:app/core/storage/key_value_store.dart';
import 'package:app/core/theme/app_theme.dart';
import 'package:app/features/catalog/presentation/catalog_screen.dart';
import 'package:app/features/catalog/presentation/product_row_card.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_backend.dart';

/// 카탈로그 화면의 상태별 표시 (02-patterns §2 · 04-app-components §3.1).
void main() {
  late FakeBackend backend;

  setUp(() => backend = FakeBackend());

  Map<String, dynamic> productJson(
    int id, {
    String name = '김치만두 1kg',
    bool limited = false,
    int? availableQty,
  }) => {
    'id': id,
    'productCode': 'P-$id',
    'name': name,
    'barcode': '880$id',
    'category': '냉동식품',
    'orderUnit': 'BOX',
    'unitPrice': 12000,
    'limited': limited,
    'availableQty': availableQty,
    'status': 'ON_SALE',
  };

  Map<String, dynamic> pageOf(List<Map<String, dynamic>> items) => {
    'items': items,
    'page': {
      'number': 0,
      'size': 20,
      'totalElements': items.length,
      'totalPages': items.isEmpty ? 0 : 1,
    },
  };

  /// 페이지 번호·총 페이지를 직접 지정하는 형태 (무한 스크롤 검증용).
  Map<String, dynamic> pageOfWith(
    List<Map<String, dynamic>> items, {
    int number = 0,
    required int totalPages,
  }) => {
    'items': items,
    'page': {
      'number': number,
      'size': 20,
      'totalElements': items.length * totalPages,
      'totalPages': totalPages,
    },
  };

  /// 스켈레톤 셔머가 무한 반복이라 `pumpAndSettle`을 쓸 수 없다.
  /// 대신 몇 프레임을 직접 돌려 비동기 응답과 300ms 지연을 넘긴다.
  Future<void> settle(WidgetTester tester) async {
    for (var i = 0; i < 6; i++) {
      await tester.pump(const Duration(milliseconds: 100));
    }
  }

  Future<void> pumpCatalog(WidgetTester tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          backend.override,
          keyValueStoreProvider.overrideWithValue(InMemoryKeyValueStore()),
        ],
        child: MaterialApp(theme: AppTheme.light(), home: const CatalogScreen()),
      ),
    );
    await settle(tester);
  }

  testWidgets('목록을 상품 행 카드로 그린다', (tester) async {
    backend.stub(
      '/products',
      () => okBody(
        pageOf([
          productJson(1, name: '김치만두 1kg'),
          productJson(2, name: '군만두 500g'),
        ]),
      ),
    );

    await pumpCatalog(tester);

    expect(find.byType(ProductRowCard), findsNWidgets(2));
    expect(find.text('김치만두 1kg'), findsOneWidget);
    expect(find.text('₩12,000 / BOX'), findsNWidgets(2));
  });

  testWidgets('한정 품목은 뱃지와 잔여 수량을 함께 보여준다 (04 §3.1)', (tester) async {
    backend.stub(
      '/products',
      () => okBody(
        pageOf([productJson(1, limited: true, availableQty: 1200)]),
      ),
    );

    await pumpCatalog(tester);

    expect(find.text('한정'), findsOneWidget);
    expect(find.text('₩12,000 / BOX · 잔여 1,200'), findsOneWidget);
  });

  testWidgets('카탈로그가 비면 "등록된 상품이 없습니다"', (tester) async {
    backend.stub('/products', () => okBody(pageOf([])));

    await pumpCatalog(tester);

    expect(find.text('등록된 상품이 없습니다'), findsOneWidget);
    expect(find.text('검색어 지우기'), findsNothing);
  });

  testWidgets('검색 결과 없음은 빈 카탈로그와 다르게 보여준다 (02 §2.1)', (tester) async {
    backend.on(
      '/products',
      (options, calls) =>
          okBody(pageOf(calls == 1 ? [productJson(1)] : [])),
    );
    await pumpCatalog(tester);

    await tester.enterText(find.byType(TextField), '없는상품');
    await settle(tester);

    expect(find.text('검색 결과가 없습니다'), findsOneWidget);
    expect(find.text('검색어 지우기'), findsOneWidget);
    expect(find.text('등록된 상품이 없습니다'), findsNothing);
  });

  testWidgets('"검색어 지우기"를 누르면 전체 목록으로 돌아온다', (tester) async {
    backend.on(
      '/products',
      (options, calls) => okBody(
        pageOf(
          options.queryParameters.containsKey('keyword')
              ? []
              : [productJson(1)],
        ),
      ),
    );
    await pumpCatalog(tester);

    await tester.enterText(find.byType(TextField), '없는상품');
    await settle(tester);
    await tester.tap(find.text('검색어 지우기'));
    await settle(tester);

    expect(find.byType(ProductRowCard), findsOneWidget);
  });

  testWidgets('다음 페이지가 남아 있어도 받는 중이 아니면 스피너를 띄우지 않는다', (tester) async {
    // 한 화면을 넘기는 분량이라 자동 이어받기가 걸리지 않는다 — 가만히 있는 상태다.
    backend.stub(
      '/products',
      () => okBody(
        pageOfWith(
          [for (var i = 1; i <= 20; i++) productJson(i, name: '상품 $i')],
          totalPages: 5,
        ),
      ),
    );

    await pumpCatalog(tester);

    expect(find.byType(CircularProgressIndicator), findsNothing);
  });

  testWidgets('첫 페이지가 화면을 못 채우면 스크롤 없이도 다음 페이지를 받는다', (tester) async {
    // 스크롤이 불가능한 짧은 목록에서 스크롤 이벤트만 기다리면 영영 멈춰 있게 된다.
    backend.on(
      '/products',
      (options, calls) => okBody(
        calls == 1
            ? pageOfWith([
                productJson(1),
                productJson(2),
              ], number: 0, totalPages: 2)
            : pageOfWith([
                productJson(3),
                productJson(4),
              ], number: 1, totalPages: 2),
      ),
    );

    await pumpCatalog(tester);
    await settle(tester);

    expect(backend.callsTo('/products'), 2);
    expect(find.byType(ProductRowCard), findsNWidgets(4));
  });

  testWidgets('로드 실패는 에러 뷰 + 다시 시도 (02 §2.3)', (tester) async {
    backend.on(
      '/products',
      (options, calls) => calls == 1
          ? errorBody('INTERNAL_ERROR', 500, message: '일시적인 오류가 발생했습니다.')
          : okBody(pageOf([productJson(1)])),
    );

    await pumpCatalog(tester);

    expect(find.text('불러오지 못했습니다'), findsOneWidget);
    expect(find.text('일시적인 오류가 발생했습니다.'), findsOneWidget);

    await tester.tap(find.text('다시 시도'));
    await settle(tester);

    expect(find.byType(ProductRowCard), findsOneWidget);
  });
}
