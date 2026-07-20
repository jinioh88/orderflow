import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

/// 디자인 시스템 규칙 중 **사람이 기억하는 대신 기계가 잡아야 하는 것**을 소스 코드에서 검사한다.
///
/// 04-app-components §8 인수 조건:
/// - 화면 코드 hex 하드코딩 금지 (01-foundations §0)
/// - 오렌지(`AppColors.primary`) + 흰 텍스트 조합이 버튼 위젯 밖에서 만들어지지 않을 것 (§2)
void main() {
  final libDir = Directory('lib');

  List<File> dartFilesIn(String path) {
    final dir = Directory(path);
    if (!dir.existsSync()) return const [];
    return dir
        .listSync(recursive: true)
        .whereType<File>()
        .where((f) => f.path.endsWith('.dart'))
        .toList();
  }

  /// 토큰 정의 파일 자신은 hex를 가질 수밖에 없다 — 유일한 예외.
  const tokenDefinition = 'lib/core/theme/tokens.dart';

  /// 오렌지 면 위에 흰 텍스트를 얹는 조합을 캡슐화하는 위젯들 (04 §2).
  const primaryFillOwners = {
    'lib/core/theme/app_theme.dart',
    'lib/core/widgets/app_button.dart',
    'lib/core/widgets/app_shell.dart',
  };

  String normalize(File f) => f.path.replaceAll(r'\', '/');

  test('lib/ 어디에도 hex 색상을 하드코딩하지 않는다 (토큰 정의 파일 제외)', () {
    final hex = RegExp(r'Color\(0x[0-9a-fA-F]{8}\)');
    final offenders = <String>[];

    for (final file in dartFilesIn(libDir.path)) {
      final path = normalize(file);
      if (path == tokenDefinition) continue;
      final lines = file.readAsLinesSync();
      for (var i = 0; i < lines.length; i++) {
        if (hex.hasMatch(lines[i])) {
          offenders.add('$path:${i + 1}  ${lines[i].trim()}');
        }
      }
    }

    expect(
      offenders,
      isEmpty,
      reason: 'hex 대신 tokens.dart의 토큰 이름을 쓴다 (01-foundations §0):\n'
          '${offenders.join('\n')}',
    );
  });

  test('Colors.* 팔레트를 화면 코드에서 쓰지 않는다', () {
    // Colors.transparent만 허용 — Material 기본 틴트를 끄는 용도라 색이 아니다.
    final materialColor = RegExp(r'\bColors\.(?!transparent\b)\w+');
    final offenders = <String>[];

    for (final file in dartFilesIn(libDir.path)) {
      final path = normalize(file);
      final lines = file.readAsLinesSync();
      for (var i = 0; i < lines.length; i++) {
        if (materialColor.hasMatch(lines[i])) {
          offenders.add('$path:${i + 1}  ${lines[i].trim()}');
        }
      }
    }

    expect(
      offenders,
      isEmpty,
      reason: 'Material 기본 팔레트 대신 AppColors/AppFeedback을 쓴다:\n'
          '${offenders.join('\n')}',
    );
  });

  test('오렌지 면(AppColors.primary)은 버튼·탭·테마 위젯 안에서만 쓴다', () {
    // 흰 텍스트 on orange-600은 대비 3.2:1이라 16px semibold 이상에서만 허용된다
    // (01-foundations §1.2). 그 조건 판단을 화면 코드에 맡기지 않는다.
    final usage = RegExp(r'AppColors\.primary\b(?!Strong|Bg)');
    final offenders = <String>[];

    for (final file in dartFilesIn(libDir.path)) {
      final path = normalize(file);
      if (primaryFillOwners.contains(path)) continue;
      if (path == 'lib/core/theme/tokens.dart') continue;
      final lines = file.readAsLinesSync();
      for (var i = 0; i < lines.length; i++) {
        if (usage.hasMatch(lines[i])) {
          offenders.add('$path:${i + 1}  ${lines[i].trim()}');
        }
      }
    }

    expect(
      offenders,
      isEmpty,
      reason: '본문 크기 오렌지는 AppColors.primaryStrong을 쓰고, 오렌지 면이 필요하면 '
          'AppButton 등 캡슐화된 위젯을 쓴다 (04 §2):\n${offenders.join('\n')}',
    );
  });

  test('features/ 화면 코드는 TextStyle을 직접 만들지 않는다', () {
    // 스케일 밖 크기가 생기는 것을 막는다 — AppText.* 를 copyWith 해서 쓴다.
    final rawStyle = RegExp(r'TextStyle\(');
    final offenders = <String>[];

    for (final file in dartFilesIn('lib/features')) {
      final lines = file.readAsLinesSync();
      for (var i = 0; i < lines.length; i++) {
        if (rawStyle.hasMatch(lines[i])) {
          offenders.add('${normalize(file)}:${i + 1}  ${lines[i].trim()}');
        }
      }
    }

    expect(
      offenders,
      isEmpty,
      reason: 'AppText 스케일을 쓴다 (01-foundations §2.2):\n${offenders.join('\n')}',
    );
  });
}
