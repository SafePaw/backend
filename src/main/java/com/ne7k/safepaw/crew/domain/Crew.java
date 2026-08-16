package com.ne7k.safepaw.crew.domain;

import com.ne7k.safepaw.global.audit.BaseTimeEntity;
import com.ne7k.safepaw.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "crews",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_crews_name", columnNames = "name"),
                @UniqueConstraint(name = "uq_crews_invite_code", columnNames = "invite_code")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Crew extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "image_key", length = 500)
    private String imageKey;

    @Column(name = "territory_color", nullable = false, length = 7)
    private String territoryColor;

    @Column(name = "invite_code", nullable = false, length = 8)
    private String inviteCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leader_user_id", nullable = false)
    private User leader;

    public static Crew create(User leader, String name, String imageKey,
                              String territoryColor, String inviteCode) {
        Crew c = new Crew();
        c.leader = leader;
        c.name = name.trim();
        c.imageKey = imageKey;
        c.territoryColor = normalizeHex(territoryColor);
        c.inviteCode = inviteCode;
        return c;
    }

    public void updateProfile(String name, String imageKey, String territoryColor) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (imageKey != null) {
            this.imageKey = imageKey.isBlank() ? null : imageKey;
        }
        if (territoryColor != null && !territoryColor.isBlank()) {
            this.territoryColor = normalizeHex(territoryColor);
        }
    }

    public void rotateInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public void transferLeader(User next) {
        this.leader = next;
    }

    public boolean isLedBy(Long userId) {
        return leader != null && leader.getId().equals(userId);
    }

    public static String normalizeHex(String raw) {
        String v = raw.startsWith("#") ? raw : "#" + raw;
        return v.toUpperCase();
    }
}
