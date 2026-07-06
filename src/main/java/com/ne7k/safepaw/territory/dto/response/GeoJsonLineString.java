package com.ne7k.safepaw.territory.dto.response;

import java.util.List;

/** walk_points 시계열 → GeoJSON LineString (산책 경로 polyline) */
public record GeoJsonLineString(String type, List<List<Double>> coordinates) {

    /** WalkPointRepository.findLngLatByWalkSessionId 결과 Object[][] 변환 */
    public static GeoJsonLineString from(List<Object[]> rows) {
        List<List<Double>> coords = rows.stream()
                .map(r -> List.of(((Number) r[0]).doubleValue(), ((Number) r[1]).doubleValue()))
                .toList();
        return new GeoJsonLineString("LineString", coords);
    }
}