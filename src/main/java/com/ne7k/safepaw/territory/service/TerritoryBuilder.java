package com.ne7k.safepaw.territory.service;

import com.ne7k.safepaw.territory.config.TerritoryProperties;
import com.ne7k.safepaw.territory.repository.TerritoryRepository;
import com.ne7k.safepaw.walk.domain.Geometries;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Component;

import com.ne7k.safepaw.territory.dto.response.GeoJsonPolygon;
import org.locationtech.jts.geom.Geometry;

@Component
@RequiredArgsConstructor
public class TerritoryBuilder {

    private final TerritoryRepository territoryRepository;
    private final TerritoryProperties props;

    /** walkId 의 포인트들로 concave hull 폴리곤(WKT) 생성. 폴리곤이 아니면 null */
    public String buildHullWkt(Long walkId) {
        String wkt = territoryRepository.buildConcaveHullWkt(
                walkId,
                props.concaveHullTargetPercent(),
                props.hullSimplifyToleranceDegrees());
        if (wkt == null || !wkt.startsWith("POLYGON")) {
            // 점이 일직선이거나 너무 적으면 LINESTRING/POINT 가 나옴 → 영토 불가
            return null;
        }
        return wkt;
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