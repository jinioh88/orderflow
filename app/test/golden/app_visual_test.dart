import 'dart:io';

import 'package:app/core/theme/app_theme.dart';
import 'package:app/core/theme/tokens.dart';
import 'package:app/core/widgets/app_button.dart';
import 'package:app/core/widgets/app_shell.dart';
import 'package:app/core/widgets/app_text_field.dart';
import 'package:app/core/widgets/bottom_cta_bar.dart';
import 'package:app/core/widgets/order_status_badge.dart';
import 'package:app/core/widgets/quantity_stepper.dart';
import 'package:app/core/widgets/status_views.dart';
import 'package:app/features/auth/presentation/login_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

/// 골든 테스트 — `styleguide-app.html`과 눈으로 대조하기 위한 렌더 결과물이자,
/// 이후 디자인 시스템 변경이 화면을 조용히 망가뜨리는 것을 잡는 회귀 테스트다.
///
/// 갱신: `flutter test --update-goldens test/golden`
///
/// ⚠ 골든 이미지는 폰트 래스터라이저가 OS마다 달라 **생성 환경에 종속적**이다.
/// 현재 baseline은 macOS에서 생성했다. 다른 OS(CI 등)에서 돌리면 미세한 픽셀 차이로
/// 실패할 수 있으므로, CI 도입 시에는 이 디렉토리를 제외하거나 baseline을 그 환경에서 다시 만든다.
void main() {
  setUpAll(() async {
    TestWidgetsFlutterBinding.ensureInitialized();
    // 골든에 실제 자형이 나와야 styleguide와 대조가 된다 — Pretendard를 직접 로드한다.
    await _loadFont(AppText.fontFamily, 'assets/fonts/PretendardVariable.ttf');

    // 아이콘 폰트는 테스트 런타임에 자동으로 붙지 않아 □(tofu)로 찍힌다.
    // Flutter SDK가 캐시해 둔 원본을 찾아 로드한다 (없으면 아이콘만 tofu로 남는다).
    final materialIcons = _materialIconsFile();
    if (materialIcons != null) {
      await _loadFont('MaterialIcons', materialIcons.path);
    }
  });

  Future<void> pumpPhone(WidgetTester tester, Widget child) async {
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      MaterialApp(theme: AppTheme.light(), home: child),
    );
    // 스켈레톤 셔머는 무한 반복이라 pumpAndSettle이 끝나지 않는다.
    // 로딩 지연(300ms)을 넘긴 시점의 한 프레임만 찍는다.
    await tester.pump(const Duration(milliseconds: 400));
    await tester.pump(const Duration(milliseconds: 100));
  }

  testWidgets('로그인 화면', (tester) async {
    await pumpPhone(tester, const LoginScreen());
    await expectLater(
      find.byType(LoginScreen),
      matchesGoldenFile('goldens/login_screen.png'),
    );
  });

  testWidgets('컴포넌트 갤러리 — 버튼·인풋·뱃지·스테퍼', (tester) async {
    await pumpPhone(tester, const _ComponentGallery());
    await expectLater(
      find.byType(_ComponentGallery),
      matchesGoldenFile('goldens/components.png'),
    );
  });

  testWidgets('상태 뷰 — 빈 상태 · 에러', (tester) async {
    await pumpPhone(
      tester,
      Scaffold(
        body: Column(
          children: [
            Expanded(
              child: EmptyView(
                icon: Icons.sell,
                title: '등록된 상품이 없습니다',
                description: '본사에서 상품을 등록하면 여기에 표시됩니다',
                action: AppButton.secondary(label: '새로고침', onPressed: () {}),
              ),
            ),
            const Divider(),
            Expanded(
              child: ErrorView(
                message: '네트워크 연결을 확인한 뒤 다시 시도하세요',
                onRetry: () {},
              ),
            ),
          ],
        ),
      ),
    );
    await expectLater(
      find.byType(Scaffold),
      matchesGoldenFile('goldens/status_views.png'),
    );
  });

  testWidgets('앱 셸 — 하단 탭 4개 + 하단 고정 CTA 바', (tester) async {
    await pumpPhone(
      tester,
      Scaffold(
        appBar: AppBar(title: const Text('장바구니')),
        body: const SizedBox.expand(),
        bottomNavigationBar: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            BottomCtaBar(
              summary: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text('12품목', style: AppText.body),
                  Text('₩1,240,000', style: AppText.numXl),
                ],
              ),
              child: AppButton.cta(label: '발주 제출', onPressed: () {}),
            ),
            const _TabBarPreview(),
          ],
        ),
      ),
    );
    await expectLater(
      find.byType(Scaffold),
      matchesGoldenFile('goldens/shell.png'),
    );
  });
}

