import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// 키-값 보안 저장소 인터페이스.
///
/// 토큰처럼 평문으로 남으면 안 되는 값을 다룬다. 인터페이스를 따로 둔 이유는
/// 플랫폼 채널(Keychain/Keystore)에 의존하는 [SecureKeyValueStore]가 단위 테스트에서
/// 동작하지 않기 때문이다 — 테스트는 이 인터페이스의 in-memory 구현을 주입한다.
abstract interface class KeyValueStore {
  Future<String?> read(String key);
  Future<void> write(String key, String value);
  Future<void> delete(String key);
}

/// flutter_secure_storage 구현 — iOS Keychain / Android Keystore (app/CLAUDE.md 기술 스택).
final class SecureKeyValueStore implements KeyValueStore {
  const SecureKeyValueStore(this._storage);

  final FlutterSecureStorage _storage;

  @override
  Future<String?> read(String key) => _storage.read(key: key);

  @override
  Future<void> write(String key, String value) =>
      _storage.write(key: key, value: value);

  @override
  Future<void> delete(String key) => _storage.delete(key: key);
}

/// 전역 보안 저장소 Provider. 테스트에서는 이 Provider를 override한다.
final keyValueStoreProvider = Provider<KeyValueStore>((ref) {
  // v10 기본값이 이미 Android Keystore(AES-GCM + RSA 키 래핑) / iOS Keychain이다.
  // (v9에서 쓰던 `encryptedSharedPreferences` 옵션은 v10에서 폐기됐다.)
  return const SecureKeyValueStore(FlutterSecureStorage());
});
