package com.ne7k.safepaw.territory.dto.response;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** JTS Polygon / MultiPolygon → GeoJSON (holes · MultiPolygon 지원) */
public record GeoJsonGeometry(String type, Object coordinates) {

    public static GeoJsonGeometry from(Geometry geom) {
        if (geom instanceof Polygon p) {
            return fromPolygon(p);
        }
        if (geom instanceof MultiPolygon mp) {
            return fromMultiPolygon(mp);
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
            Polygon p = (Polygon) multi.getGeometryN(i);
            List<List<List<Double>>> one = new ArrayList<>();
            one.add(ringCoords(p.getExteriorRing()));
            for (int h = 0; h < p.getNumInteriorRing(); h++) {
                one.add(ringCoords(p.getInteriorRingN(h)));
            }
            polys.add(one);
        }
        return new GeoJsonGeometry("MultiPolygon", polys);
    }

    private static List<List<Double>> ringCoords(LineString ring) {
        Coordinate[] cs = ring.getCoordinates();
        return Arrays.stream(cs)
                .map(c -> List.of(c.x, c.y))
                .toList();
    }
}