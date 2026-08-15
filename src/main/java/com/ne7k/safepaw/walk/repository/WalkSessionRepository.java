package com.ne7k.safepaw.walk.repository;

import com.ne7k.safepaw.walk.domain.WalkSession;
import com.ne7k.safepaw.walk.domain.WalkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WalkSessionRepository extends JpaRepository<WalkSession, Long> {

    Optional<WalkSession> findByIdAndDog_Owner_Id(Long id, Long userId);

    /** 동일 강아지 활성 세션 (ONGOING | PAUSED) — 시작 중복 방지 */
    @Query("""
            select w from WalkSession w
            where w.dog.id = :dogId
              and w.status in ('ONGOING', 'PAUSED')
            """)
    Optional<WalkSession> findActiveByDogId(@Param("dogId") Long dogId);

    /** 유저 소유 강아지의 활성 산책 목록 — List(페이징 없음) 이므로 fetch join 허용 */
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from WalkSession w where w.dog.id = :dogId")
    int deleteAllByDogId(@Param("dogId") Long dogId);

    List<WalkSession> findByDog_IdAndStatusIn(Long dogId, Collection<WalkStatus> statuses);

    /**
     * set10: 종료 산책 기록 (페이징).
     * territoryOnly — null: 전체, true: TERRITORY, false: NORMAL
     * join fetch 사용 금지 — @EntityGraph(dog) 로 N+1 방지.
     */
    @EntityGraph(attributePaths = {"dog"})
    @Query(
            value = """
                    select w from WalkSession w
                    join w.dog d
                    where d.owner.id = :userId
                      and w.status in :statuses
                      and (:territoryOnly is null
                           or (:territoryOnly = true
                               and exists (select 1 from Territory t where t.walkSession.id = w.id))
                           or (:territoryOnly = false
                               and not exists (select 1 from Territory t where t.walkSession.id = w.id)))
                    """,
            countQuery = """
                    select count(w) from WalkSession w
                    join w.dog d
                    where d.owner.id = :userId
                      and w.status in :statuses
                      and (:territoryOnly is null
                           or (:territoryOnly = true
                               and exists (select 1 from Territory t where t.walkSession.id = w.id))
                           or (:territoryOnly = false
                               and not exists (select 1 from Territory t where t.walkSession.id = w.id)))
                    """
    )
    Page<WalkSession> findHistoryByOwnerId(
            @Param("userId") Long userId,
            @Param("statuses") Collection<WalkStatus> statuses,
            @Param("territoryOnly") Boolean territoryOnly,
            Pageable pageable);

    @EntityGraph(attributePaths = {"dog"})
    @Query(
            value = """
                    select w from WalkSession w
                    join w.dog d
                    where d.owner.id = :userId
                      and d.id = :dogId
                      and w.status in :statuses
                      and (:territoryOnly is null
                           or (:territoryOnly = true
                               and exists (select 1 from Territory t where t.walkSession.id = w.id))
                           or (:territoryOnly = false
                               and not exists (select 1 from Territory t where t.walkSession.id = w.id)))
                    """,
            countQuery = """
                    select count(w) from WalkSession w
                    join w.dog d
                    where d.owner.id = :userId
                      and d.id = :dogId
                      and w.status in :statuses
                      and (:territoryOnly is null
                           or (:territoryOnly = true
                               and exists (select 1 from Territory t where t.walkSession.id = w.id))
                           or (:territoryOnly = false
                               and not exists (select 1 from Territory t where t.walkSession.id = w.id)))
                    """
    )
    Page<WalkSession> findHistoryByOwnerIdAndDogId(
            @Param("userId") Long userId,
            @Param("dogId") Long dogId,
            @Param("statuses") Collection<WalkStatus> statuses,
            @Param("territoryOnly") Boolean territoryOnly,
            Pageable pageable);
}
