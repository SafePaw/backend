package com.ne7k.safepaw.auth.domain;

import com.ne7k.safepaw.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.optional.qual.Present;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "social_identities",
        // 소셜 계정 1개 = safepaw 계정 1개 제약
        // 한 사람당 하나의 provider 연결
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_social_identity_provider",      columnNames = {"provider", "provider_user_id"}),
                @UniqueConstraint(name = "uq_social_identity_user_provider", columnNames = {"user_id", "provider"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialIdentity {

    // 연결 id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 회원 id
    // N:1 - 여러 플랫폼 > 하나의 계정, user notnull
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // lazy로 user 테이블까지는 미조회
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // OAuth 제공자
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 10)
    private SocialProvider provider;

    // 제공자 회원 식별자
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    // 연결 시각
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // DB 저장 직전 created 채우기
    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }

    @Builder
    private SocialIdentity(User user, SocialProvider provider, String providerUserId) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

}
