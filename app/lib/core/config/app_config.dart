/// 앱 전역 환경 설정.
///
/// 빌드 시점에 `--dart-define`으로 주입한다. 예:
/// ```
/// flutter run --dart-define=SERVER_ORIGIN=http://10.0.2.2:8080
/// ```
/// 값을 주지 않으면 로컬 개발 기본값을 쓴다.
abstract final class AppConfig {
  static const String serverOrigin = String.fromEnvironment(
    'SERVER_ORIGIN',
    defaultValue: 'http://localhost:8080',
  );

  /// api-spec 1.1 — Base URL은 `/api/v1`.
  static const String apiBaseUrl = '$serverOrigin/api/v1';
}
