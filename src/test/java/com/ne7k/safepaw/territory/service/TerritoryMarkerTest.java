package com.ne7k.safepaw.territory.service;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TerritoryMarkerTest {

    private final GeometryFactory gf = new GeometryFactory();

    @Test
    void emptyGeomReturnsNull() {
        assertThat(TerritoryMarker.ofLargestPart(null)).isNull();
        assertThat(TerritoryMarker.ofLargestPart(gf.createMultiPolygon())).isNull();
    }

    @Test
    void singlePolygonUsesCentroid() {
        Polygon square = square(127.0, 37.5, 0.002);
        TerritoryMarker.Point2d pt = TerritoryMarker.ofLargestPart(square);

        assertThat(pt).isNotNull();
        assertThat(pt.lng()).isCloseTo(127.001, within(0.0002));
        assertThat(pt.lat()).isCloseTo(37.501, within(0.0002));
        assertThat(square.covers(gf.createPoint(new Coordinate(pt.lng(), pt.lat())))).isTrue();
    }

    @Test
    void multiPolygonPicksLargestPartNotSmallIsland() {
        Polygon small = square(127.00, 37.50, 0.0004);
        Polygon large = square(127.02, 37.52, 0.003);
        MultiPolygon multi = gf.createMultiPolygon(new Polygon[]{small, large});

        TerritoryMarker.Point2d pt = TerritoryMarker.ofLargestPart(multi);

        assertThat(pt).isNotNull();
        assertThat(large.covers(gf.createPoint(new Coordinate(pt.lng(), pt.lat())))).isTrue();
        assertThat(small.covers(gf.createPoint(new Coordinate(pt.lng(), pt.lat())))).isFalse();
        assertThat(pt.lng()).isCloseTo(127.0215, within(0.0005));
        assertThat(pt.lat()).isCloseTo(37.5215, within(0.0005));
    }

    @Test
    void concavePolygonFallsBackToInteriorPoint() {
        // C자 오목 폴리곤: centroid가 빈 공간으로 빠질 수 있음
        Coordinate[] ring = new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(6, 0),
                new Coordinate(6, 1),
                new Coordinate(1, 1),
                new Coordinate(1, 5),
                new Coordinate(6, 5),
                new Coordinate(6, 6),
                new Coordinate(0, 6),
                new Coordinate(0, 0)
        };
        LinearRing shell = gf.createLinearRing(new CoordinateArraySequence(ring));
        Polygon concave = gf.createPolygon(shell, null);

        TerritoryMarker.Point2d pt = TerritoryMarker.ofLargestPart(concave);

        assertThat(pt).isNotNull();
        assertThat(concave.covers(gf.createPoint(new Coordinate(pt.lng(), pt.lat())))).isTrue();
    }

    @Test
    void recomputesAfterPartialConquestCutsLargestIsland() {
        Polygon west = square(127.00, 37.50, 0.003);
        Polygon east = square(127.03, 37.50, 0.001);
        MultiPolygon before = gf.createMultiPolygon(new Polygon[]{west, east});
        TerritoryMarker.Point2d beforePt = TerritoryMarker.ofLargestPart(before);

        assertThat(west.covers(gf.createPoint(new Coordinate(beforePt.lng(), beforePt.lat())))).isTrue();

        Polygon westCut = square(127.00, 37.50, 0.0005);
        MultiPolygon after = gf.createMultiPolygon(new Polygon[]{westCut, east});
        TerritoryMarker.Point2d afterPt = TerritoryMarker.ofLargestPart(after);

        assertThat(afterPt.lng()).isNotCloseTo(beforePt.lng(), within(0.001));
        assertThat(east.covers(gf.createPoint(new Coordinate(afterPt.lng(), afterPt.lat())))).isTrue();
        assertThat(westCut.covers(gf.createPoint(new Coordinate(afterPt.lng(), afterPt.lat())))).isFalse();
    }

    @Test
    void recomputesCentroidWhenSinglePieceShrinks() {
        Polygon full = square(127.0, 37.5, 0.004);
        Polygon remain = square(127.0, 37.5, 0.001);
        TerritoryMarker.Point2d before = TerritoryMarker.ofLargestPart(full);
        TerritoryMarker.Point2d after = TerritoryMarker.ofLargestPart(remain);

        assertThat(after.lng()).isLessThan(before.lng());
        assertThat(after.lat()).isLessThan(before.lat());
        assertThat(remain.covers(gf.createPoint(new Coordinate(after.lng(), after.lat())))).isTrue();
    }

    @Test
    void recomputesAfterMergeGrowsTheShape() {
        Polygon a = square(127.00, 37.50, 0.002);
        Polygon b = square(127.0015, 37.50, 0.002);
        Geometry union = a.union(b);
        TerritoryMarker.Point2d onlyA = TerritoryMarker.ofLargestPart(a);
        TerritoryMarker.Point2d merged = TerritoryMarker.ofLargestPart(union);

        assertThat(merged.lng()).isGreaterThan(onlyA.lng());
        assertThat(union.covers(gf.createPoint(new Coordinate(merged.lng(), merged.lat())))).isTrue();
    }

    @Test
    void geometryCollectionPicksLargestPolygon() {
        Polygon small = square(127.00, 37.50, 0.0004);
        Polygon large = square(127.02, 37.52, 0.003);
        Geometry gc = gf.createGeometryCollection(new Geometry[]{small, large});
        TerritoryMarker.Point2d pt = TerritoryMarker.ofLargestPart(gc);
        assertThat(large.covers(gf.createPoint(new Coordinate(pt.lng(), pt.lat())))).isTrue();
        assertThat(small.covers(gf.createPoint(new Coordinate(pt.lng(), pt.lat())))).isFalse();
    }

    @Test
    void donutUsesInteriorPointWhenCentroidFallsInHole() {
        Polygon outer = square(0, 0, 6);
        Polygon hole = square(2, 2, 2);
        Polygon donut = gf.createPolygon(outer.getExteriorRing(), new LinearRing[]{hole.getExteriorRing()});
        TerritoryMarker.Point2d pt = TerritoryMarker.ofLargestPart(donut);
        assertThat(pt).isNotNull();
        assertThat(donut.covers(gf.createPoint(new Coordinate(pt.lng(), pt.lat())))).isTrue();
        assertThat(hole.covers(gf.createPoint(new Coordinate(pt.lng(), pt.lat())))).isFalse();
    }

    @Test
    void roundsTo7Decimals() {
        Polygon odd = square(127.012345678, 37.512345678, 0.001);
        TerritoryMarker.Point2d pt = TerritoryMarker.ofLargestPart(odd);
        assertThat(pt.lng()).isEqualTo(Math.round(pt.lng() * 10_000_000.0) / 10_000_000.0);
        assertThat(pt.lat()).isEqualTo(Math.round(pt.lat() * 10_000_000.0) / 10_000_000.0);
    }

    @Test
    void ofActiveNullIsNull() {
        assertThat(TerritoryMarker.ofActive(null)).isNull();
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
