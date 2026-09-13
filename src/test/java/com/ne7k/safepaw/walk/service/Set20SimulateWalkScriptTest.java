package com.ne7k.safepaw.walk.service;

import com.ne7k.safepaw.territory.service.TerritoryMarker;
import com.ne7k.safepaw.walk.repository.redis.RedisWalkPoint;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * scripts/simulate_walk.py 와 같은 사각형 루프·제자리 배회를 validator/칼로리/마커에 그대로 넣는다.
 */
class Set20SimulateWalkScriptTest {

    private static final double ORIGIN_LNG = 126.8235;
    private static final double ORIGIN_LAT = 37.4812;
    private static final double METERS_PER_DEG_LAT = 111_320.0;

    private final WalkValidator validator = new WalkValidator(CalorieCalculatorTest.walkProps(1.1));
    private final CalorieCalculator calories = new CalorieCalculator(CalorieCalculatorTest.walkProps(1.1));

    @Test
    void simulateWalkDefaultRectangle_distanceAndCaloriesAreSane() {
        // simulate_walk.py 기본값: 80x50m, 4km/h, 5s → step ≈ 5.56m > min-step 5
        List<RedisWalkPoint> points = rectangleLoop(80, 50, 5, 4.0);
        WalkValidator.Result result = validator.filter(null, null, null, points);

        double raw = rawPathMeters(points);
        assertThat(result.accepted().size()).isGreaterThan(10);
        assertThat(result.addedMeters()).isCloseTo(raw, within(raw * 0.08));
        assertThat(result.addedMeters()).isBetween(240.0, 280.0); // 둘레 260m 근방

        double kcal10 = calories.kcal(null, result.addedMeters());
        double expected = Math.round(10.0 * (result.addedMeters() / 1000.0) * 1.1 * 10.0) / 10.0;
        assertThat(kcal10).isEqualTo(expected);
        assertThat(kcal10).isBetween(2.0, 4.0);

        double kcal8 = calories.kcal(new BigDecimal("8.00"), result.addedMeters());
        assertThat(kcal8).isLessThan(kcal10);
        assertThat(kcal8).isGreaterThan(1.5);
    }

    @Test
    void oneHzWalkStillAccumulatesDistanceDespiteMinStep() {
        // 앱 실측: 1초마다 포인트, 4km/h → 1.11m < min-step 5. prev를 안 바꾸면 5m마다 적산되어야 함
        List<RedisWalkPoint> points = eastLine(100, 1, 4.0);
        WalkValidator.Result result = validator.filter(null, null, null, points);

        assertThat(result.addedMeters()).isBetween(90.0, 110.0);
        assertThat(calories.kcal(new BigDecimal("10"), result.addedMeters()))
                .isCloseTo(1.1, within(0.2));
    }

    @Test
    void standingJitterDoesNotInflateCalories() {
        OffsetDateTime t0 = OffsetDateTime.parse("2026-09-13T12:00:00+09:00");
        List<RedisWalkPoint> points = new ArrayList<>();
        points.add(p(ORIGIN_LNG, ORIGIN_LAT, t0));
        for (int i = 1; i <= 40; i++) {
            double[] off = offsetMeters(2.5, i * 33);
            points.add(p(ORIGIN_LNG + off[0], ORIGIN_LAT + off[1], t0.plusSeconds(i)));
        }

        WalkValidator.Result result = validator.filter(null, null, null, points);
        assertThat(result.addedMeters()).isLessThan(5.0);
        assertThat(calories.kcal(new BigDecimal("8"), result.addedMeters())).isLessThan(0.1);
    }

    @Test
    void incrementalLiveDistanceMatchesFinishRefilterOnScriptPath() {
        List<RedisWalkPoint> all = rectangleLoop(80, 50, 5, 4.0);
        double incremental = 0;
        RedisWalkPoint prev = all.get(0);
        for (int i = 1; i < all.size(); i++) {
            WalkValidator.Result batch = validator.filter(
                    prev.lng(), prev.lat(), prev.recordedAt(), List.of(all.get(i)));
            incremental += batch.addedMeters();
            if (!batch.accepted().isEmpty()) {
                prev = batch.accepted().get(batch.accepted().size() - 1);
            }
        }
        double full = validator.filter(null, null, null, all).addedMeters();
        assertThat(incremental).isEqualTo(full);
        assertThat(calories.kcal(new BigDecimal("8"), incremental))
                .isEqualTo(calories.kcal(new BigDecimal("8"), full));
    }

