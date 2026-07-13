package com.ne7k.safepaw.ranking.domain;

public enum RankingCategory {
    XP,                 // 시즌 xp
    TERRITORY,          // 영토 - active 면접 합
    DISTANCE,           // 완료 산책 거리 합
    DURATION;            // 완료 산책 시간 합 (sec)

    public String unit() {
        return switch (this) {
            case XP -> "xp";
            case TERRITORY ->  "m2";
            case DISTANCE -> "m";
            case DURATION -> "seconds";
        };
    }
}
