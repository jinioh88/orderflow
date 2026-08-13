import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/storage/key_value_store.dart';
import 'auth_models.dart';

/// 로그인 세션의 로컬 저장소.
///
/// 토큰과 사용자 프로필을 **JSON 한 덩어리**로 저장한다 — 토큰만 남고 프로필이 사라지는
/// 어중간한 상태가 생기지 않게 하려는 것이다(쓰기·삭제가 각각 1회).
/// 저장 위치는 secure storage(Keychain/Keystore).
final class AuthSessionStore {
  const AuthSessionStore(this._store);

  static const _key = 'auth.session';

  final KeyValueStore _store;

  /// 저장된 세션이 없거나 읽을 수 없으면 null.
  ///
  /// **어떤 이유로도 예외를 던지지 않는다.** 저장소 자체가 실패하는 일이 실제로 있고
  /// (Keystore 손상·기기 백업 복원 후 복호화 실패 → `PlatformException`), 그때 예외가
  /// 위로 새면 앱이 부팅 화면에서 영영 멈춘다. 읽기 실패는 "저장된 세션 없음"과 같게 취급하고
  /// 로그인 화면으로 보내는 편이 낫다. 깨진 값은 지워서 다음 기동을 오염시키지 않는다.
  Future<AuthSession?> read() async {
    try {
      final raw = await _store.read(_key);
      if (raw == null || raw.isEmpty) return null;
      return AuthSession.fromJson(jsonDecode(raw) as Map<String, dynamic>);
    } catch (_) {
      await clear();
      return null;
    }
  }

  Future<void> write(AuthSession session) =>
      _store.write(_key, jsonEncode(session.toJson()));

  /// 삭제 실패는 삼킨다 — 호출자(로그아웃·세션 정리)는 저장소 상태와 무관하게
  /// 반드시 다음 단계로 진행해야 한다.
  Future<void> clear() async {
    try {
      await _store.delete(_key);
    } catch (_) {
      // 무시.
    }
  }
}

final authSessionStoreProvider = Provider<AuthSessionStore>((ref) {
  return AuthSessionStore(ref.watch(keyValueStoreProvider));
});
