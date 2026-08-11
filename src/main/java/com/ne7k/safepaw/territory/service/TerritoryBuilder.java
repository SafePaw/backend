package com.ne7k.safepaw.territory.service;

import com.ne7k.safepaw.territory.config.TerritoryProperties;
import com.ne7k.safepaw.territory.dto.response.GeoJsonPolygon;
import com.ne7k.safepaw.territory.repository.TerritoryRepository;
import com.ne7k.safepaw.walk.domain.Geometries;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TerritoryBuilder {

    private final TerritoryRepository territoryRepository;
    private final TerritoryProperties props;

    /** walkId 포인트 → 개선된 concave hull WKT. 폴리곤 아니면 null */
    public String buildHullWkt(Long walkId) {
        String wkt = territoryRepository.buildConcaveHullWkt(
                walkId,
                props.concaveHullTargetPercent(),
                props.hullSimplifyToleranceDegrees());
        if (wkt == null) {
            return null;
        }
        String trimmed = wkt.trim();
        // MakeValid 결과가 MultiPolygon일 수 있음 → 면적 최대 Polygon만 허용하려면 상위 처리
        if (trimmed.startsWith("POLYGON")) {
            return trimmed;
        }
        if (trimmed.startsWith("MULTIPOLYGON")) {
            // 단순화: 첫 번째 링만 쓰지 말고, 면적 판정은 Repository에 맡기거나 null
            // set9: MultiPolygon이면 일단 null → INSUFFICIENT (또는 ST_CollectionExtract를 SQL에서 Polygon만)
            return null;
        }
        return null;
    }

    public Polygon parse(String wkt) {
        try {
            Polygon poly = (Polygon) new WKTReader(Geometries.FACTORY).read(wkt);
            poly.setSRID(Geometries.SRID);
            return poly;
        } catch (Exception e) {
            throw new IllegalStateException("폴리곤 WKT 파싱 실패 : " + wkt, e);
        }
    }

    public GeoJsonPolygon toGeoJsonPolygon(Geometry geom) {
        if (geom == null || geom.isEmpty()) {
            return null;
        }
        if (geom instanceof Polygon polygon) {
            return GeoJsonPolygon.from(polygon);
        }
        throw new IllegalArgumentException(
                "GeoJSON 변환은 Polygon만 지원 : " + geom.getGeometryType());
    }
}
