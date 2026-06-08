package com.ne7k.safepaw.user.domain;

import com.ne7k.safepaw.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    // 회원 id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 최초 가입 소셜
    // primary auth provider - 이름 문자열 참고
    @Enumerated(EnumType.STRING)
    @Column(name = "primary_auth_provider", nullable = false, length = 10)
    private AuthProvider primaryAuthProvider;

    // 이메일
    @Column(name = "email", length = 255)
    private String email;

    // 닉네임
    @Column(name = "nickname", nullable = false, length = 20, unique = true)
    private String nickname;

    // 권한
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private UserRole role;

    // 계정 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private UserStatus status;

    // 마지막 로그인 시각
    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    // 수정 및 생성 시각은 BasicTimeEntity로 대체

    @Builder
    private User(AuthProvider primaryAuthProvider, String email, String nickname) {
        this.primaryAuthProvider = primaryAuthProvider;
        this.email = (email == null) ? null : email.toLowerCase(); // email null이면 null 아니면, 소문자
        this.nickname = nickname;
        this.role = UserRole.USER;
        this.status = UserStatus.ACTIVE;
    }

    public static User createSocial(AuthProvider provider, String email, String nickname) {
        return User.builder()
                .primaryAuthProvider(provider)
                .email(email)
                .nickname(nickname)
                .build();
    }

    // 로그인 성공시 현재 시각으로 변경
    public void updateLastLoginAt() {
        this.lastLoginAt = OffsetDateTime.now();
    }

}