    @Test
    void markerOfLargestPartIsInsideLargeIslandNotSmallOne() {
        GeometryFactory gf = new GeometryFactory();
        Polygon small = square(gf, ORIGIN_LNG, ORIGIN_LAT, 0.0003);
        Polygon large = square(gf, ORIGIN_LNG + 0.01, ORIGIN_LAT + 0.01, 0.002);
        var multi = gf.createMultiPolygon(new Polygon[]{small, large});

        TerritoryMarker.Point2d pt = TerritoryMarker.ofLargestPart(multi);
        assertThat(pt).isNotNull();
        assertThat(large.covers(gf.createPoint(new Coordinate(pt.lng(), pt.lat())))).isTrue();
        assertThat(small.covers(gf.createPoint(new Coordinate(pt.lng(), pt.lat())))).isFalse();
    }

    private static List<RedisWalkPoint> rectangleLoop(
            double widthM, double heightM, int intervalSec, double speedKmh) {
        double stepM = (speedKmh / 3.6) * intervalSec;
        List<double[]> corners = List.of(
                new double[]{0, 0},
                new double[]{widthM, 0},
                new double[]{widthM, heightM},
                new double[]{0, heightM},
                new double[]{0, 0}
        );
        List<double[]> sampled = resample(corners, stepM);
        OffsetDateTime t0 = OffsetDateTime.parse("2026-09-13T12:00:00+09:00");
        List<RedisWalkPoint> points = new ArrayList<>();
        for (int i = 0; i < sampled.size(); i++) {
            double[] ll = toLngLat(sampled.get(i)[0], sampled.get(i)[1]);
            points.add(p(ll[0], ll[1], t0.plusSeconds((long) i * intervalSec)));
        }
        return points;
    }

    private static List<RedisWalkPoint> eastLine(double meters, int intervalSec, double speedKmh) {
        double stepM = (speedKmh / 3.6) * intervalSec;
        int n = Math.max(2, (int) Math.round(meters / stepM) + 1);
        OffsetDateTime t0 = OffsetDateTime.parse("2026-09-13T12:00:00+09:00");
        List<RedisWalkPoint> points = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double[] ll = toLngLat(i * stepM, 0);
            points.add(p(ll[0], ll[1], t0.plusSeconds((long) i * intervalSec)));
        }
        return points;
    }

    private static List<double[]> resample(List<double[]> vertices, double stepM) {
        List<double[]> out = new ArrayList<>();
        out.add(vertices.get(0));
        double remaining = stepM;
        for (int i = 0; i < vertices.size() - 1; i++) {
            double e0 = vertices.get(i)[0], n0 = vertices.get(i)[1];
            double e1 = vertices.get(i + 1)[0], n1 = vertices.get(i + 1)[1];
            double seg = Math.hypot(e1 - e0, n1 - n0);
            if (seg < 1e-6) continue;
            double de = (e1 - e0) / seg, dn = (n1 - n0) / seg;
            double pos = 0;
            while (remaining <= seg - pos + 1e-9) {
                pos += remaining;
                out.add(new double[]{e0 + de * pos, n0 + dn * pos});
                remaining = stepM;
            }
            remaining -= (seg - pos);
        }
        out.add(vertices.get(vertices.size() - 1));
        return out;
    }

    private static double[] toLngLat(double eastM, double northM) {
        double dlat = northM / METERS_PER_DEG_LAT;
        double dlng = eastM / (METERS_PER_DEG_LAT * Math.cos(Math.toRadians(ORIGIN_LAT)));
        return new double[]{ORIGIN_LNG + dlng, ORIGIN_LAT + dlat};
    }

    private static double[] offsetMeters(double meters, double headingDeg) {
        double heading = Math.toRadians(headingDeg);
        double dLat = (meters * Math.cos(heading)) / METERS_PER_DEG_LAT;
        double dLng = (meters * Math.sin(heading))
                / (METERS_PER_DEG_LAT * Math.cos(Math.toRadians(ORIGIN_LAT)));
        return new double[]{dLng, dLat};
    }

    private static double rawPathMeters(List<RedisWalkPoint> points) {
        double sum = 0;
        for (int i = 1; i < points.size(); i++) {
            RedisWalkPoint a = points.get(i - 1), b = points.get(i);
            sum += GeoUtils.haversineMeters(a.lng(), a.lat(), b.lng(), b.lat());
        }
        return sum;
    }

    private static RedisWalkPoint p(double lng, double lat, OffsetDateTime at) {
        return new RedisWalkPoint(lng, lat, 8f, 4f, at);
    }

    private static Polygon square(GeometryFactory gf, double lng, double lat, double size) {
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
