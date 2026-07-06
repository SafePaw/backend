package com.ne7k.safepaw.score.domain;

import com.ne7k.safepaw.dog.domain.Dog;
import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.walk.domain.WalkSession;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "xp_ledger",
        indexes = {
                @Index(name = "idx_xp_ledger_ranking", columnList = "season_key, dog_id"),
                @Index(name = "idx_xp_ledger_dog",     columnList = "dog_id, season_key"),
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class XpLedger {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dog_id", nullable = false)
    private Dog dog;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_key", nullable = false)
    private Season season;

    // xp source
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private XpSource source;

    // xp 양
    @Column(nullable = false)
    private int amount;

    // xp 여러개 산책 1개
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ref_walk_session_id")
    private WalkSession refWalkSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ref_territory_id")
    private Territory refTerritory;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // xpLedger
    public static XpLedger of(Dog dog, Season season, XpSource source, int amount,
                              WalkSession refWalk, Territory refTerritory) {
        if (amount < 0) throw new IllegalArgumentException("xp는 마이너스가 될 수 없습니다. amount 음수");
        XpLedger x = new XpLedger();
        x.dog = dog;
        x.season = season;
        x.source = source;
        x.amount = amount;
        x.refWalkSession = refWalk;
        x.refTerritory = refTerritory;
        x.createdAt = OffsetDateTime.now();
        return x;
    }

}
