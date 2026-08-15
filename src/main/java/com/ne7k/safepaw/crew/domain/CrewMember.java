package com.ne7k.safepaw.crew.domain;

import com.ne7k.safepaw.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "crew_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_crew_members_user", columnNames = "user_id"),
                @UniqueConstraint(name = "uq_crew_members_crew_user", columnNames = {"crew_id", "user_id"})
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CrewRole role;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    public static CrewMember leaderOf(Crew crew, User user) {
        return of(crew, user, CrewRole.LEADER);
    }

    public static CrewMember memberOf(Crew crew, User user) {
        return of(crew, user, CrewRole.MEMBER);
    }

    private static CrewMember of(Crew crew, User user, CrewRole role) {
        CrewMember m = new CrewMember();
        m.crew = crew;
        m.user = user;
        m.role = role;
        m.joinedAt = OffsetDateTime.now();
        return m;
    }

    public void changeRole(CrewRole role) {
        this.role = role;
    }
}
