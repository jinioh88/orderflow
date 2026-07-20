import localFont from "next/font/local";

/**
 * Pretendard (01-foundations §2). npm 패키지의 가변 폰트를 자가 호스팅한다 —
 * CDN 의존 없이 내부망 배포가 가능하고, 가변 1파일(2MB)이 static 4굵기(3.1MB)보다 작다.
 * 400/500/600/700을 이 한 파일이 모두 커버한다.
 */
export const pretendard = localFont({
  src: "../node_modules/pretendard/dist/web/variable/woff2/PretendardVariable.woff2",
  weight: "45 920",
  display: "swap",
  variable: "--font-pretendard",
});
