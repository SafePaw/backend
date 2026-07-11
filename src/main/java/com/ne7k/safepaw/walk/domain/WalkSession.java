package com.ne7k.safepaw.walk.domain;

import com.ne7k.safepaw.dog.domain.Dog;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "walk_sessions", indexes = @Index(name = "idx_walk_sessions_dog", columnList = "dog_id, started_at DESC"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalkSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 강아지 없이는 산책 불가
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dog_id", nullable = false)
    private Dog dog;

    // 산책 생명주기 ongoing 상태에서만 gps 업로드 및 finish 가능
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalkStatus status;

    // 산책 시작 및 종료
    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    // point 개수
    @Column(name = "point_count", nullable = false)
    private int pointCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 산책 시작
    public static WalkSession start(Dog dog) {
        WalkSession w = new WalkSession();
        w.dog = dog;
        w.status = WalkStatus.ONGOING;
        w.startedAt = OffsetDateTime.now();
        w.createdAt = w.startedAt;
        w.pointCount = 0;
        return w;
    }

    // 산책 완료
    public void complete(double distanceMeters, int durationSeconds, int pointCount) {
        // 강제 재개
        if (status == WalkStatus.PAUSED) {
            this.status = WalkStatus.ONGOING;
        }
        // status ongoing일 때에만 finish 가능하게
        if (status != WalkStatus.ONGOING) {
            throw new BusinessException(ErrorCode.WALK_ALREADY_FINISHED);
        }
        this.status = WalkStatus.COMPLETED;
        this.endedAt = OffsetDateTime.now();
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.pointCount = pointCount;
    }

    // 산책 중지
    public void pause() {
        if (status != WalkStatus.ONGOING) {
            throw new BusinessException(ErrorCode.WALK_NOT_ONGOING);
        }
        this.status = WalkStatus.PAUSED;
    }

    // 산책 재개
    public void resume() {
        if (status != WalkStatus.PAUSED) {
            throw new BusinessException(ErrorCode.WALK_NOT_PAUSED);
        }
        this.status = WalkStatus.ONGOING;
    }

    // 산책 포기 - ongoing, paused 상태일 때에도 포기할 수 있게
    public void abort() {
        if (status != WalkStatus.ONGOING && status != WalkStatus.PAUSED) return;
        this.status = WalkStatus.ABORTED;
        this.endedAt = OffsetDateTime.now();
    }

    // 산책의 강아지 주인이 요청 사용자인지 체크
    public boolean isOwnedBy(Long userId) { return dog.isOwnedBy(userId); }

    // 상태 확인
    public boolean isOngoing() { return status == WalkStatus.ONGOING; }
    public boolean isPaused() { return status == WalkStatus.PAUSED; }
    public boolean isActive() { return status == WalkStatus.ONGOING || status == WalkStatus.PAUSED; }

}
