package com.orderflow.api.common.response;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 응답 시간 변환 — 스펙 1.1의 "ISO-8601 오프셋 포함"을 지키기 위해
 * DB의 LocalDateTime을 KST 오프셋으로 변환한다 (저장 시각이 Asia/Seoul 기준).
 */
public final class KstTimes {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private KstTimes() {
    }

    public static OffsetDateTime toOffset(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(KST).toOffsetDateTime();
    }
}
