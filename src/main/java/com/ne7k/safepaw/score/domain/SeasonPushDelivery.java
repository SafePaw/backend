package com.ne7k.safepaw.score.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "season_push_deliveries",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_season_push",
                columnNames = {"user_id", "season_key", "reminder_kind"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeasonPushDelivery {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "season_key", nullable = false, length = 7)
    private String seasonKey;

    @Column(name = "reminder_kind", nullable = false, length = 4)
    private String reminderKind;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    public static SeasonPushDelivery record(Long userId, String seasonKey, String reminderKind) {
        SeasonPushDelivery d = new SeasonPushDelivery();
        d.userId = userId;
        d.seasonKey = seasonKey;
        d.reminderKind = reminderKind;
        d.sentAt = OffsetDateTime.now();
        return d;
    }
}