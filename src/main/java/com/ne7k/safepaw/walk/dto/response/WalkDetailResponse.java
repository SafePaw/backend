package com.ne7k.safepaw.walk.dto.response;

import com.ne7k.safepaw.territory.dto.response.GeoJsonLineString;
import com.ne7k.safepaw.walk.domain.WalkSession;
import java.time.OffsetDateTime;
import java.util.List;

public record WalkDetailResponse(
        Long id, Long dogId, String status,
        OffsetDateTime startedAt, OffsetDateTime endedAt,
        WalkStats stats,
        GeoJsonLineString polyline,   // walk_points 시계열 (set4-api.md §4.5)
        Long territoryId
) {
    /** WalkSessionService.detail() 에서 호출 — polyline 은 Repository에서 별도 조회 후 주입 */
    public static WalkDetailResponse from(WalkSession w, List<Object[]> lngLatRows, Long territoryId) {
        return new WalkDetailResponse(
                w.getId(), w.getDog().getId(), w.getStatus().name(),
                w.getStartedAt(), w.getEndedAt(),
                new WalkStats(
                        w.getDistanceMeters() == null ? 0 : w.getDistanceMeters(),
                        w.getDurationSeconds() == null ? 0 : w.getDurationSeconds(),
                        0, w.getPointCount(), null),
                lngLatRows.isEmpty() ? null : GeoJsonLineString.from(lngLatRows),
                territoryId);  // w.getRefTerritory() 삭제
    }
}