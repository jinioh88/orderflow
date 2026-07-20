import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../theme/tokens.dart';

/// 하단 탭 정의 (04-app-components §1.1 — D0 결정 #5).
///
/// 아이콘은 01-foundations §4의 도메인 개념 ↔ Material 아이콘 매핑을 따른다.
enum AppTab {
  home('/home', '홈', Icons.home),
  order('/order', '발주', Icons.list_alt),
  history('/history', '내역', Icons.receipt_long),
  more('/more', '더보기', Icons.menu);

  const AppTab(this.path, this.label, this.icon);

  final String path;
  final String label;
  final IconData icon;
}

/// 앱 셸 — 하단 탭 4개를 가진 화면들의 공통 껍데기.
///
/// go_router의 `StatefulShellRoute`가 탭별 네비게이션 스택을 따로 유지하고,
/// 이 위젯은 그 브랜치를 전환하는 하단 탭만 그린다.
class AppShell extends StatelessWidget {
  const AppShell({super.key, required this.navigationShell});

  final StatefulNavigationShell navigationShell;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: navigationShell,
      bottomNavigationBar: DecoratedBox(
        decoration: const BoxDecoration(
          border: Border(top: BorderSide(color: AppColors.border)),
        ),
        child: NavigationBar(
          selectedIndex: navigationShell.currentIndex,
          // 같은 탭을 다시 누르면 그 탭의 스택을 최상단으로 되돌린다.
          onDestinationSelected: (index) => navigationShell.goBranch(
            index,
            initialLocation: index == navigationShell.currentIndex,
          ),
          destinations: [
            for (final tab in AppTab.values)
              NavigationDestination(
                icon: Icon(tab.icon),
                label: tab.label,
              ),
          ],
        ),
      ),
    );
  }
}
