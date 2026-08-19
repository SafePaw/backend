package com.ne7k.safepaw.territory.dto.response;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** JTS Polygon / MultiPolygon → GeoJSON (holes · MultiPolygon 지원) */
public record GeoJsonGeometry(String type, Object coordinates) {

    public static GeoJsonGeometry from(Geometry geom) {
        if (geom == null || geom.isEmpty()) {
            return new GeoJsonGeometry("MultiPolygon", List.of());
        }
        if (geom instanceof Polygon p) {
            return fromPolygon(p);
        }
        if (geom instanceof MultiPolygon mp) {
            return fromMultiPolygon(mp);
        }
        // [수정] GeometryCollection 등 기타 타입 — Polygon/MultiPolygon 부분만 추출
        if (geom instanceof GeometryCollection gc) {
            List<List<List<List<Double>>>> polys = new ArrayList<>();
            for (int i = 0; i < gc.getNumGeometries(); i++) {
                Geometry part = gc.getGeometryN(i);
                if (part instanceof Polygon p) {
                    polys.add(polygonRings(p));
                } else if (part instanceof MultiPolygon mp) {
                    for (int j = 0; j < mp.getNumGeometries(); j++) {
                        polys.add(polygonRings((Polygon) mp.getGeometryN(j)));
                    }
                }
            }
            return new GeoJsonGeometry("MultiPolygon", polys);
        }
        throw new IllegalArgumentException("GeoJSON 변환 미지원 : " + geom.getGeometryType());
    }

    public static GeoJsonGeometry fromPolygon(Polygon polygon) {
        List<List<List<Double>>> rings = new ArrayList<>();
        rings.add(ringCoords(polygon.getExteriorRing()));
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            rings.add(ringCoords(polygon.getInteriorRingN(i)));
        }
        return new GeoJsonGeometry("Polygon", rings);
    }

    public static GeoJsonGeometry fromMultiPolygon(MultiPolygon multi) {
        List<List<List<List<Double>>>> polys = new ArrayList<>();
        for (int i = 0; i < multi.getNumGeometries(); i++) {
            polys.add(polygonRings((Polygon) multi.getGeometryN(i)));
        }
        return new GeoJsonGeometry("MultiPolygon", polys);
    }

    private static List<List<List<Double>>> polygonRings(Polygon p) {
        List<List<List<Double>>> rings = new ArrayList<>();
        rings.add(ringCoords(p.getExteriorRing()));
        for (int h = 0; h < p.getNumInteriorRing(); h++) {
            rings.add(ringCoords(p.getInteriorRingN(h)));
        }
        return rings;
    }

    private static List<List<Double>> ringCoords(LineString ring) {
        Coordinate[] cs = ring.getCoordinates();
        return Arrays.stream(cs)
                .map(c -> List.of(c.x, c.y))
                .toList();
    }
}