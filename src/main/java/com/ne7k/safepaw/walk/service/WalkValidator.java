package com.ne7k.safepaw.walk.service;

import com.ne7k.safepaw.walk.config.WalkProperties;
import com.ne7k.safepaw.walk.repository.redis.RedisWalkPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WalkValidator {

    private final WalkProperties props;

    /**
     * 정확도/속도/점프 기준으로 유효 포인트만 추린다.
     * @param prevLng/prevLat/prevAt 직전 유효 포인트(Redis state). 없으면 null.
     * @return 통과한 포인트 + 직전→현재 누적 거리(m)
     */
    public Result filter(Double prevLng, Double prevLat, java.time.OffsetDateTime prevAt,
                         List<RedisWalkPoint> batch) {
        WalkProperties.Gps g = props.gps();
        List<RedisWalkPoint> accepted = new ArrayList<>();
        double addedMeters = 0;

        Double pLng = prevLng, pLat = prevLat;
        java.time.OffsetDateTime pAt = prevAt;

        for (RedisWalkPoint p : batch) {
            // ① 정확도
            if (p.accuracyMeters() > g.maxAccuracyMeters()) continue;

            if (pLng != null && pAt != null) {
                // getSeconds()는 1초 미만을 0으로 버려 고빈도 GPS가 전부 폐기되거나 속도가 왜곡됨
                double dtSec = Duration.between(pAt, p.recordedAt()).toMillis() / 1000.0;
                if (dtSec <= 0) continue; // 시간 역행/중복
                double meters = GeoUtils.haversineMeters(pLng, pLat, p.lng(), p.lat());

                // ④ 점프(텔레포트)
                if (meters > g.jumpDistanceMeters() && dtSec < g.jumpMinIntervalSeconds()) continue;

                // GPS 정지 배회(3~8m)가 도보 속도로 잡혀 거리·칼로리가 부풀어 오르는 것 방지
                if (meters < g.minStepMeters()) continue;

                // ②③ 속도 구간 (영토 면적 왜곡 방지). 정지(<min)는 거리만 0 처리하고 점은 유지해도 되나
                //      MVP 는 단순히 상·하한 벗어나면 폐기.
                double kmh = GeoUtils.speedKmh(meters, dtSec);
                if (kmh < g.minSpeedKmh() || kmh > g.maxSpeedKmh()) continue;

                addedMeters += meters;
            }

            accepted.add(p);
            pLng = p.lng(); pLat = p.lat(); pAt = p.recordedAt();
        }
        return new Result(accepted, addedMeters);
    }

    public record Result(List<RedisWalkPoint> accepted, double addedMeters) {}
}
