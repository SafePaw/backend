package com.ne7k.safepaw.dog.domain;

import com.ne7k.safepaw.global.audit.BaseTimeEntity;
import com.ne7k.safepaw.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "dogs", indexes = @Index(name = "idx_dogs_user", columnList = "user_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dog extends BaseTimeEntity {

    // 강아지 Id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 보호자 Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    // 이름
    @Column(nullable = false, length = 20)
    private String name;

    // 견종
    @Column(length = 50)
    private String breed;

    // 성별
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    // 나이
    @Column(name = "age")
    private Integer age;

    // kg
    @Column(name = "weight_kg", precision = 5, scale = 2)
    private java.math.BigDecimal weightKg;

    // 마커 이미지 url
    @Column(name = "marker_image_url", length = 500)
    private String markerImageKey;

    // 색
    @Column(name = "territory_color", length = 7)
    private String territoryColor;

    // 랭크
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DogRank rank;

    // 누적 xp
    @Column(name = "total_xp", nullable = false)
    private int totalXp;

    // dog 객체 생성
    public static Dog create(User owner, String name, String breed, Gender gender, Integer age,
                             java.math.BigDecimal weightKg, String markerImageKey, String territoryColor) {
        Dog dog = new Dog();
        dog.owner = owner;
        dog.name = name;
        dog.breed = breed;
        dog.gender = gender;
        dog.age = age;
        dog.weightKg = weightKg;
        dog.markerImageKey = markerImageKey;
        dog.territoryColor = territoryColor;
        dog.rank = DogRank.initial();
        dog.totalXp = 0;
        return dog;
    }

    // dog upadate
    public void updateProfile(String name, String breed, Gender gender, Integer age,
                              java.math.BigDecimal weightKg, String markerImageKey, String territoryColor) {
        if (name != null) this.name = name;
        if (breed != null) this.breed = breed;
        if (gender != null) this.gender = gender;
        if (age != null) this.age = age;
        if (weightKg != null) this.weightKg = weightKg;
        if (markerImageKey != null) this.markerImageKey = markerImageKey;
        if (territoryColor != null) this.territoryColor = territoryColor;
    }

    /** XP 누적. Service가 promote()를 별도 호출 */
    public void gainXp(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must be non-negative");
        this.totalXp += amount;
    }
    public void promote(DogRank newRank) {
        this.rank = newRank;
    }
    // 소유권 검사
    public boolean isOwnedBy(Long userId) {
        return owner.getId().equals(userId);
    }

}
