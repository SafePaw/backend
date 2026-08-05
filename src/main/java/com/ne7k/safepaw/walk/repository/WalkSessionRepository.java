package com.ne7k.safepaw.walk.repository;

import com.ne7k.safepaw.walk.domain.WalkSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WalkSessionRepository extends JpaRepository<WalkSession, Long> {

    // 소유권 검증 동시 조회
    Optional<WalkSession> findByIdAndDog_Owner_Id(Long id, Long userId);

    /** 동일 강아지 활성 세션 (ONGOING | PAUSED) — 시작 중복 방지 */
    @Query("""
            select w from WalkSession w
            where w.dog.id = :dogId
              and w.status in ('ONGOING', 'PAUSED')
            """)
    Optional<WalkSession> findActiveByDogId(@Param("dogId") Long dogId);

    /** 유저 소유 강아지의 활성 산책 목록 */
    @Query("""
            select w from WalkSession w
            join fetch w.dog d
            where d.owner.id = :userId
              and w.status in ('ONGOING', 'PAUSED')
            order by w.startedAt desc
            """)
    List<WalkSession> findActiveByOwnerId(@Param("userId") Long userId);

    /** 유저 + 강아지 필터 */
    @Query("""
            select w from WalkSession w
            join fetch w.dog d
            where d.owner.id = :userId
              and d.id = :dogId
              and w.status in ('ONGOING', 'PAUSED')
            order by w.startedAt desc
            """)
    List<WalkSession> findActiveByOwnerIdAndDogId(
            @Param("userId") Long userId,
            @Param("dogId") Long dogId);
}