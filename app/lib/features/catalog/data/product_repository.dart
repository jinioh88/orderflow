import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/network_providers.dart';
import '../../../core/network/paged.dart';
import 'product.dart';

/// CAT 조회 API 호출 (api-spec 3.2.2).
///
/// 앱이 쓰는 CAT API는 목록 조회 하나뿐이다 — 등록·수정·판매중지·엑셀은 전부 본사(HQ) 권한이라
/// 웹 담당이다(3.2 접근 권한 표).
final class ProductRepository {
  const ProductRepository(this._client);

  final ApiClient _client;

  /// 3.2.2 상품 목록. 쿼리는 모두 선택이고, 값이 없는 파라미터는 아예 보내지 않는다
  /// (빈 문자열을 보내면 "빈 값과 정확히 일치"로 해석될 여지가 있다).
  Future<Paged<Product>> list({
    String? keyword,
    String? category,
    ProductStatus? status,
    bool? limited,
    int page = 0,
    int size = defaultPageSize,
    String sort = defaultSort,
  }) async {
    final data = await _client.get(
      '/products',
      query: {
        if (keyword != null && keyword.isNotEmpty) 'keyword': keyword,
        if (category != null && category.isNotEmpty) 'category': category,
        'status': ?status?.wire,
        'limited': ?limited,
        'page': page,
        'size': size,
        'sort': sort,
      },
    );
    return Paged.fromJson(data as Map<String, dynamic>, Product.fromJson);
  }

  /// 공통 규약 1.5의 기본값(20)을 그대로 쓴다. 행 높이 72 기준 한 화면에 약 8행이
  /// 들어가므로, 20이면 첫 스크롤 전에 다음 페이지를 채울 여유가 있다.
  static const defaultPageSize = 20;

  /// 3.2.2의 정렬 허용 필드 중 `name,asc`.
  ///
  /// 스펙 기본값은 `createdAt,desc`지만, 점주는 "언제 등록됐는지" 모르는 상태로 상품을 찾는다.
  /// 목록을 훑어 찾는 화면이므로 가나다순이 스캔하기 쉽다.
  static const defaultSort = 'name,asc';
}

final productRepositoryProvider = Provider<ProductRepository>((ref) {
  return ProductRepository(ref.watch(apiClientProvider));
});
