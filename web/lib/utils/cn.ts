/** 조건부 클래스명 결합. 뒤에 오는 클래스가 우선하도록 호출부에서 순서를 지킨다. */
export function cn(
  ...parts: Array<string | false | null | undefined>
): string {
  return parts.filter(Boolean).join(" ");
}
