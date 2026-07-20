// 디자인 토큰 — `design/design-system/tokens.json`의 `core`/`semantic`/`app` 레이어를
// 상수로 1:1 옮긴 것. **값의 단일 진실 공급원은 tokens.json이다**(01-foundations §0).
// 값이 어긋나면 tokens.json이 맞다.
//
// 화면 코드는 hex를 직접 쓰지 않고 반드시 이 파일의 이름을 경유한다.
// (`test/design_system_guard_test.dart`가 기계적으로 검사한다.)

import 'package:flutter/material.dart';

/// core 레이어 — 원시 팔레트.
///
/// 01-foundations §0: **화면 코드에서 직접 참조 금지.** 이 파일 안에서 semantic/app
/// 토큰을 조립할 때만 쓴다. 그래서 라이브러리 프라이빗(`_`)으로 둔다.
abstract final class _Core {
  static const neutral0 = Color(0xFFFFFFFF);
  static const neutral50 = Color(0xFFF8FAFC);
  static const neutral100 = Color(0xFFF1F5F9);
  static const neutral200 = Color(0xFFE2E8F0);
  static const neutral300 = Color(0xFFCBD5E1);
  static const neutral400 = Color(0xFF94A3B8);
  static const neutral600 = Color(0xFF475569);
  static const neutral800 = Color(0xFF1E293B);
  static const neutral900 = Color(0xFF0F172A);

  static const blue50 = Color(0xFFEFF6FF);
  static const blue600 = Color(0xFF2563EB);
  static const blue700 = Color(0xFF1D4ED8);

  static const orange50 = Color(0xFFFFF7ED);
  static const orange600 = Color(0xFFEA580C);
  static const orange700 = Color(0xFFC2410C);

  static const green50 = Color(0xFFF0FDF4);
  static const green600 = Color(0xFF16A34A);
  static const green700 = Color(0xFF15803D);

  static const amber50 = Color(0xFFFFFBEB);
  static const amber600 = Color(0xFFD97706);
  static const amber700 = Color(0xFFB45309);

  static const red50 = Color(0xFFFEF2F2);
  static const red600 = Color(0xFFDC2626);
  static const red700 = Color(0xFFB91C1C);

  static const violet50 = Color(0xFFF5F3FF);
  static const violet600 = Color(0xFF7C3AED);
  static const violet700 = Color(0xFF6D28D9);

  static const teal50 = Color(0xFFF0FDFA);
  static const teal600 = Color(0xFF0D9488);
  static const teal700 = Color(0xFF0F766E);
}

/// 시맨틱 피드백 4색 (01-foundations §1.3 — 웹·앱 공통 hue).
///
/// `solid`=면/아이콘, `text`=텍스트, `bg`=연한 배경.
class FeedbackColor {
  const FeedbackColor({
    required this.solid,
    required this.text,
    required this.bg,
  });

  final Color solid;
  final Color text;
  final Color bg;
}

abstract final class AppFeedback {
  static const success = FeedbackColor(
    solid: _Core.green600,
    text: _Core.green700,
    bg: _Core.green50,
  );
  static const warning = FeedbackColor(
    solid: _Core.amber600,
    text: _Core.amber700,
    bg: _Core.amber50,
  );
  static const danger = FeedbackColor(
    solid: _Core.red600,
    text: _Core.red700,
    bg: _Core.red50,
  );
  static const info = FeedbackColor(
    solid: _Core.blue600,
    text: _Core.blue700,
    bg: _Core.blue50,
  );
}

/// app 레이어 색 (tokens.json `app.color`).
///
/// ⚠ [primary]는 **면 전용**이다 (01-foundations §1.2 — 흰 텍스트 대비 3.2:1).
/// 흰 텍스트와의 조합은 버튼·탭 위젯 내부에서만 허용하고, 화면 코드는
/// 본문 크기 오렌지가 필요하면 [primaryStrong]을 쓴다.
abstract final class AppColors {
  static const primary = _Core.orange600;
  static const primaryStrong = _Core.orange700;
  static const primaryBg = _Core.orange50;

  static const pageBg = _Core.neutral0;
  static const surface = _Core.neutral0;
  static const surfaceAlt = _Core.neutral50;
  static const surfaceMuted = _Core.neutral100;
  static const border = _Core.neutral200;
  static const borderStrong = _Core.neutral300;

  static const textTitle = _Core.neutral900;
  static const textBody = _Core.neutral800;
  static const textCaption = _Core.neutral600;
  static const textDisabled = _Core.neutral400;
  static const textOnPrimary = _Core.neutral0;

  /// 타임라인 미래 단계·빈 상태 아이콘 등 "아직/없음"의 회색 (02-patterns §1.2, §2.1).
  static const inactive = _Core.neutral300;

  /// 스캔 검수 화면 전용 (04 §5 — 앱의 유일한 다크 화면).
  static const scanScreenBg = _Core.neutral900;
  static const scanScreenText = _Core.neutral50;

  /// 오프라인 배너 배경 (04 §7 — 에러 색을 쓰지 않는다).
  static const offlineBannerBg = _Core.neutral800;
}

/// 발주 상태 7종 (01-foundations §1.4 / semantic.color.orderStatus).
///
/// 라벨은 토큰의 `label` 값을 그대로 쓴다 — 화면마다 다른 표현 금지(02-patterns §1).
enum OrderStatus {
  submitted('SUBMITTED', '제출됨', _Core.blue600, _Core.blue700, _Core.blue50),
  pendingApproval('PENDING_APPROVAL', '승인 대기', _Core.amber600, _Core.amber700,
      _Core.amber50),
  approved('APPROVED', '승인됨', _Core.green600, _Core.green700, _Core.green50),
  rejected('REJECTED', '거절됨', _Core.red600, _Core.red700, _Core.red50),
  shipped('SHIPPED', '출하됨', _Core.violet600, _Core.violet700, _Core.violet50),
  received('RECEIVED', '입고 완료', _Core.teal600, _Core.teal700, _Core.teal50),
  canceled(
      'CANCELED', '취소됨', _Core.neutral400, _Core.neutral600, _Core.neutral100);