Future<void> _loadFont(String family, String path) async {
  final loader = FontLoader(family)
    ..addFont(
      File(path).readAsBytes().then((bytes) => ByteData.view(bytes.buffer)),
    );
  await loader.load();
}

/// Flutter SDK 캐시의 Material 아이콘 폰트를 찾는다.
File? _materialIconsFile() {
  final flutterRoot = Platform.environment['FLUTTER_ROOT'] ??
      // `flutter test`는 SDK의 dart를 쓰므로 실행 중인 Dart의 위치로 SDK를 역산한다.
      Directory(Platform.resolvedExecutable).parent.parent.parent.parent.path;
  final file = File(
    '$flutterRoot/bin/cache/artifacts/material_fonts/MaterialIcons-Regular.otf',
  );
  return file.existsSync() ? file : null;
}

/// 실제 라우터 없이 하단 탭의 모양만 확인하기 위한 미리보기.
class _TabBarPreview extends StatelessWidget {
  const _TabBarPreview();

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: const BoxDecoration(
        border: Border(top: BorderSide(color: AppColors.border)),
      ),
      child: NavigationBar(
        selectedIndex: 1,
        onDestinationSelected: (_) {},
        destinations: [
          for (final tab in AppTab.values)
            NavigationDestination(icon: Icon(tab.icon), label: tab.label),
        ],
      ),
    );
  }
}

class _ComponentGallery extends StatefulWidget {
  const _ComponentGallery();

  @override
  State<_ComponentGallery> createState() => _ComponentGalleryState();
}

class _ComponentGalleryState extends State<_ComponentGallery> {
  int _quantity = 12;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('컴포넌트')),
      body: ListView(
        padding: const EdgeInsets.all(AppSpace.lg),
        children: [
          Text('발주하기', style: AppText.title),
          Text('자주 쓰는 발주', style: AppText.heading),
          Text('콜드브루 원액 1L', style: AppText.body),
          Text(
            '₩12,000 / 박스 · 7월 19일(일)',
            style: AppText.caption.copyWith(color: AppColors.textCaption),
          ),
          Text('₩1,240,000', style: AppText.numXl),
          const SizedBox(height: AppSpace.lg),
          Row(
            children: [
              AppButton.primary(label: '템플릿 불러오기', onPressed: () {}),
              const SizedBox(width: AppSpace.sm),
              AppButton.secondary(label: '취소', onPressed: () {}),
            ],
          ),
          const SizedBox(height: AppSpace.sm),
          Row(
            children: [
              AppTextButton(label: '전체 보기', onPressed: () {}),
              const Spacer(),
              QuantityStepper(
                value: _quantity,
                onChanged: (v) => setState(() => _quantity = v),
              ),
            ],
          ),
          const SizedBox(height: AppSpace.sm),
          AppButton.cta(label: '발주 제출 · ₩1,240,000', onPressed: () {}),
          const SizedBox(height: AppSpace.sm),
          const AppButton.cta(label: '제출 중', onPressed: null, loading: true),
          const SizedBox(height: AppSpace.lg),
          Wrap(
            spacing: AppSpace.sm,
            runSpacing: AppSpace.sm,
            children: [
              for (final status in OrderStatus.values)
                OrderStatusBadge(status),
              const OrderStatusBadge.pendingSend(),
              const OrderStatusBadge(
                OrderStatus.approved,
                size: BadgeSize.lg,
              ),
            ],
          ),
          const SizedBox(height: AppSpace.lg),
          const AppTextField(
            label: '이메일',
            hint: 'name@company.com',
          ),
          const SizedBox(height: AppSpace.md),
          const AppTextField(
            label: '수량',
            unitSuffix: '박스',
            numeric: true,
            optional: true,
          ),
          const SizedBox(height: AppSpace.md),
          const SkeletonBox(height: 72),
        ],
      ),
    );
  }
}
