/// 상품 객체 (api-spec 3.1).
///
/// 목록·단건·수정 응답이 모두 같은 스키마를 쓰므로 모델도 하나만 둔다.
final class Product {
  const Product({
    required this.id,
    required this.productCode,
    required this.name,
    required this.barcode,
    required this.category,
    required this.orderUnit,
    required this.unitPrice,
    required this.limited,
    required this.availableQty,
    required this.status,
  });

  factory Product.fromJson(Map<String, dynamic> json) => Product(
    id: json['id'] as int,
    productCode: json['productCode'] as String? ?? '',
    name: json['name'] as String? ?? '',
    barcode: json['barcode'] as String? ?? '',
    category: json['category'] as String? ?? '',
    orderUnit: json['orderUnit'] as String? ?? '',
    unitPrice: json['unitPrice'] as int? ?? 0,
    limited: json['limited'] as bool? ?? false,
    availableQty: json['availableQty'] as int?,
    status: ProductStatus.parse(json['status'] as String?),
  );

  final int id;

  /// 테넌트 내 유일. 등록 후 수정 불가 — 발주 라인 스냅샷의 기준(3.1).
  final String productCode;

  final String name;

  /// 입고 바코드 스캔 조회 겸용 (US-GRN-01).
  final String barcode;

  /// 자유 문자열 — MVP는 계층 카테고리가 없다(3.1).
  final String category;

  /// 발주 단위 (`BOX`, `EA`, `KG` …). 자유 문자열이라 앱이 해석하지 않고 그대로 보여준다.
  final String orderUnit;

  /// KRW 정수 (공통 규약 1.1).
  final int unitPrice;

  /// 한정 품목 여부 (US-CAT-04).
  final bool limited;

  /// 본사 가용 재고 — **한정 품목일 때만 값이 있다**(3.1).
  final int? availableQty;

  final ProductStatus status;
}

/// 상품 판매 상태 (3.1).
enum ProductStatus {
  onSale('ON_SALE'),
  suspended('SUSPENDED');

  const ProductStatus(this.wire);

  /// 서버와 주고받는 값. 화면 문구가 아니라 계약 값이다.
  final String wire;

  /// 모르는 값은 판매중지로 본다 — 담을 수 없는 품목을 담을 수 있는 것처럼
  /// 보여주는 쪽이 반대보다 위험하다.
  static ProductStatus parse(String? wire) => wire == onSale.wire
      ? ProductStatus.onSale
      : ProductStatus.suspended;
}
