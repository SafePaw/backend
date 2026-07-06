package com.ne7k.safepaw.walk.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "walk_points",
        indexes = @Index(name = "idx_walk_points_session_time", columnList = "walk_session_id, recorded_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalkPoint {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어느 산책에 속하는지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "walk_session_id", nullable = false)
    private WalkSession walkSession;

    // 위치
    @Column(name = "geom", columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point geom;

    // gps 정확도 - 검증용
    @Column(name = "accuracy_meters", nullable = false)
    private float accuracyMeters;

    // 클라가 보낸 속도
    @Column(name = "speed_kmh")
    private Float speedKmh;

    // GPS 찍힌 시각
    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    // 점을 만들 때의 규칙
    public static WalkPoint of(WalkSession session, double lng, double lat,
                               float accuracyMeters, Float speedKmh, OffsetDateTime recordedAt) {
        WalkPoint p = new WalkPoint();
        p.walkSession = session;
        p.geom = Geometries.point(lng, lat);
        p.accuracyMeters = accuracyMeters;
        p.speedKmh = speedKmh;
        p.recordedAt = recordedAt;
        return p;
    }

    // geom에서 좌표 꺼내기
    public double getLng() { return geom.getX(); }
    public double getLat() { return geom.getY(); }
}
