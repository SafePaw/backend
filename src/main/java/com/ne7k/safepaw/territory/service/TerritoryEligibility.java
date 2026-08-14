package com.ne7k.safepaw.territory.service;

import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.territory.config.TerritoryProperties;
import com.ne7k.safepaw.territory.repository.TerritoryRepository;
import com.ne7k.safepaw.walk.config.WalkProperties;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TerritoryEligibility {

    private final TerritoryRepository territoryRepository;
    private final TerritoryBuilder builder;
    private final TerritoryProperties tProps;
    private final WalkProperties wProps;

    public Outcome evaluate(Long dogId, Long walkId, int durationSeconds, int validPointCount) {

        int minMinutes = Math.max(1, wProps.session().minDurationSeconds() / 60);
        if (durationSeconds < wProps.session().minDurationSeconds()) {
            return Outcome.ineligible(ErrorCode.WALK_TOO_SHORT,
                    minMinutes + "분 이상 산책해야 영토가 생겨요. (현재 "
                            + (durationSeconds / 60) + "분)");
        }
        if (validPointCount < tProps.minPoints()) {
            return Outcome.ineligible(ErrorCode.TERRITORY_INSUFFICIENT_POINTS,
                    "유효 GPS 포인트가 부족해요.");
        }
        Double loopGap = territoryRepository.loopGapMeters(walkId);
        if (loopGap == null || loopGap > tProps.loopCloseMeters()) {
            double gap = loopGap == null ? -1 : loopGap;
            return Outcome.ineligible(ErrorCode.TERRITORY_LOOP_NOT_CLOSED,
                    String.format("시작점과 %.1fm 떨어져 종료됐어요. %.0fm 이내로 돌아오면 영토가 생겨요.",
                            gap, tProps.loopCloseMeters()), gap);
        }
        String wkt = builder.buildHullWkt(walkId);
        if (wkt == null) {
            return Outcome.ineligible(ErrorCode.TERRITORY_INSUFFICIENT_POINTS,
                    "경로가 면을 이루지 못했어요.", loopGap);
        }
        double area = territoryRepository.areaSquareMeters(wkt);
        if (area < tProps.minAreaSquareMeters()) {
            return Outcome.ineligible(ErrorCode.TERRITORY_TOO_SMALL,
                    String.format("영토가 너무 좁아요. (%.0f㎡)", area), loopGap);
        }
        if (territoryRepository.isNarrowerThan(wkt, tProps.minWidthMeters() / 2.0)) {
            return Outcome.ineligible(ErrorCode.TERRITORY_TOO_NARROW,
                    String.format("루프 폭이 %.0fm 미만이에요.", tProps.minWidthMeters()), loopGap);
        }
        if (territoryRepository.countRecentDuplicates(dogId, wkt, tProps.duplicateWindowHours()) > 0) {
            return Outcome.ineligible(ErrorCode.TERRITORY_DUPLICATE,
                    "24시간 내 같은 영역이 이미 인정됐어요.", loopGap);
        }

        Polygon polygon = builder.parse(wkt);
        return Outcome.eligible(polygon, wkt, area, loopGap);
    }

    public record Outcome(
            boolean eligible,
            Polygon polygon, String wkt, Double areaSquareMeters,
            ErrorCode reason, String message, Double loopGapMeters
    ) {
        public static Outcome eligible(Polygon p, String wkt, double area, double gap) {
            return new Outcome(true, p, wkt, area, null, null, gap);
        }
        public static Outcome ineligible(ErrorCode reason, String message) {
            return new Outcome(false, null, null, null, reason, message, null);
        }
        public static Outcome ineligible(ErrorCode reason, String message, Double gap) {
            return new Outcome(false, null, null, null, reason, message, gap);
        }
    }
}
