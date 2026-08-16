package com.ne7k.safepaw.crew.repository;

import com.ne7k.safepaw.crew.domain.Crew;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CrewStatsRepository extends JpaRepository<Crew, Long> {

    @Query(value = """
            SELECT
              COALESCE(
                ST_Area(
                  ST_MakeValid(
                    ST_CollectionExtract(
                      ST_UnaryUnion(ST_Collect(t.geom)),
                      3
                    )
                  )::geography
                ),
                0
              )::float8 AS area,
              COUNT(*)::bigint AS cnt,
              ST_AsGeoJSON(
                ST_MakeValid(
                  ST_CollectionExtract(
                    ST_UnaryUnion(ST_Collect(t.geom)),
                    3
                  )
                )
              ) AS geojson
            FROM territories t
            JOIN dogs d ON d.id = t.dog_id
            JOIN crew_members cm ON cm.user_id = d.user_id
            WHERE cm.crew_id = :crewId
              AND t.status = 'ACTIVE'
              AND t.season_key = :season
            """, nativeQuery = true)
    Optional<CrewUnionRow> findCrewUnion(@Param("crewId") Long crewId,
                                         @Param("season") String season);

    @Query(value = """
            SELECT
                cm.user_id AS user_id,
                COALESCE(
                  ST_Area(
                    ST_MakeValid(
                      ST_CollectionExtract(
                        ST_UnaryUnion(ST_Collect(t.geom)),
                        3
                      )
                    )::geography
                  ),
                  0
                )::float8 AS area
            FROM crew_members cm
            LEFT JOIN dogs d ON d.user_id = cm.user_id
            LEFT JOIN territories t
                   ON t.dog_id = d.id
                  AND t.status = 'ACTIVE'
                  AND t.season_key = :season
            WHERE cm.crew_id = :crewId
            GROUP BY cm.user_id
            """, nativeQuery = true)
    List<UserAreaRow> findMemberAreas(@Param("crewId") Long crewId,
                                      @Param("season") String season);

    @Query(value = """
            SELECT crew_id AS crew_id,
                   active_area::float8 AS value,
                   RANK() OVER (ORDER BY active_area DESC) AS rank
            FROM v_season_crew_stats
            WHERE season_key = :season AND active_area > 0
            ORDER BY active_area DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<CrewRankRow> findCrewTerritoryBoard(@Param("season") String season,
                                             @Param("limit") int limit,
                                             @Param("offset") int offset);

    @Query(value = """
            SELECT COUNT(*) FROM v_season_crew_stats
            WHERE season_key = :season AND active_area > 0
            """, nativeQuery = true)
    long countCrewTerritoryParticipants(@Param("season") String season);

    @Query(value = """
            WITH ranked AS (
              SELECT crew_id AS crew_id,
                     active_area::float8 AS value,
                     RANK() OVER (ORDER BY active_area DESC) AS rank
              FROM v_season_crew_stats
              WHERE season_key = :season AND active_area > 0
            )
            SELECT crew_id, value, rank
            FROM ranked
            WHERE crew_id = :crewId
            """, nativeQuery = true)
    Optional<CrewRankRow> findCrewTerritoryRank(@Param("season") String season,
                                                @Param("crewId") Long crewId);

    @Query(value = """
            SELECT COUNT(*) FROM v_season_crew_stats
            WHERE season_key = :season AND active_area > :value
            """, nativeQuery = true)
    long countAboveCrewTerritory(@Param("season") String season,
                                 @Param("value") double value);

    interface CrewUnionRow {
        Double getArea();
        Long getCnt();
        String getGeojson();
    }

    interface UserAreaRow {
        Long getUserId();
        Double getArea();
    }

    interface CrewRankRow {
        Long getCrewId();
        Double getValue();
        Integer getRank();
    }
}
