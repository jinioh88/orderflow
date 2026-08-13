// AUTH 데이터 모델 — api-spec 2장(`server/docs/api/02-auth.md`) 기준.
//
// 스펙에 없는 필드는 만들지 않는다. 서버가 모르는 role 문자열을 보내도 앱이 죽지 않도록
// [UserRole]만 unknown 폴백을 둔다.

/// 사용자 역할 (스펙 2.1).
enum UserRole {
  system('SYSTEM'),
  hqAdmin('HQ_ADMIN'),
  hqManager('HQ_MANAGER'),
  storeOwner('STORE_OWNER'),
  storeStaff('STORE_STAFF'),

  /// 앱이 모르는 역할 — 서버가 역할을 추가해도 파싱이 깨지지 않게 한다.
  unknown('UNKNOWN');

  const UserRole(this.code);

  final String code;

  static UserRole fromCode(String? code) {
    for (final role in UserRole.values) {
      if (role.code == code) return role;
    }
    return UserRole.unknown;
  }
}

/// 로그인 응답의 `user` (스펙 2.4.2).
///
/// `tenantId`/`storeId`는 SYSTEM 계정에서 null이다. 앱은 점주(STORE_OWNER)만 쓰지만
/// 스펙 그대로 nullable로 받는다.
final class AuthUser {
  const AuthUser({
    required this.id,
    required this.email,
    required this.name,
    required this.role,
    this.tenantId,
    this.storeId,
  });

  factory AuthUser.fromJson(Map<String, dynamic> json) => AuthUser(
    id: (json['id'] as num).toInt(),
    email: json['email'] as String? ?? '',
    name: json['name'] as String? ?? '',
    role: UserRole.fromCode(json['role'] as String?),
    tenantId: (json['tenantId'] as num?)?.toInt(),
    storeId: (json['storeId'] as num?)?.toInt(),
  );

  final int id;
  final String email;
  final String name;
  final UserRole role;
  final int? tenantId;
  final int? storeId;

  Map<String, dynamic> toJson() => {
    'id': id,
    'email': email,
    'name': name,
    'role': role.code,
    'tenantId': tenantId,
    'storeId': storeId,
  };
}

/// 액세스 + 리프레시 토큰 쌍 (스펙 2.4.3 — 재발급 시 회전된 새 쌍이 온다).
///
/// `accessTokenExpiresIn`(초)은 스펙이 주는 값이지만 앱은 만료를 선제적으로 계산하지 않는다 —
/// 만료 판정은 서버의 `401 TOKEN_EXPIRED`가 정본이다(스펙 2.5). 값은 참고용으로만 보관한다.
final class TokenPair {
  const TokenPair({
    required this.accessToken,
    required this.refreshToken,
    this.accessTokenExpiresIn,
  });

  factory TokenPair.fromJson(Map<String, dynamic> json) => TokenPair(
    accessToken: json['accessToken'] as String,
    refreshToken: json['refreshToken'] as String,
    accessTokenExpiresIn: (json['accessTokenExpiresIn'] as num?)?.toInt(),
  );

  final String accessToken;
  final String refreshToken;
  final int? accessTokenExpiresIn;
}

/// 앱이 들고 다니는 로그인 세션.
///
/// 스펙에 프로필 조회 API(`GET /users/me`)가 없으므로, 앱 재시작 시 사용자 정보를
/// 복원하려면 로그인 응답의 [user]를 토큰과 함께 로컬에 저장해야 한다
/// (자동 로그인 방식 — 사용자 승인 2026-08-12).
final class AuthSession {
  const AuthSession({
    required this.accessToken,
    required this.refreshToken,
    required this.user,
    required this.passwordSetupRequired,
  });

  /// 로그인 응답(2.4.2)과 로컬 저장 형식이 같은 모양이라 파서를 하나만 둔다.
  factory AuthSession.fromJson(Map<String, dynamic> json) => AuthSession(
    accessToken: json['accessToken'] as String,
    refreshToken: json['refreshToken'] as String,
    user: AuthUser.fromJson(json['user'] as Map<String, dynamic>),
    passwordSetupRequired: json['passwordSetupRequired'] as bool? ?? false,
  );

  final String accessToken;
  final String refreshToken;
  final AuthUser user;

  /// 임시 비밀번호 상태 (스펙 2.3) — true면 비밀번호 설정 화면 외 진입을 막는다.
  final bool passwordSetupRequired;

  AuthSession copyWith({required bool passwordSetupRequired}) => AuthSession(
    accessToken: accessToken,
    refreshToken: refreshToken,
    user: user,
    passwordSetupRequired: passwordSetupRequired,
  );

  AuthSession copyWithTokens(TokenPair tokens, {bool? passwordSetupRequired}) =>
      AuthSession(
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
        user: user,
        passwordSetupRequired:
            passwordSetupRequired ?? this.passwordSetupRequired,
      );

  Map<String, dynamic> toJson() => {
    'accessToken': accessToken,
    'refreshToken': refreshToken,
    'user': user.toJson(),
    'passwordSetupRequired': passwordSetupRequired,
  };
}
