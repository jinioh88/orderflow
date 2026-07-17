import 'package:app/core/network/paged.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('페이징 응답을 파싱한다 (api-spec 1.5)', () {
    final json = {
      'items': [
        {'id': 1, 'name': 'A'},
        {'id': 2, 'name': 'B'},
      ],
      'page': {'number': 0, 'size': 20, 'totalElements': 135, 'totalPages': 7},
    };

    final paged = Paged.fromJson(json, (item) => item['name'] as String);

    expect(paged.items, ['A', 'B']);
    expect(paged.page.totalElements, 135);
    expect(paged.isLast, false);
  });

  test('마지막 페이지를 판별한다', () {
    final paged = Paged.fromJson({
      'items': const <Map<String, dynamic>>[],
      'page': {'number': 6, 'size': 20, 'totalElements': 135, 'totalPages': 7},
    }, (item) => item);

    expect(paged.isLast, true);
  });
}
