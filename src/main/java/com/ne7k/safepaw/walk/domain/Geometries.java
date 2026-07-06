package com.ne7k.safepaw.walk.domain;

import org.locationtech.jts.geom.*;

public final class Geometries {

    // 표준 공간 참조 식별자
    public static final int SRID = 4326;

    // Geometry 객체 생성
    public static final GeometryFactory FACTORY = new GeometryFactory(
            new PrecisionModel(), SRID
    );

    // 생성자 제한
    private Geometries() {}

    // 경도 lng, 위도 lat 값을 실수로 받아 점으로 생성해 반환
    public static Point point(double lng, double lat) {
        Point p = FACTORY.createPoint(new Coordinate(lng, lat));
        p.setSRID(SRID);
        return p;
    }
}
