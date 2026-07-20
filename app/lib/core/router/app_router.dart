import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/presentation/login_screen.dart';
import '../../features/catalog/presentation/catalog_screen.dart';
import '../../features/home/presentation/home_screen.dart';
import '../../features/more/presentation/more_screen.dart';
import '../../features/order/presentation/order_history_screen.dart';
import '../widgets/app_shell.dart';

/// 앱 라우터.
///
/// 로그인 화면은 셸 밖에 있고(탭 없음), 나머지는 [AppShell]의 하단 탭 4개
/// (홈/발주/내역/더보기 — 04-app-components §1.1) 아래에 놓인다.
/// 탭마다 네비게이션 스택을 따로 유지하려고 `StatefulShellRoute`를 쓴다.
///
/// 인증 가드(`redirect`)는 AUTH 태스크에서 인증 상태 Provider와 함께 붙인다.
final appRouterProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/login',
    routes: [
      GoRoute(
        path: '/login',
        builder: (context, state) => const LoginScreen(),
      ),
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) =>
            AppShell(navigationShell: navigationShell),
        branches: [
          _branch(AppTab.home, (context, state) => const HomeScreen()),
          _branch(AppTab.order, (context, state) => const CatalogScreen()),
          _branch(
            AppTab.history,
            (context, state) => const OrderHistoryScreen(),
          ),
          _branch(AppTab.more, (context, state) => const MoreScreen()),
        ],
      ),
    ],
  );
});

StatefulShellBranch _branch(AppTab tab, GoRouterWidgetBuilder builder) {
  return StatefulShellBranch(
    routes: [GoRoute(path: tab.path, builder: builder)],
  );
}
