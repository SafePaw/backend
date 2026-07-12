package com.ne7k.safepaw.walk.repository.redis;

import java.time.OffsetDateTime;
import java.util.Map;

public record WalkState(
        long userId,                    // 산책 주인
        long dogId,                     // 산책 중인 강아지
        OffsetDateTime startedAt,       // 산책 시작 시간
        Double lastLng,                 // 가장 최근 GPS 경도
        Double lastLat,                 // 가장 최근 GPS 위도
        OffsetDateTime lastAt,          // 최근 포인트가 찍힌 시각
        Double prevLng,                 // 바로 이전 유효 포인트 경도 - 속도 계산용
        Double prevLat,                 // 바로 이전 유효 포인트 위도 - 속도 계산용
        OffsetDateTime prevAt,          // 바로 이전 포인트 시각
        int pointCount,                 // 지금까지 반영된 포인트 개수
        double totalMeters,             // 누적 이동 거리
        OffsetDateTime pausedAt,        // 일시정지 시작 시각 - null인 상태이면 진행 중
        long totalPausedSeconds,        // 지금까지 일시정지한 시간 합 (sec)
        Double minLng,                  // 경로 bbox 서쪽 - 최소 경도
        Double maxLng,                  // 경로 bbox 동쪽 - 최대 경도
        Double minLat,                  // 경로 bbox 남쪽 - 최소 위도
        Double maxLat                   // 경로 bbox 북쪽 - 최대 위도
) {

    // 완전한 GPS 한 점
    public boolean hasLast() { return lastLng != null && lastLat != null && lastAt != null; }

    // 속도 = 거리 나누기 시간
    public boolean hasPrev() { return prevLng != null && prevLat != null && prevAt != null; }

    // 퍼즈 시각이 있는지
    public boolean isPaused() { return pausedAt != null; }

    public static WalkState from(Map<Object, Object> h) {
        return new WalkState(
                Long.parseLong((String) h.get("userId")),
                Long.parseLong((String) h.get("dogId")),
                OffsetDateTime.parse((String) h.get("startedAt")),
                parseDouble(h, "lastLng"),
                parseDouble(h, "lastLat"),
                parseOffsetDateTime(h, "lastAt"),
                parseDouble(h, "prevLng"),
                parseDouble(h, "prevLat"),
                parseOffsetDateTime(h, "prevAt"),
                Integer.parseInt((String) h.getOrDefault("pointCount", "0")),
                Double.parseDouble((String) h.getOrDefault("totalMeters", "0")),
                parseOffsetDateTime(h, "pausedAt"),
                Long.parseLong((String) h.getOrDefault("totalPausedSeconds", "0")),
                parseDouble(h, "minLng"),
                parseDouble(h, "maxLng"),
                parseDouble(h, "minLat"),
                parseDouble(h, "maxLat")
        );
    }

    private static Double parseDouble(Map<Object, Object> h, String key) {
        Object v = h.get(key);
        return v == null ? null : Double.parseDouble((String) v);
    }

    private static OffsetDateTime parseOffsetDateTime(Map<Object, Object> h, String key) {
        Object v = h.get(key);
        return v == null ? null : OffsetDateTime.parse((String) v);
    }
}
