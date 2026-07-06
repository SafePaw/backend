package com.ne7k.safepaw.walk.repository;

import com.ne7k.safepaw.walk.domain.WalkSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalkSessionRepository extends JpaRepository<WalkSession, Long> {

    // 소유권 검증 동시 조회
    Optional<WalkSession> findByIdAndDog_Owner_Id(Long id, Long userId);

    // 동일 강아지의 진행 중 세션
    @Query("select w from WalkSession w where w.dog.id = :dogId and w.status = 'ONGOING'")
    Optional<WalkSession> findOngoingByDogId(@Param("dogId") Long dogId);
}