  const OrderStatus(this.code, this.label, this.dot, this.text, this.bg);

  /// 서버 API가 쓰는 상태 코드 (docs/04-domain-model 3장 상태 머신).
  final String code;
  final String label;
  final Color dot;
  final Color text;
  final Color bg;

  static OrderStatus? fromCode(String code) {
    for (final s in OrderStatus.values) {
      if (s.code == code) return s;
    }
    return null;
  }
}

/// 앱 타이포 스케일 (01-foundations §2.2 / app.typography).
///
/// 숫자가 정렬되는 곳(수량·금액)은 [numXl] 또는 [tabular]를 쓴다.
abstract final class AppText {
  static const fontFamily = 'Pretendard';

  static const _tabularFigures = [FontFeature.tabularFigures()];

  static const display = TextStyle(
    fontFamily: fontFamily,
    fontSize: 28,
    height: 36 / 28,
    fontWeight: FontWeight.w700,
  );
  static const title = TextStyle(
    fontFamily: fontFamily,
    fontSize: 22,
    height: 30 / 22,
    fontWeight: FontWeight.w600,
  );
  static const heading = TextStyle(
    fontFamily: fontFamily,
    fontSize: 18,
    height: 26 / 18,
    fontWeight: FontWeight.w600,
  );
  static const body = TextStyle(
    fontFamily: fontFamily,
    fontSize: 16,
    height: 24 / 16,
    fontWeight: FontWeight.w400,
  );
  static const bodyStrong = TextStyle(
    fontFamily: fontFamily,
    fontSize: 16,
    height: 24 / 16,
    fontWeight: FontWeight.w600,
  );

  /// 보조 설명·타임스탬프. **14 미만 금지**(01-foundations §2.2).
  static const caption = TextStyle(
    fontFamily: fontFamily,
    fontSize: 14,
    height: 20 / 14,
    fontWeight: FontWeight.w400,
  );

  /// 수량 스테퍼 값, 장바구니 합계 금액.
  static const numXl = TextStyle(
    fontFamily: fontFamily,
    fontSize: 24,
    height: 32 / 24,
    fontWeight: FontWeight.w700,
    fontFeatures: _tabularFigures,
  );

  /// 임의 스타일에 tabular figures를 켠다 — 수량·금액을 렌더하는 위젯 전용.
  static TextStyle tabular(TextStyle base) =>
      base.copyWith(fontFeatures: _tabularFigures);
}

/// 앱 스페이싱 (8dp 그리드, 보조 4 — 01-foundations §3.1).
abstract final class AppSpace {
  static const xs = 4.0;
  static const sm = 8.0;
  static const md = 12.0;

  /// 화면 좌우 여백 기본값.
  static const lg = 16.0;

  /// 섹션 간격.
  static const xl = 24.0;
  static const xxl = 32.0;
  static const xxxl = 40.0;
  static const huge = 48.0;
}

/// 앱 라운드 (01-foundations §3.2).
abstract final class AppRadius {
  /// 인풋·뱃지·칩.
  static const sm = 8.0;

  /// 버튼·셀렉트.
  static const md = 12.0;

  /// 카드·모달/시트.
  static const lg = 16.0;

  /// 도트·필 뱃지.
  static const full = 999.0;
}

/// 엘리베이션 (01-foundations §3.3).
abstract final class AppShadow {
  static const _shadowColor = Color(0x0F0F172A); // rgba(15,23,42,0.06)
  static const _shadowColor2 = Color(0x1A0F172A); // 0.10
  static const _shadowColor3 = Color(0x290F172A); // 0.16

  /// 카드 기본.
  static const level1 = [
    BoxShadow(color: _shadowColor, offset: Offset(0, 1), blurRadius: 2),
  ];

  /// 드롭다운, 팝오버.
  static const level2 = [
    BoxShadow(color: _shadowColor2, offset: Offset(0, 4), blurRadius: 12),
  ];

  /// 하단 고정 CTA 바 — shadow-2를 위로 뒤집은 것(화면 아래에 붙는 요소용).
  static const level2Up = [
    BoxShadow(color: _shadowColor, offset: Offset(0, -4), blurRadius: 12),
  ];

  /// 모달, 바텀 시트.
  static const level3 = [
    BoxShadow(color: _shadowColor3, offset: Offset(0, 12), blurRadius: 32),
  ];
}

/// 터치 타깃 (01-foundations §6 / app.touch).
abstract final class AppTouch {
  /// 최소 터치 타깃 — 이보다 작은 조작 요소 금지.
  static const minTarget = 48.0;

  /// 인접 타깃 간 최소 간격.
  static const minGap = 8.0;

  /// 하단 고정 CTA 버튼 높이 (04 §2).
  static const ctaHeight = 52.0;

  /// 수량 스테퍼 버튼 몸체 (간격 포함 터치 영역은 48 — 04 §2).
  static const stepperButton = 44.0;
}

/// 아이콘 크기 (app.icon).
abstract final class AppIconSize {
  static const normal = 24.0;
  static const inline = 20.0;

  /// 빈 상태 아이콘 (02-patterns §2.1 — 앱 48).
  static const emptyState = 48.0;
}
