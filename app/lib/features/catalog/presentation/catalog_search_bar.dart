import 'package:flutter/material.dart';

import '../../../core/theme/tokens.dart';

/// 카탈로그 검색 바 (04-app-components §3.1 — h=44, 상단 고정, surfaceAlt 필드형).
///
/// 폼 입력이 아니라 탐색 도구라 [AppTextField]를 쓰지 않는다 — 라벨이 위에 붙지 않고,
/// 제출·검증 개념도 없다. 입력할 때마다 [onChanged]로 흘려보내고, 디바운스와 조회 시점은
/// 컨트롤러가 정한다 (구현 원칙 6).
class CatalogSearchBar extends StatefulWidget {
  const CatalogSearchBar({
    super.key,
    required this.onChanged,
    this.initialText = '',
  });

  final ValueChanged<String> onChanged;
  final String initialText;

  @override
  State<CatalogSearchBar> createState() => _CatalogSearchBarState();
}

class _CatalogSearchBarState extends State<CatalogSearchBar> {
  late final TextEditingController _controller = TextEditingController(
    text: widget.initialText,
  );

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _clear() {
    _controller.clear();
    widget.onChanged('');
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 44,
      child: TextField(
        controller: _controller,
        onChanged: widget.onChanged,
        textInputAction: TextInputAction.search,
        style: AppText.body.copyWith(color: AppColors.textBody),
        decoration: InputDecoration(
          hintText: '품명 · 품목코드 · 바코드 검색',
          filled: true,
          fillColor: AppColors.surfaceAlt,
          prefixIcon: const Icon(
            Icons.search,
            size: AppIconSize.inline,
            color: AppColors.textCaption,
          ),
          // 지우기 버튼은 입력이 있을 때만 — 빈 검색창에 조작 요소를 남기지 않는다.
          suffixIcon: ValueListenableBuilder<TextEditingValue>(
            valueListenable: _controller,
            builder: (context, value, _) => value.text.isEmpty
                ? const SizedBox.shrink()
                : IconButton(
                    icon: const Icon(Icons.close, size: AppIconSize.inline),
                    color: AppColors.textCaption,
                    onPressed: _clear,
                    tooltip: '검색어 지우기',
                  ),
          ),
          contentPadding: const EdgeInsets.symmetric(vertical: 0),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(AppRadius.sm),
            borderSide: BorderSide.none,
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(AppRadius.sm),
            borderSide: BorderSide.none,
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(AppRadius.sm),
            borderSide: const BorderSide(color: AppColors.borderStrong),
          ),
        ),
      ),
    );
  }
}
