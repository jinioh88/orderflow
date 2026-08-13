import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/application/auth_controller.dart';
import '../../features/auth/presentation/login_screen.dart';
import '../../features/auth/presentation/password_setup_screen.dart';
import '../../features/auth/presentation/splash_screen.dart';
import '../../features/catalog/presentation/catalog_screen.dart';
import '../../features/home/presentation/home_screen.dart';
import '../../features/more/presentation/more_screen.dart';
import '../../features/order/presentation/order_history_screen.dart';
import '../widgets/app_shell.dart';

/// 셸(하단 탭) 밖에 있는 경로들 — 로그인 전·비밀번호 설정 단계.
abstract final class AppRoutes {
  static const splash = '/splash';
  static const login = '/login';
  static const passwordSetup = '/password-setup';

  /// 로그인 후 처음 보게 되는 화면. M1의 첫 업무 화면은 카탈로그(발주 탭)다.
  static final home = AppTab.order.path;
}

/// 앱 라우터.
///
/// 로그인·비밀번호 설정 화면은 셸 밖에 있고(탭 없음), 나머지는 [AppShell]의 하단 탭 4개
/// (홈/발주/내역/더보기 — 04-app-components §1.1) 아래에 놓인다.
/// 탭마다 네비게이션 스택을 따로 유지하려고 `StatefulShellRoute`를 쓴다.
///
/// **인증 가드는 `redirect` 한 곳에만 둔다.** 화면이 각자 이동을 지시하면 규칙이 흩어져서,
/// 자동 로그인·임시 비밀번호 강제 이동 같은 조건이 화면마다 어긋나기 쉽다.
final appRouterProvider = Provider<GoRouter>((ref) {
  // go_router는 Listenable이 울릴 때만 redirect를 다시 계산한다.
  // 인증 상태가 바뀌면(로그인·로그아웃·토큰 만료) 이 값을 흔들어 재평가시킨다.
  final authChanged = ValueNotifier<int>(0);
  ref.listen(authControllerProvider, (_, _) => authChanged.value++);
  ref.onDispose(authChanged.dispose);

  return GoRouter(
    initialLocation: AppRoutes.splash,
    refreshListenable: authChanged,
    redirect: (context, state) {
      final status = ref.read(authControllerProvider).status;
      final location = state.matchedLocation;

      return switch (status) {
        // 세션 복원 중 — 아직 어느 쪽으로도 보내지 않는다.
        AuthStatus.initializing =>
          location == AppRoutes.splash ? null : AppRoutes.splash,
        AuthStatus.unauthenticated =>
          location == AppRoutes.login ? null : AppRoutes.login,
        // 임시 비밀번호 상태(api-spec 2.3)에서는 다른 화면 진입을 전부 되돌린다.
        AuthStatus.passwordSetupRequired =>
          location == AppRoutes.passwordSetup ? null : AppRoutes.passwordSetup,
        // 로그인된 사용자가 로그인·설정 화면에 남아 있지 않게 한다.
        AuthStatus.authenticated =>
          const {
                AppRoutes.splash,
                AppRoutes.login,
                AppRoutes.passwordSetup,
              }.contains(location)
              ? AppRoutes.home
              : null,
      };
    },
    routes: [
      GoRoute(
        path: AppRoutes.splash,
        builder: (context, state) => const SplashScreen(),
      ),
      GoRoute(
        path: AppRoutes.login,
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: AppRoutes.passwordSetup,
        builder: (context, state) => const PasswordSetupScreen(),
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
