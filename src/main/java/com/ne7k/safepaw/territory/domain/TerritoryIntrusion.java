package com.ne7k.safepaw.territory.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Getter
@Table(name = "territory_intrusions",
        indexes = {
                @Index(name = "idx_intrusions_victim",   columnList = "victim_territory_id"),
                @Index(name = "idx_intrusions_intruder", columnList = "intruder_territory_id"),
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TerritoryIntrusion {

    // 침범 기록 pk
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 피해 영토
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "victim_territory_id", nullable = false)
    private Territory victimTerritory;

    // 침범 영토
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intruder_territory_id", nullable = false)
    private Territory intruderTerritory;

    // 피해 영토 대비 겹친 비율
    @Column(name = "overlap_ratio", nullable = false)
    private double overlapRatio;

    // 뺏긴 조각
    @Column(name = "overlap_geom", columnDefinition = "geometry(Geometry, 4326)", nullable = false)
    private org.locationtech.jts.geom.Geometry overlapGeom;

    // 침범 발생 시각 - 종료 기점
    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    // 침범 기록 객체
    public static TerritoryIntrusion record(Territory victim, Territory intruder,
                                            double overlap, org.locationtech.jts.geom.Geometry overlapGeom) {
        // 피해 영토 침범 영토가 같은 row x
        if (Objects.equals(victim.getId(), intruder.getId()))
            throw new IllegalArgumentException("victim and intruder must differ");

        // overlap 0 초과 1 이하
        if (overlap <= 0 || overlap > 1)
            throw new IllegalArgumentException("overlap must be in (0,1]");

        // 실제 조각이 있는지 체크
        if (overlapGeom == null || overlapGeom.isEmpty())
            throw new IllegalArgumentException("overlapGeom required");

        TerritoryIntrusion i = new TerritoryIntrusion();
        i.victimTerritory = victim;
        i.intruderTerritory = intruder;
        i.overlapRatio = overlap;
        i.overlapGeom = overlapGeom;
        i.overlapGeom.setSRID(4326);
        i.occurredAt = OffsetDateTime.now();
        return i;
    }
}