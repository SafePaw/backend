package com.ne7k.safepaw.walk.service;

public final class GeoUtils {
    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private GeoUtils() {}

    /** 두 위경도 사이 거리(m) — Haversine */
    public static double haversineMeters(double lng1, double lat1, double lng2, double lat2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** 두 포인트 사이 속도(km/h). dt(초) <= 0 이면 0 */
    public static double speedKmh(double meters, long seconds) {
        if (seconds <= 0) return 0;
        return (meters / 1000.0) / (seconds / 3600.0);
    }
}