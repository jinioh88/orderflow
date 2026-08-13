import 'package:dio/dio.dart';

import '../config/app_config.dart';
import 'app_exception.dart';

/// `Authorization: Bearer <accessToken>` 헤더 주입용 토큰 공급자.
///
/// null을 돌려주면 헤더를 붙이지 않는다 (익명 허용 API — api-spec 1.2).
typedef AccessTokenReader = Future<String?> Function();

/// 액세스 토큰 재발급기 — 새 액세스 토큰을 돌려주고, 재발급이 실패하면 null.
///
/// api-spec 2.5: `401 TOKEN_EXPIRED`를 받으면 클라이언트는 `POST /auth/refresh` 후
/// **원 요청을 재시도**한다. 이 콜백은 그 재발급(+세션 갱신·저장)을 수행한다.
typedef AccessTokenRefresher = Future<String?> Function();

/// `403 PASSWORD_SETUP_REQUIRED` 수신 통지 (api-spec 2.3).
///
/// 본사가 임시 비밀번호를 재발급하면 서버는 그 즉시 업무 API를 이 코드로 거절하지만,
/// 앱이 들고 있는 세션 플래그는 여전히 false다. 인증 컨트롤러가 상태를 맞추게 알린다.
typedef PasswordSetupRequiredHandler = Future<void> Function();

/// OrderFlow API 공통 클라이언트 (api-spec 1장).
///
/// - 성공(2xx): `{data: ...}` 래퍼를 벗겨 `data` 값만 돌려준다. 204는 null.
/// - 실패(4xx/5xx): `{error: {code, message, details}}` → [ApiException].
/// - 서버 미도달(타임아웃·연결 불가): [NetworkException].
///
/// 리포지토리는 dio를 직접 만지지 않고 이 클래스만 쓴다.
class ApiClient {
  ApiClient({
    Dio? dio,
    AccessTokenReader? readAccessToken,
    AccessTokenRefresher? refreshAccessToken,
    PasswordSetupRequiredHandler? onPasswordSetupRequired,
  }) : _dio = dio ?? Dio(defaultOptions) {
    // 익명 클라이언트(로그인·재발급 전용)는 붙일 일이 없다 — 인터셉터를 아예 달지 않는다.
    if (readAccessToken == null &&
        refreshAccessToken == null &&
        onPasswordSetupRequired == null) {
      return;
    }
    _dio.interceptors.add(
      _AuthInterceptor(
        readAccessToken: readAccessToken,
        refreshAccessToken: refreshAccessToken,
        onPasswordSetupRequired: onPasswordSetupRequired,
        retry: (options) => _dio.fetch<dynamic>(options),
      ),
    );
  }

  static BaseOptions get defaultOptions => BaseOptions(
    baseUrl: AppConfig.apiBaseUrl,
    connectTimeout: const Duration(seconds: 5),
    receiveTimeout: const Duration(seconds: 15),
    // 상태 코드로 throw하지 않게 전부 받아서 아래에서 일괄 매핑한다.
    validateStatus: (_) => true,
  );

  final Dio _dio;

  Future<dynamic> get(String path, {Map<String, dynamic>? query}) =>
      _request(() => _dio.get<dynamic>(path, queryParameters: query));

  Future<dynamic> post(String path, {Object? body}) =>
      _request(() => _dio.post<dynamic>(path, data: body));

  Future<dynamic> put(String path, {Object? body}) =>
      _request(() => _dio.put<dynamic>(path, data: body));

  Future<dynamic> delete(String path) =>
      _request(() => _dio.delete<dynamic>(path));

  Future<dynamic> _request(Future<Response<dynamic>> Function() send) async {
    final Response<dynamic> response;
    try {
      response = await send();
    } on DioException catch (e) {
      throw _mapDioException(e);
    }
    return _unwrap(response);
  }

  dynamic _unwrap(Response<dynamic> response) {
    final status = response.statusCode ?? 0;

    if (status >= 200 && status < 300) {
      if (status == 204 || response.data == null) return null;
      final body = response.data;
      if (body is Map<String, dynamic> && body.containsKey('data')) {
        return body['data'];
      }
      // 래퍼 없는 2xx는 계약 위반 — 조용히 넘기지 않고 드러낸다.
      throw ApiException(
        statusCode: status,
        code: 'MALFORMED_RESPONSE',
        message: '서버 응답 형식이 올바르지 않습니다.',
      );
    }

    final error = _errorEnvelopeOf(response);
    if (error != null) {
      final rawDetails = error['details'];
      throw ApiException(
        statusCode: status,
        code: error['code'] as String? ?? 'UNKNOWN',
        message: error['message'] as String? ?? '요청을 처리하지 못했습니다.',
        details: rawDetails is List
            ? rawDetails
                  .whereType<Map<String, dynamic>>()
                  .map(FieldError.fromJson)
                  .toList()
            : const [],
      );
    }
    // 게이트웨이 오류 등 래퍼가 없는 실패 응답.
    throw ApiException(
      statusCode: status,
      code: 'UNKNOWN',
      message: '요청을 처리하지 못했습니다. (HTTP $status)',
    );
  }

