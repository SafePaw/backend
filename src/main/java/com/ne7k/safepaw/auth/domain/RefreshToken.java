package com.ne7k.safepaw.auth.domain;

import com.ne7k.safepaw.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    // 토큰 id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 회원 id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 토큰 해시
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    // 발급 시각
    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    // 만료 시각
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    // 폐기 시각
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    // 교체된 다음 토큰 id
    @Column(name = "replaced_by_id")
    private Long replacedById;

    // 클라이언트 정보
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    // ip 주소

    @Builder
    private RefreshToken(User user, String tokenHash, OffsetDateTime issuedAt,
                         OffsetDateTime expiresAt, String userAgent) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.userAgent = userAgent;
    }

    // 현재 토큰 사용 가능 여부 (아직 폐기 안되고 만료 전)
    public boolean isActive(OffsetDateTime now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    // 폐기 시간이 되었는지 체크
    public boolean isRevoked() {
        return revokedAt != null;
    }

    // 토큰 유지
    public void revoke(OffsetDateTime now) {
        if (this.revokedAt == null) {
            this.revokedAt = now;
        }
    }

    // 토큰 교체
    public void replaceWith(Long newTokenId, OffsetDateTime now) {
        this.replacedById = newTokenId;
        this.revokedAt = now;
    }
}
