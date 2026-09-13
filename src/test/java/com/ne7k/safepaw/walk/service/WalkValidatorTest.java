package com.ne7k.safepaw.walk.service;

import com.ne7k.safepaw.walk.config.WalkProperties;
import com.ne7k.safepaw.walk.repository.redis.RedisWalkPoint;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WalkValidatorTest {

    private static final double LNG = 127.0276;
    private static final double LAT = 37.4979;

    private final WalkValidator validator = new WalkValidator(CalorieCalculatorTest.walkProps(1.1));

    @Test
    void gpsJitterUnderMinStepDoesNotAddDistance() {
        OffsetDateTime t0 = OffsetDateTime.parse("2026-09-13T12:00:00+09:00");
        List<RedisWalkPoint> points = new ArrayList<>();
        points.add(p(LNG, LAT, t0));
        // 원점 주변 2.5m 배회. 예전엔 구간 속도 4~8km/h로 통과해 거리·칼로리가 부풀어 올랐음
        for (int i = 1; i <= 20; i++) {
            double[] offset = offsetMeters(2.5, i * 30);
            points.add(p(LNG + offset[0], LAT + offset[1], t0.plusSeconds(i * 2L)));
        }

        WalkValidator.Result result = validator.filter(null, null, null, points);

        assertThat(result.addedMeters()).isLessThan(5.0);
    }

    @Test
    void realWalkSegmentOfAbout5mIn1_8sIsKept() {
        // getSeconds() 절사 버그: 1.8s → 1s 로 보면 5.5m가 19.8km/h → 폐기됐음
        OffsetDateTime t0 = OffsetDateTime.parse("2026-09-13T12:00:00+09:00");
        double[] step = offsetMeters(5.5, 90);
        List<RedisWalkPoint> points = List.of(
                p(LNG, LAT, t0),
                p(LNG + step[0], LAT + step[1], t0.plusNanos(1_800_000_000L))
        );

        WalkValidator.Result result = validator.filter(null, null, null, points);

        assertThat(result.accepted()).hasSize(2);
        assertThat(result.addedMeters()).isGreaterThan(5.0);
        assertThat(result.addedMeters()).isLessThan(7.0);
    }

    @Test
    void caloriesMatchBetweenIncrementalAndFullRefilter() {
        OffsetDateTime t0 = OffsetDateTime.parse("2026-09-13T12:00:00+09:00");
        List<RedisWalkPoint> all = new ArrayList<>();
        all.add(p(LNG, LAT, t0));
        double lng = LNG;
        double lat = LAT;
        for (int i = 1; i <= 10; i++) {
            double[] step = offsetMeters(6.0, 90);
            lng += step[0];
            lat += step[1];
            all.add(p(lng, lat, t0.plusSeconds(i * 3L)));
        }

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

        CalorieCalculator calc = new CalorieCalculator(CalorieCalculatorTest.walkProps(1.1));
        assertThat(calc.kcal(new java.math.BigDecimal("8"), incremental))
                .isEqualTo(calc.kcal(new java.math.BigDecimal("8"), full));
    }

    private static RedisWalkPoint p(double lng, double lat, OffsetDateTime at) {
        return new RedisWalkPoint(lng, lat, 8f, null, at);
    }

    /** headingDeg 방향으로 meters만큼 이동한 lng/lat 오프셋 */
    private static double[] offsetMeters(double meters, double headingDeg) {
        double heading = Math.toRadians(headingDeg);
        double dLat = (meters * Math.cos(heading)) / 111_320.0;
        double dLng = (meters * Math.sin(heading)) / (111_320.0 * Math.cos(Math.toRadians(LAT)));
        return new double[]{dLng, dLat};
    }
}
