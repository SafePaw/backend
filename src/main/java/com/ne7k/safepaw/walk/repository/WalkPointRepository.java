package com.ne7k.safepaw.walk.repository;

import com.ne7k.safepaw.walk.domain.WalkPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WalkPointRepository extends JpaRepository<WalkPoint, Long> {

    // 산책 경로 폴리곤 조회
    @Query(value = """
        SELECT ST_X(wp.geom) AS lng, ST_Y(wp.geom) AS lat
        FROM walk_points wp
        WHERE wp.walk_session_id = :walkId
        ORDER BY wp.recorded_at ASC
        """, nativeQuery = true)
    List<Object[]> findLngLatByWalkSessionId(@Param("walkId") Long walkId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    delete from WalkPoint p
    where p.walkSession.id in (
        select w.id from WalkSession w where w.dog.id = :dogId)
    """)
    void deleteAllByDogId(@Param("dogId") Long dogId);

}
