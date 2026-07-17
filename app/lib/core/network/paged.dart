/// api-spec 1.5 페이징 응답 공통 모델.
///
/// ```json
/// { "items": [...], "page": { "number": 0, "size": 20, "totalElements": 135, "totalPages": 7 } }
/// ```
final class Paged<T> {
  const Paged({required this.items, required this.page});

  factory Paged.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) fromItem,
  ) =>
      Paged(
        items: (json['items'] as List? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(fromItem)
            .toList(),
        page: PageInfo.fromJson(json['page'] as Map<String, dynamic>? ?? const {}),
      );

  final List<T> items;
  final PageInfo page;

  /// 마지막 페이지 여부 — 무한 스크롤 종료 판단용.
  bool get isLast => page.number + 1 >= page.totalPages;
}

final class PageInfo {
  const PageInfo({
    required this.number,
    required this.size,
    required this.totalElements,
    required this.totalPages,
  });

  factory PageInfo.fromJson(Map<String, dynamic> json) => PageInfo(
        number: json['number'] as int? ?? 0,
        size: json['size'] as int? ?? 0,
        totalElements: json['totalElements'] as int? ?? 0,
        totalPages: json['totalPages'] as int? ?? 0,
      );

  final int number;
  final int size;
  final int totalElements;
  final int totalPages;
}
