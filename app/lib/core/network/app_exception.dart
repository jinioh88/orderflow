/// 네트워크 계층이 상위(리포지토리·컨트롤러)로 던지는 예외 체계.
///
/// api-spec 1.3·1.4 — 클라이언트 분기는 **HTTP 상태 + error.code**로만 한다.
/// `message`는 표시용이며 문구는 계약이 아니다.
sealed class AppException implements Exception {
  const AppException(this.message);

  /// 사용자에게 그대로 보여줄 수 있는 메시지.
  final String message;

  @override
  String toString() => '$runtimeType: $message';
}

/// 서버에 도달하지 못한 실패 — 연결 불가·타임아웃 등.
/// 오프라인 큐(US-ORD-07)의 트리거가 되는 예외이므로 [ApiException]과 구분한다.
final class NetworkException extends AppException {
  const NetworkException([super.message = '네트워크에 연결할 수 없습니다.']);
}

/// 서버가 `{error: {code, message, details}}`로 응답한 실패 (HTTP 4xx/5xx).
final class ApiException extends AppException {
  const ApiException({
    required this.statusCode,
    required this.code,
    required String message,
    this.details = const [],
  }) : super(message);

  final int statusCode;

  /// api-spec 1.4의 SCREAMING_SNAKE 에러 코드 (예: `VALIDATION_ERROR`).
  final String code;

  /// 필드 단위 오류 — `VALIDATION_ERROR` 등에서만 존재.
  final List<FieldError> details;

  bool get isUnauthorized => statusCode == 401 && code == 'UNAUTHORIZED';
  bool get isTokenExpired => statusCode == 401 && code == 'TOKEN_EXPIRED';
  bool get isForbidden => statusCode == 403;
  bool get isNotFound => statusCode == 404;
  bool get isConflict => statusCode == 409;
}

/// api-spec 1.3 — `error.details[{field, reason}]`.
final class FieldError {
  const FieldError({required this.field, required this.reason});

  factory FieldError.fromJson(Map<String, dynamic> json) => FieldError(
        field: json['field'] as String? ?? '',
        reason: json['reason'] as String? ?? '',
      );

  final String field;
  final String reason;
}
