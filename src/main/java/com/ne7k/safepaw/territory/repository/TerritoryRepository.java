package com.ne7k.safepaw.territory.repository;

import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.territory.domain.TerritoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TerritoryRepository extends JpaRepository<Territory, Long> {

    // 산책 포인트들 폴리곤 만들어서 반환
    @Query(value = """
        SELECT ST_AsText(
                 ST_ConcaveHull(ST_Collect(wp.geom), :targetPercent, false)
               )
        FROM walk_points wp
        WHERE wp.walk_session_id = :walkId
        """, nativeQuery = true)
    String buildConcaveHullWkt(@Param("walkId") Long walkId,
                               @Param("targetPercent") double targetPercent);

    // 시작점 종료지점 거리 - 루프 닫힘 판정
    @Query(value = """
        SELECT ST_DistanceSphere(
                 (SELECT geom FROM walk_points WHERE walk_session_id = :walkId ORDER BY recorded_at ASC  LIMIT 1),
                 (SELECT geom FROM walk_points WHERE walk_session_id = :walkId ORDER BY recorded_at DESC LIMIT 1)
               )
        """, nativeQuery = true)
    Double loopGapMeters(@Param("walkId") Long walkId);

    // 폴리곤 면적
    @Query(value = "SELECT ST_Area(ST_GeomFromText(:wkt, 4326)::geography)", nativeQuery = true)
    double areaSquareMeters(@Param("wkt") String wkt);

    // 최소 폭 검증
    @Query(value = """
        SELECT ST_IsEmpty(
                 ST_Buffer(ST_GeomFromText(:wkt, 4326)::geography, -:halfWidthMeters)::geometry
               )
        """, nativeQuery = true)
    boolean isNarrowerThan(@Param("wkt") String wkt,
                           @Param("halfWidthMeters") double halfWidthMeters);

    // box 내부의 active 영토
    @Query(value = """
        SELECT * FROM territories t
        WHERE t.status = 'ACTIVE'
          AND ST_Intersects(t.geom, ST_MakeEnvelope(:swLng, :swLat, :neLng, :neLat, 4326))
        """, nativeQuery = true)
    List<Territory> findActiveInBbox(@Param("swLng") double swLng, @Param("swLat") double swLat,
                                     @Param("neLng") double neLng, @Param("neLat") double neLat);

    // 새 폴리곤과 겹치는 타 강아지의 활성화 영토 + 겹침 비율
    @Query(value = """
        SELECT t.id AS territory_id,
               ST_Area(ST_Intersection(t.geom, ST_GeomFromText(:wkt, 4326))::geography)
                 / NULLIF(ST_Area(t.geom::geography), 0) AS overlap_ratio
        FROM territories t
        WHERE t.status = 'ACTIVE'
          AND t.dog_id <> :myDogId
          AND ST_Intersects(t.geom, ST_GeomFromText(:wkt, 4326))
          AND ST_Area(ST_Intersection(t.geom, ST_GeomFromText(:wkt, 4326))::geography) > 0
        """, nativeQuery = true)
    List<IntrusionRow> findIntrusionCandidates(@Param("myDogId") Long myDogId,
                                               @Param("wkt") String wkt);

    // 겹치는 조각
    @Query(value = """
        SELECT ST_AsText(ST_Intersection(t.geom, ST_GeomFromText(:intruderWkt, 4326)))
        FROM territories t WHERE t.id = :victimId
        """, nativeQuery = true)
    String intersectionWkt(@Param("victimId") Long victimId, @Param("intruderWkt") String intruderWkt);

    // 잔여 영토
    @Query(value = """
        SELECT ST_AsText(ST_Difference(t.geom, ST_GeomFromText(:intruderWkt, 4326)))
        FROM territories t WHERE t.id = :victimId
        """, nativeQuery = true)
    String differenceWkt(@Param("victimId") Long victimId, @Param("intruderWkt") String intruderWkt);

    // 24시간 내 동일 강아지의 거의 같은 영역
    @Query(value = """
        SELECT count(*) FROM territories t
        WHERE t.dog_id = :dogId
          AND t.claimed_at > now() - (:hours || ' hours')::interval
          AND ST_Area(ST_Intersection(t.geom, ST_GeomFromText(:wkt, 4326))::geography)
              / NULLIF(ST_Area(t.geom::geography), 0) > 0.9
        """, nativeQuery = true)
    long countRecentDuplicates(@Param("dogId") Long dogId, @Param("wkt") String wkt,
                               @Param("hours") int hours);

    // 영토 상세 조회
    Optional<Territory> findById(Long id);   // JpaRepository 기본 제공

    // 사용자의 강아지 영토 페이지
    Page<Territory> findByDog_Owner_IdAndStatus(Long userId, TerritoryStatus status, Pageable pageable);

    // dog id 필터
    Page<Territory> findByDog_Owner_IdAndDog_IdAndStatus(Long userId, Long dogId, TerritoryStatus status, Pageable pageable);

    Optional<Territory> findByWalkSession_Id(Long walkSessionId);

    // 해당 강아지의 첫 영토 여부
    boolean existsByDog_IdAndStatus(Long dogId, TerritoryStatus status);

    // native projection
    interface IntrusionRow {
        Long getTerritoryId();
        double getOverlapRatio();
    }

}
