package com.ne7k.safepaw.territory.service;

import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.territory.domain.TerritoryStatus;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * 영토 마커 좌표. DB에 저장하지 않고 호출 시점의 geom으로 매번 다시 계산한다.
 * 쟁탈(Difference)·병합(Union)으로 크기·모양이 바뀌면 다음 조회부터 새 중앙이 나간다.
 * MultiPolygon이면 면적이 가장 큰 조각의 centroid. 도형 밖이면 interior point.
 * CONQUERED 영토는 핀을 내리지 않는다 (옛 전체 도형 중심에 찍히는 것을 막음).
 */
public final class TerritoryMarker {

    private static final double COORD_SCALE = 10_000_000.0;

    private TerritoryMarker() {}

    public record Point2d(double lng, double lat) {}

    public static Point2d ofActive(Territory t) {
        if (t == null || t.getStatus() != TerritoryStatus.ACTIVE) {
            return null;
        }
        return ofLargestPart(t.getGeom());
    }

    public static Point2d ofLargestPart(Geometry geom) {
        Polygon part = largestPolygon(geom);
        if (part == null || part.isEmpty()) {
            return null;
        }
        Point candidate = part.getCentroid();
        if (candidate == null || candidate.isEmpty() || !part.covers(candidate)) {
            candidate = part.getInteriorPoint();
        }
        if (candidate == null || candidate.isEmpty()) {
            return null;
        }
        return new Point2d(roundCoord(candidate.getX()), roundCoord(candidate.getY()));
    }

    static Polygon largestPolygon(Geometry geom) {
        if (geom == null || geom.isEmpty()) {
            return null;
        }
        if (geom instanceof Polygon p) {
            return p;
        }
        if (!(geom instanceof MultiPolygon) && !(geom instanceof GeometryCollection)) {
            return null;
        }
        Polygon best = null;
        double bestArea = -1;
        for (int i = 0; i < geom.getNumGeometries(); i++) {
            Polygon part = largestPolygon(geom.getGeometryN(i));
            if (part == null) {
                continue;
            }
            double area = part.getArea();
            if (area > bestArea) {
                bestArea = area;
                best = part;
            }
        }
        return best;
    }

    private static double roundCoord(double value) {
        return Math.round(value * COORD_SCALE) / COORD_SCALE;
    }
}
