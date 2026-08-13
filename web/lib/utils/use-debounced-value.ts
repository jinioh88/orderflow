"use client";

import { useEffect, useState } from "react";

/**
 * 타이핑이 멎은 뒤에야 값을 넘긴다 — 텍스트 필터가 글자마다 조회를 날리는 것을 막는다.
 * 한글 IME는 조합 중에도 input 이벤트가 나므로 자모 단위 요청까지 흡수한다.
 */
export function useDebouncedValue<T>(value: T, delayMs = 300): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);

  return debounced;
}
