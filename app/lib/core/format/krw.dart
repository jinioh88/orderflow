/// 금액·수량 표기 (01-foundations §5).
///
/// `intl`을 넣지 않고 직접 만든다 — 앱에서 필요한 형식은 "KRW 정수 + 천 단위 쉼표"
/// 하나뿐이고(공통 규약 1.1), 통화·로케일이 늘어날 계획이 없다.
library;

/// `12000` → `₩12,000`.
String formatKrw(int amount) => '₩${formatCount(amount)}';

/// `12000` → `12,000`. 부호는 그대로 유지한다.
String formatCount(int value) {
  final digits = value.abs().toString();
  final buffer = StringBuffer(value.isNegative ? '-' : '');

  for (var i = 0; i < digits.length; i++) {
    if (i > 0 && (digits.length - i) % 3 == 0) buffer.write(',');
    buffer.write(digits[i]);
  }
  return buffer.toString();
}
