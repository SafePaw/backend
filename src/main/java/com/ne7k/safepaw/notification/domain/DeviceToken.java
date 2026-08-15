package com.ne7k.safepaw.notification.domain;

import com.ne7k.safepaw.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "device_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uq_device_tokens_token", columnNames = "token"),
        indexes = @Index(name = "idx_device_tokens_user", columnList = "user_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DevicePlatform platform;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static DeviceToken of(User user, DevicePlatform platform, String token) {
        DeviceToken dt = new DeviceToken();
        dt.user = user;
        dt.platform = platform;
        dt.token = token;
        dt.createdAt = OffsetDateTime.now();
        dt.updatedAt = dt.createdAt;
        return dt;
    }

    public void refreshOwnership(User user, DevicePlatform platform) {
        this.user = user;
        this.platform = platform;
        this.updatedAt = OffsetDateTime.now();
    }
}