package com.ne7k.safepaw.walk.repository;

import com.ne7k.safepaw.walk.domain.WalkPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WalkPointRepository extends JpaRepository<WalkPoint, Long> {

    @Query(value = """
        SELECT ST_X(wp.geom) AS lng, ST_Y(wp.geom) AS lat
        FROM walk_points wp
        WHERE wp.walk_session_id = :walkId
        ORDER BY wp.recorded_at ASC
        """, nativeQuery = true)
    List<Object[]> findLngLatByWalkSessionId(@Param("walkId") Long walkId);

    /** Redis 버퍼 만료 시 DB 폴백용 — 시간 순 정렬 */
    List<WalkPoint> findByWalkSession_IdOrderByRecordedAtAsc(Long walkSessionId);

    long countByWalkSession_Id(Long walkSessionId);

    /**
     * DB에 저장된 walk_points로 총 이동 거리(m) 계산.
     * Redis 버퍼 만료 시 WalkSessionService.finish() 에서 사용.
     */
    @Query(value = """
        SELECT COALESCE(SUM(ST_DistanceSphere(lag_geom, geom)), 0)
        FROM (
            SELECT geom, LAG(geom) OVER (ORDER BY recorded_at) AS lag_geom
            FROM walk_points
            WHERE walk_session_id = :walkId
        ) sub
        WHERE lag_geom IS NOT NULL
        """, nativeQuery = true)
    double computeTotalDistanceMeters(@Param("walkId") Long walkId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    delete from WalkPoint p
    where p.walkSession.id in (
        select w.id from WalkSession w where w.dog.id = :dogId)
    """)
    void deleteAllByDogId(@Param("dogId") Long dogId);

}