  AppException _mapDioException(DioException e) {
    switch (e.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
      case DioExceptionType.transformTimeout:
        return const NetworkException('서버 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요.');
      case DioExceptionType.connectionError:
        return const NetworkException();
      case DioExceptionType.cancel:
        return const NetworkException('요청이 취소되었습니다.');
      case DioExceptionType.badCertificate:
      case DioExceptionType.badResponse:
      case DioExceptionType.unknown:
        return const NetworkException('요청에 실패했습니다. 네트워크 상태를 확인해 주세요.');
    }
  }
}

/// 실패 응답의 `{error: {...}}` 봉투. 형식이 아니면 null (api-spec 1.3).
Map<String, dynamic>? _errorEnvelopeOf(Response<dynamic> response) {
  final body = response.data;
  final error = body is Map<String, dynamic> ? body['error'] : null;
  return error is Map<String, dynamic> ? error : null;
}

/// 실패 응답의 `error.code`. 없으면 null.
String? _errorCodeOf(Response<dynamic> response) =>
    _errorEnvelopeOf(response)?['code'] as String?;

/// 토큰 헤더 주입 + `401 TOKEN_EXPIRED` 자동 재발급·재시도 (api-spec 1.2 · 2.5).
class _AuthInterceptor extends Interceptor {
  _AuthInterceptor({
    required this.readAccessToken,
    required this.refreshAccessToken,
    required this.onPasswordSetupRequired,
    required this.retry,
  });

  /// 재발급 후 원 요청을 다시 태우는 경로 — 무한 재귀를 막으려고 [_retriedFlag]를 함께 쓴다.
  static const _retriedFlag = 'auth.retried';

  final AccessTokenReader? readAccessToken;
  final AccessTokenRefresher? refreshAccessToken;
  final PasswordSetupRequiredHandler? onPasswordSetupRequired;
  final Future<Response<dynamic>> Function(RequestOptions options) retry;

  /// 진행 중인 재발급 1건. 동시에 여러 요청이 401을 받아도 재발급은 한 번만 나간다
  /// (single-flight). 리프레시 토큰은 회전(2.2)되므로 병렬 재발급은 서로를 무효화한다.
  Future<String?>? _refreshing;

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final token = await readAccessToken?.call();
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  /// [ApiClient]는 `validateStatus`로 모든 상태 코드를 성공 응답으로 받으므로
  /// 401도 `onError`가 아니라 여기로 온다.
  @override
  Future<void> onResponse(
    Response<dynamic> response,
    ResponseInterceptorHandler handler,
  ) async {
    final options = response.requestOptions;
    final code = _errorCodeOf(response);

    // 임시 비밀번호 상태(2.3) — 재시도해도 결과가 같으므로 상태만 맞추고 응답은 그대로 올린다.
    // 라우터 가드가 비밀번호 설정 화면으로 보낸다.
    if (response.statusCode == 403 && code == 'PASSWORD_SETUP_REQUIRED') {
      await onPasswordSetupRequired?.call();
      handler.next(response);
      return;
    }

    final canRefresh =
        response.statusCode == 401 &&
        code == 'TOKEN_EXPIRED' &&
        refreshAccessToken != null &&
        options.extra[_retriedFlag] != true;
    if (!canRefresh) {
      handler.next(response);
      return;
    }

    // 이미 다른 요청이 재발급을 끝냈다면(토큰이 바뀌었다면) 다시 재발급하지 않는다.
    // 리프레시 토큰은 회전(2.2)되므로, 병렬 요청이 각자 재발급하면 서로의 토큰을 무효화한다.
    final current = await readAccessToken?.call();
    final token = current != null && !_wasSentWith(options, current)
        ? current
        : await _refreshOnce();

    if (token == null) {
      // 재발급 실패 — 원래의 401을 그대로 올려보낸다. 세션 정리·화면 이동은
      // 재발급 콜백(인증 컨트롤러)이 이미 했다.
      handler.next(response);
      return;
    }

    try {
      // 원 요청을 **복사해서** 다시 태운다 — 같은 RequestOptions를 재사용하면
      // 이미 소비된 요청 스트림을 다시 읽으려다 실패한다.
      final retried = await retry(
        options.copyWith(
          headers: {...options.headers, 'Authorization': 'Bearer $token'},
          extra: {...options.extra, _retriedFlag: true},
        ),
      );
      handler.resolve(retried);
    } on DioException catch (e) {
      handler.reject(e);
    }
  }

  bool _wasSentWith(RequestOptions options, String token) =>
      options.headers['Authorization'] == 'Bearer $token';

  Future<String?> _refreshOnce() {
    return _refreshing ??= refreshAccessToken!().whenComplete(() {
      _refreshing = null;
    });
  }
}
