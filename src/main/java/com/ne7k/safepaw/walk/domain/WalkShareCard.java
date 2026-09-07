package com.ne7k.safepaw.walk.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "walk_share_cards",
        uniqueConstraints = @UniqueConstraint(name = "uq_walk_share_cards_walk",
                columnNames = "walk_session_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalkShareCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "walk_session_id", nullable = false)
    private WalkSession walkSession;

    /** MinIO key (share-cards 버킷). 클라이언트가 배경 이미지 업로드 후 전달 */
    @Column(name = "background_image_key", length = 500)
    private String backgroundImageKey;

    /** MinIO key (share-cards 버킷). 클라이언트가 렌더링 완성 카드 업로드 후 전달 */
    @Column(name = "rendered_image_key", length = 500)
    private String renderedImageKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static WalkShareCard create(WalkSession session,
                                       String backgroundImageKey,
                                       String renderedImageKey) {
        WalkShareCard card = new WalkShareCard();
        card.walkSession = session;
        card.backgroundImageKey = backgroundImageKey;
        card.renderedImageKey = renderedImageKey;
        card.createdAt = OffsetDateTime.now();
        card.updatedAt = card.createdAt;
        return card;
    }

    public void update(String backgroundImageKey, String renderedImageKey) {
        if (backgroundImageKey != null) {
            this.backgroundImageKey = backgroundImageKey;
        }
        if (renderedImageKey != null) {
            this.renderedImageKey = renderedImageKey;
        }
        this.updatedAt = OffsetDateTime.now();
    }
}
