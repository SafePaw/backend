package com.ne7k.safepaw.auth.repository;

import com.ne7k.safepaw.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // 폐기되지 않은 refresh 토큰 폐기
    @Modifying // 수정 쿼리 명시
    @Query("update RefreshToken r set r.revokedAt = :now " +
            "where r.user.id = :userId and r.revokedAt is null")
    int revokeAllByUserId(@Param("userId") Long userId, @Param("now") OffsetDateTime now);
}
