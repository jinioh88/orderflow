import 'package:dio/dio.dart';

import '../config/app_config.dart';
import 'app_exception.dart';

/// `Authorization: Bearer <accessToken>` 헤더 주입용 토큰 공급자.
///
/// Phase 0에서는 연결할 토큰이 없으므로 null을 돌려주는 구현을 쓰고,
/// AUTH 단계에서 secure storage와 연결한다. null이면 헤더를 붙이지 않는다
/// (익명 허용 API — api-spec 1.2).
typedef AccessTokenReader = Future<String?> Function();

/// OrderFlow API 공통 클라이언트 (api-spec 1장).
///
/// - 성공(2xx): `{data: ...}` 래퍼를 벗겨 `data` 값만 돌려준다. 204는 null.
/// - 실패(4xx/5xx): `{error: {code, message, details}}` → [ApiException].
/// - 서버 미도달(타임아웃·연결 불가): [NetworkException].
///
/// 리포지토리는 dio를 직접 만지지 않고 이 클래스만 쓴다.
class ApiClient {
  ApiClient({Dio? dio, AccessTokenReader? readAccessToken})
      : _dio = dio ?? Dio(defaultOptions) {
    _dio.interceptors.add(_AuthHeaderInterceptor(readAccessToken));
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

    final body = response.data;
    final error = body is Map<String, dynamic> ? body['error'] : null;
    if (error is Map<String, dynamic>) {
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

class _AuthHeaderInterceptor extends Interceptor {
  _AuthHeaderInterceptor(this._readAccessToken);

  final AccessTokenReader? _readAccessToken;

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final token = await _readAccessToken?.call();
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }
}
