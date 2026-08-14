package com.ne7k.safepaw.territory.domain;

import com.ne7k.safepaw.dog.domain.Dog;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.score.domain.Season;
import com.ne7k.safepaw.walk.domain.WalkSession;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Polygon;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "territories",
        indexes = @Index(name = "idx_territories_season", columnList = "season_key, area_square_meters DESC"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Territory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dog_id", nullable = false)
    private Dog dog;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "walk_session_id", nullable = false)
    private WalkSession walkSession;

    @Column(name = "geom", columnDefinition = "geometry(Polygon, 4326)", nullable = false)
    private Polygon geom;

    // 영토 면적
    @Column(name = "area_square_meters", nullable = false)
    private double areaSquareMeters;

    // 생명 주기
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TerritoryStatus status;

    // 시즌
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_key", nullable = false)
    private Season season;

    // 영토 차지한 시간
    @Column(name = "claimed_at", nullable = false)
    private OffsetDateTime claimedAt;

    // 빼앗긴 시간
    @Column(name = "conquered_at")
    private OffsetDateTime conqueredAt;

    // 신규 영토 생성
    public static Territory claim(Dog dog, WalkSession session, Season season,
                                  Polygon geom, double areaSquareMeters) {
        if (areaSquareMeters <= 0) throw new IllegalArgumentException("area must be > 0");
        Territory t = new Territory();
        t.dog = dog;
        t.walkSession = session;
        t.season = season;
        t.geom = geom;
        t.areaSquareMeters = areaSquareMeters;
        t.status = TerritoryStatus.ACTIVE;
        t.claimedAt = OffsetDateTime.now();
        return t;
    }

    // 전부 빼앗겼을 때
    public void markConquered() {
        if (status != TerritoryStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "ACTIVE 영토만 CONQUERED 처리 가능");
        }
        this.status = TerritoryStatus.CONQUERED;
        this.conqueredAt = OffsetDateTime.now();
    }

    // 부분 점령
    public void shrinkToRemainder(Polygon remainder, double remainderAreaSqm) {
        if (status != TerritoryStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "ACTIVE 영토만 분할 가능");
        }
        if (remainderAreaSqm <= 0) {
            markConquered();
            return;
        }
        this.geom = remainder;
        this.geom.setSRID(4326);
        this.areaSquareMeters = remainderAreaSqm;
    }

    public void replaceGeom(Polygon newGeom, double newAreaSquareMeters) {
        if (status != TerritoryStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "ACTIVE 영토만 갱신 가능");
        }
        if (newAreaSquareMeters <= 0) {
            throw new IllegalArgumentException("area must be > 0");
        }
        this.geom = newGeom;
        this.geom.setSRID(4326);
        this.areaSquareMeters = newAreaSquareMeters;
    }

    // 소유권 확인
    public boolean isOwnedBy(Long userId) { return dog.isOwnedBy(userId); }
}