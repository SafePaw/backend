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

    /**
     * set11: recorded_at 순 경로 → 닫힌 ring → ST_BuildArea (+ fallback Polygonize).
     * 반환: MULTIPOLYGON WKT (단일 POLYGON도 ST_Multi). 실패 시 null.
     */
    @Query(value = """
        WITH pts AS (
          SELECT ST_MakeLine(geom ORDER BY recorded_at) AS raw_line
          FROM walk_points
          WHERE walk_session_id = :walkId
          HAVING COUNT(*) >= 2
        ),
        cleaned AS (
          SELECT ST_RemoveRepeatedPoints(
                   ST_SimplifyPreserveTopology(raw_line, :simplifyTolerance)
                 ) AS line
          FROM pts
        ),
        closed AS (
          SELECT ST_AddPoint(line, ST_StartPoint(line)) AS ring
          FROM cleaned
          WHERE line IS NOT NULL AND ST_NPoints(line) >= 3
        ),
        built AS (
          SELECT ST_MakeValid(
                   COALESCE(
                     NULLIF(ST_BuildArea(ring), 'GEOMETRYCOLLECTION EMPTY'::geometry),
                     ST_CollectionExtract(ST_Polygonize(ST_Node(ring)), 3)
                   )
                 ) AS geom
          FROM closed
        ),
        parts AS (
          SELECT (ST_Dump(geom)).geom AS part
          FROM built
          WHERE geom IS NOT NULL AND NOT ST_IsEmpty(geom)
        ),
        filtered AS (
          SELECT part FROM parts
          WHERE ST_GeometryType(part) IN ('ST_Polygon', 'ST_MultiPolygon')
            AND ST_Area(part::geography) >= :minPartAreaSqm
        ),
        merged AS (
          SELECT ST_Union(ST_Collect(part)) AS geom FROM filtered
        )
        SELECT ST_AsText(
                 ST_ForcePolygonCCW(
                   ST_Multi(
                     ST_CollectionExtract(
                       ST_MakeValid(
                         ST_SimplifyPreserveTopology(geom, :polygonSimplifyTolerance)
                       ),
                       3
                     )
                   )
                 )
               )
        FROM merged
        WHERE geom IS NOT NULL AND NOT ST_IsEmpty(geom)
        """, nativeQuery = true)
    String buildWalkAreaWkt(@Param("walkId") Long walkId,
                            @Param("simplifyTolerance") double simplifyTolerance,
                            @Param("polygonSimplifyTolerance") double polygonSimplifyTolerance,
                            @Param("minPartAreaSqm") double minPartAreaSqm);

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
        SELECT t.* FROM territories t
        JOIN dogs d ON d.id = t.dog_id
        JOIN crew_members cm ON cm.user_id = d.user_id
        WHERE cm.crew_id = :crewId
          AND t.status = 'ACTIVE'
          AND ST_Intersects(t.geom, ST_MakeEnvelope(:swLng, :swLat, :neLng, :neLat, 4326))
        """, nativeQuery = true)
    List<Territory> findActiveInBboxByCrewId(@Param("crewId") Long crewId,
                                             @Param("swLng") double swLng,
                                             @Param("swLat") double swLat,
                                             @Param("neLng") double neLng,
                                             @Param("neLat") double neLat);

    /** 타 강아지 ACTIVE (최근 점령 우선 Difference 대상). 동일 dog 제외 */
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

    /** set9: 동일 강아지 · 겹치는 다른 ACTIVE (Union 후보) */
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

    /** set9: id 목록 Union WKT (합집합 — 새 점령 면적 포함해 저장) */
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

    // 강아지 삭제 cascade (DogService.delete)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Territory t where t.dog.id = :dogId")
    int deleteAllByDogId(@Param("dogId") Long dogId);

    interface IntrusionRow {
        Long getTerritoryId();
        double getOverlapRatio();
    }
}