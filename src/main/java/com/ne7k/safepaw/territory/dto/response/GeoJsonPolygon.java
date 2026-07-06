package com.ne7k.safepaw.territory.dto.response;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import java.util.Arrays;
import java.util.List;

/** PostGIS/JTS Polygon → GeoJSON (GeoJSON 클라이언트가 소비) */
public record GeoJsonPolygon(String type, List<List<List<Double>>> coordinates) {

    public static GeoJsonPolygon from(Polygon polygon) {
        Coordinate[] ring = polygon.getExteriorRing().getCoordinates();
        List<List<Double>> outer = Arrays.stream(ring)
                .map(c -> List.of(c.x, c.y))   // [lng, lat]
                .toList();
        return new GeoJsonPolygon("Polygon", List.of(outer));
    }
}