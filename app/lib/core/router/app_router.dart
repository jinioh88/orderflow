import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/presentation/login_screen.dart';
import '../../features/catalog/presentation/catalog_screen.dart';

/// 앱 라우터.
///
/// 인증 가드(`redirect`)는 AUTH 태스크에서 인증 상태 Provider와 함께 붙인다.
/// Phase 0에서는 화면 뼈대 간 이동만 정의한다.
final appRouterProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/login',
    routes: [
      GoRoute(
        path: '/login',
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: '/catalog',
        builder: (context, state) => const CatalogScreen(),
      ),
    ],
  );
});
