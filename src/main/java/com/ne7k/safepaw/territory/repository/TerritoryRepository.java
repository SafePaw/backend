package com.ne7k.safepaw.territory.repository;

import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.territory.domain.TerritoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TerritoryRepository extends JpaRepository<Territory, Long> {

    @Query(value = """
        SELECT ST_AsText(
                 ST_MakeValid(
                   ST_SimplifyPreserveTopology(
                     ST_ConcaveHull(ST_Collect(wp.geom), :targetPercent, false),
                     :simplifyTolerance
                   )
                 )
               )
        FROM walk_points wp
        WHERE wp.walk_session_id = :walkId
        """, nativeQuery = true)
    String buildConcaveHullWkt(@Param("walkId") Long walkId,
                               @Param("targetPercent") double targetPercent,
                               @Param("simplifyTolerance") double simplifyTolerance);

    @Query(value = """
        SELECT ST_DistanceSphere(
                 (SELECT geom FROM walk_points WHERE walk_session_id = :walkId ORDER BY recorded_at ASC  LIMIT 1),
                 (SELECT geom FROM walk_points WHERE walk_session_id = :walkId ORDER BY recorded_at DESC LIMIT 1)
               )
        """, nativeQuery = true)
    Double loopGapMeters(@Param("walkId") Long walkId);

    @Query(value = "SELECT ST_Area(ST_GeomFromText(:wkt, 4326)::geography)", nativeQuery = true)
    double areaSquareMeters(@Param("wkt") String wkt);

    @Query(value = """
        SELECT ST_IsEmpty(
                 ST_Buffer(ST_GeomFromText(:wkt, 4326)::geography, -:halfWidthMeters)::geometry
               )
        """, nativeQuery = true)
    boolean isNarrowerThan(@Param("wkt") String wkt,
                           @Param("halfWidthMeters") double halfWidthMeters);

    @Query(value = """
        SELECT * FROM territories t
        WHERE t.status = 'ACTIVE'
          AND ST_Intersects(t.geom, ST_MakeEnvelope(:swLng, :swLat, :neLng, :neLat, 4326))
        """, nativeQuery = true)
    List<Territory> findActiveInBbox(@Param("swLng") double swLng, @Param("swLat") double swLat,
                                     @Param("neLng") double neLng, @Param("neLat") double neLat);

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

    @Query(value = """
        SELECT ST_AsText(ST_Intersection(t.geom, ST_GeomFromText(:intruderWkt, 4326)))
        FROM territories t WHERE t.id = :victimId
        """, nativeQuery = true)
    String intersectionWkt(@Param("victimId") Long victimId, @Param("intruderWkt") String intruderWkt);

    @Query(value = """
        SELECT ST_AsText(ST_Difference(t.geom, ST_GeomFromText(:intruderWkt, 4326)))
        FROM territories t WHERE t.id = :victimId
        """, nativeQuery = true)
    String differenceWkt(@Param("victimId") Long victimId, @Param("intruderWkt") String intruderWkt);

    @Query(value = """
        SELECT count(*) FROM territories t
        WHERE t.dog_id = :dogId
          AND t.claimed_at > now() - (:hours || ' hours')::interval
          AND ST_Area(ST_Intersection(t.geom, ST_GeomFromText(:wkt, 4326))::geography)
              / NULLIF(ST_Area(t.geom::geography), 0) > 0.9
        """, nativeQuery = true)
    long countRecentDuplicates(@Param("dogId") Long dogId, @Param("wkt") String wkt,
                               @Param("hours") int hours);

    @Query(value = """
        SELECT t.id FROM territories t
        WHERE t.status = 'ACTIVE'
          AND t.dog_id = :dogId
          AND t.id <> :excludeId
          AND ST_Intersects(t.geom, ST_GeomFromText(:wkt, 4326))
        """, nativeQuery = true)
    List<Long> findSameDogOverlapIds(@Param("dogId") Long dogId,
                                     @Param("excludeId") Long excludeId,
                                     @Param("wkt") String wkt);

    @Query(value = """
        SELECT ST_AsText(
                 ST_MakeValid(
                   ST_CollectionExtract(
                     ST_UnaryUnion(ST_Collect(t.geom)),
                     3
                   )
                 )
               )
        FROM territories t
        WHERE t.id IN (:ids)
        """, nativeQuery = true)
    String unionPolygonsWkt(@Param("ids") List<Long> ids);

    Optional<Territory> findById(Long id);

    Page<Territory> findByDog_Owner_IdAndStatus(Long userId, TerritoryStatus status, Pageable pageable);

    Page<Territory> findByDog_Owner_IdAndDog_IdAndStatus(Long userId, Long dogId, TerritoryStatus status, Pageable pageable);

    Optional<Territory> findByWalkSession_Id(Long walkSessionId);

    /** set10: 산책 기록 목록 — walkSession.id 키 매핑용 (페이지당 소량, fetch join OK) */
    @Query("""
            select t from Territory t
            join fetch t.walkSession ws
            where ws.id in :walkSessionIds
            """)
    List<Territory> findByWalkSession_IdIn(@Param("walkSessionIds") Collection<Long> walkSessionIds);

    boolean existsByDog_IdAndStatus(Long dogId, TerritoryStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Territory t where t.dog.id = :dogId")
    int deleteAllByDogId(@Param("dogId") Long dogId);

    interface IntrusionRow {
        Long getTerritoryId();
        double getOverlapRatio();
    }
}