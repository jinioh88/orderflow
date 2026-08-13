import 'package:app/core/network/api_client.dart';
import 'package:app/core/network/app_exception.dart';
import 'package:app/features/catalog/data/product.dart';
import 'package:app/features/catalog/data/product_repository.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_backend.dart';

/// CAT 목록 API 요청·응답 정합 (api-spec 3.2.2 · 3.1).
void main() {
  late FakeBackend backend;
  late ProductRepository repository;

  setUp(() {
    backend = FakeBackend();
    repository = ProductRepository(
      ApiClient(
        dio: Dio(ApiClient.defaultOptions)..httpClientAdapter = backend.adapter,
      ),
    );
  });

  Map<String, dynamic> productJson({
    int id = 100,
    bool limited = false,
    int? availableQty,
    String status = 'ON_SALE',
  }) => {
    'id': id,
    'productCode': 'P-000$id',
    'name': '김치만두 1kg',
    'barcode': '8801234567890',
    'category': '냉동식품',
    'orderUnit': 'BOX',
    'unitPrice': 12000,
    'limited': limited,
    'availableQty': availableQty,
    'status': status,
    'createdAt': '2026-08-11T10:00:00+09:00',
  };

  Map<String, dynamic> pageOf(
    List<Map<String, dynamic>> items, {
    int number = 0,
    int totalPages = 1,
  }) => {
    'items': items,
    'page': {
      'number': number,
      'size': 20,
      'totalElements': items.length,
      'totalPages': totalPages,
    },
  };

  test('상품 객체를 스펙 3.1대로 파싱한다', () async {
    backend.stub(
      '/products',
      () => okBody(pageOf([productJson(limited: true, availableQty: 30)])),
    );

    final page = await repository.list();
    final product = page.items.single;

    expect(product.id, 100);
    expect(product.productCode, 'P-000100');
    expect(product.name, '김치만두 1kg');
    expect(product.barcode, '8801234567890');
    expect(product.category, '냉동식품');
    expect(product.orderUnit, 'BOX');
    expect(product.unitPrice, 12000);
    expect(product.limited, isTrue);
    expect(product.availableQty, 30);
    expect(product.status, ProductStatus.onSale);
  });

  test('한정 품목이 아니면 availableQty는 null이다 (3.1)', () async {
    backend.stub('/products', () => okBody(pageOf([productJson()])));

    final page = await repository.list();

    expect(page.items.single.limited, isFalse);
    expect(page.items.single.availableQty, isNull);
  });

  test('모르는 status는 판매중지로 본다 (담을 수 없는 쪽이 안전)', () async {
    backend.stub(
      '/products',
      () => okBody(
        pageOf([productJson(status: 'SOMETHING_NEW')]),
      ),
    );

    final page = await repository.list();

    expect(page.items.single.status, ProductStatus.suspended);
  });

  test('값이 없는 필터는 쿼리에서 아예 뺀다', () async {
    backend.stub('/products', () => okBody(pageOf([])));

    await repository.list(keyword: '');
    final query = backend.requestsTo('/products').single.queryParameters;

    expect(query.containsKey('keyword'), isFalse);
    expect(query.containsKey('category'), isFalse);
    expect(query.containsKey('status'), isFalse);
    expect(query.containsKey('limited'), isFalse);
  });

  test('필터·페이징을 스펙 3.2.2의 쿼리로 보낸다', () async {
    backend.stub('/products', () => okBody(pageOf([])));

    await repository.list(
      keyword: '만두',
      category: '냉동식품',
      status: ProductStatus.onSale,
      limited: true,
      page: 2,
    );
    final query = backend.requestsTo('/products').single.queryParameters;

    expect(query['keyword'], '만두');
    expect(query['category'], '냉동식품');
    expect(query['status'], 'ON_SALE');
    expect(query['limited'], true);
    expect(query['page'], 2);
    expect(query['size'], ProductRepository.defaultPageSize);
    expect(query['sort'], ProductRepository.defaultSort);
  });

  test('마지막 페이지 여부를 페이징 응답으로 판단한다 (1.5)', () async {
    backend.stub(
      '/products',
      () => okBody(pageOf([productJson()], number: 0, totalPages: 3)),
    );

    expect((await repository.list()).isLast, isFalse);

    backend.stub(
      '/products',
      () => okBody(pageOf([productJson()], number: 2, totalPages: 3)),
    );

    expect((await repository.list(page: 2)).isLast, isTrue);
  });

  test('실패 응답은 ApiException으로 올라온다 (1.3)', () async {
    backend.stub('/products', () => errorBody('FORBIDDEN', 403));

    await expectLater(
      repository.list(),
      throwsA(
        isA<ApiException>()
            .having((e) => e.statusCode, 'statusCode', 403)
            .having((e) => e.code, 'code', 'FORBIDDEN'),
      ),
    );
  });
}
