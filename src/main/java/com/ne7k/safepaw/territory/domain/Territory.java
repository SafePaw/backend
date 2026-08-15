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
import org.locationtech.jts.geom.MultiPolygon;

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

    /** set11: 단일 POLYGON도 MULTIPOLYGON 1개로 저장 */
    @Column(name = "geom", columnDefinition = "geometry(MultiPolygon, 4326)", nullable = false)
    private MultiPolygon geom;

    @Column(name = "area_square_meters", nullable = false)
    private double areaSquareMeters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TerritoryStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_key", nullable = false)
    private Season season;

    @Column(name = "claimed_at", nullable = false)
    private OffsetDateTime claimedAt;

    @Column(name = "conquered_at")
    private OffsetDateTime conqueredAt;

    public static Territory claim(Dog dog, WalkSession session, Season season,
                                  MultiPolygon geom, double areaSquareMeters) {
        if (areaSquareMeters <= 0) throw new IllegalArgumentException("area must be > 0");
        Territory t = new Territory();
        t.dog = dog;
        t.walkSession = session;
        t.season = season;
        t.geom = geom;
        t.geom.setSRID(4326);
        t.areaSquareMeters = areaSquareMeters;
        t.status = TerritoryStatus.ACTIVE;
        t.claimedAt = OffsetDateTime.now();
        return t;
    }

    public void markConquered() {
        if (status != TerritoryStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "ACTIVE 영토만 CONQUERED 처리 가능");
        }
        this.status = TerritoryStatus.CONQUERED;
        this.conqueredAt = OffsetDateTime.now();
    }

    /** set11: Difference 잔여 — Polygon 또는 MultiPolygon */
    public void shrinkToRemainder(MultiPolygon remainder, double remainderAreaSqm) {
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

    /** set9 merge: Union 결과 geom·면적 교체 */
    public void replaceGeom(MultiPolygon newGeom, double newAreaSquareMeters) {
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

    public boolean isOwnedBy(Long userId) { return dog.isOwnedBy(userId); }
}