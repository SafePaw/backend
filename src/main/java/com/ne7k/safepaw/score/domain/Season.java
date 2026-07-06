package com.ne7k.safepaw.score.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;


@Entity
@Getter
@Table(name = "seasons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Season {

    // 'YYYY-MM' 숫자 대신 문자열 - 해당 영토가 어느 달 것인지 파악하기 위해서
    @Id @Column(length = 7)
    private String key;

    // 시즌 시작일
    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    // 시즌 종료일
    @Column(name = "ended_at", nullable = false)
    private OffsetDateTime endedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static Season of(String key, OffsetDateTime startedAt, OffsetDateTime endedAt) {
        Season s = new Season();
        s.key = key;
        s.startedAt = startedAt;
        s.endedAt = endedAt;
        s.createdAt = OffsetDateTime.now();
        return s;
    }

}
