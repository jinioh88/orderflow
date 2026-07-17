export function Spinner({ label = "불러오는 중" }: { label?: string }) {
  return (
    <div
      role="status"
      aria-label={label}
      className="flex items-center justify-center gap-2 p-8 text-sm text-gray-500"
    >
      <span className="size-4 animate-spin rounded-full border-2 border-gray-300 border-t-gray-700" />
      {label}…
    </div>
  );
}
