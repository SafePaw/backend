package com.ne7k.safepaw.territory.service;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PartialConquestRemainderTest {

    private final GeometryFactory gf = new GeometryFactory();

    @Test
    void geometryCollectionRemainderKeepsPolygonsInsteadOfWiping() {
        Polygon keep = square(127.00, 37.50, 0.002);
        Polygon also = square(127.03, 37.50, 0.001);
        Geometry gc = gf.createGeometryCollection(new Geometry[]{keep, also});

        MultiPolygon remainder = PartialConquestService.remainderMultiPolygon(gc);

        assertThat(remainder).isNotNull();
        assertThat(remainder.getNumGeometries()).isEqualTo(2);
        TerritoryMarker.Point2d marker = TerritoryMarker.ofLargestPart(remainder);
        assertThat(keep.covers(gf.createPoint(new Coordinate(marker.lng(), marker.lat())))).isTrue();
        assertThat(marker.lng()).isCloseTo(127.001, within(0.001));
    }

    @Test
    void emptyOrLineOnlyRemainderIsNull() {
        assertThat(PartialConquestService.remainderMultiPolygon(null)).isNull();
        assertThat(PartialConquestService.remainderMultiPolygon(gf.createMultiPolygon())).isNull();
        Geometry line = gf.createLineString(new Coordinate[]{
                new Coordinate(127, 37), new Coordinate(127.001, 37)
        });
        assertThat(PartialConquestService.remainderMultiPolygon(line)).isNull();
    }

    @Test
    void polygonAndMultiPolygonPassThrough() {
        Polygon one = square(127.0, 37.5, 0.001);
        assertThat(PartialConquestService.remainderMultiPolygon(one).getNumGeometries()).isEqualTo(1);
        MultiPolygon multi = gf.createMultiPolygon(new Polygon[]{one, square(127.02, 37.5, 0.001)});
        assertThat(PartialConquestService.remainderMultiPolygon(multi).getNumGeometries()).isEqualTo(2);
    }

    private Polygon square(double lng, double lat, double size) {
        Coordinate[] ring = new Coordinate[]{
                new Coordinate(lng, lat),
                new Coordinate(lng + size, lat),
                new Coordinate(lng + size, lat + size),
                new Coordinate(lng, lat + size),
                new Coordinate(lng, lat)
        };
        return gf.createPolygon(gf.createLinearRing(ring));
    }
}
