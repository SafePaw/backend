package com.ne7k.safepaw.ranking.repository;

import com.ne7k.safepaw.dog.domain.Dog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeasonStatsRepository extends JpaRepository<Dog, Long> {

    // ── board ──────────────────────────────────────────────

    @Query(value = """
            SELECT dog_id AS dog_id,
                   total_xp::float8 AS value,
                   RANK() OVER (ORDER BY total_xp DESC) AS rank
            FROM v_season_xp_stats
            WHERE season_key = :season AND total_xp > 0
            ORDER BY total_xp DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<DogRankRow> findXpBoard(@Param("season") String season,
                                 @Param("limit") int limit,
                                 @Param("offset") int offset);

    @Query(value = """
            SELECT dog_id AS dog_id,
                   active_area::float8 AS value,
                   RANK() OVER (ORDER BY active_area DESC) AS rank
            FROM v_season_dog_stats
            WHERE season_key = :season AND active_area > 0
            ORDER BY active_area DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<DogRankRow> findTerritoryBoard(@Param("season") String season,
                                        @Param("limit") int limit,
                                        @Param("offset") int offset);

    @Query(value = """
            SELECT dog_id AS dog_id,
                   total_distance::float8 AS value,
                   RANK() OVER (ORDER BY total_distance DESC) AS rank
            FROM v_season_walk_stats
            WHERE season_key = :season AND total_distance > 0
            ORDER BY total_distance DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<DogRankRow> findDistanceBoard(@Param("season") String season,
                                       @Param("limit") int limit,
                                       @Param("offset") int offset);

    @Query(value = """
            SELECT dog_id AS dog_id,
                   total_duration::float8 AS value,
                   RANK() OVER (ORDER BY total_duration DESC) AS rank
            FROM v_season_walk_stats
            WHERE season_key = :season AND total_duration > 0
            ORDER BY total_duration DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<DogRankRow> findDurationBoard(@Param("season") String season,
                                       @Param("limit") int limit,
                                       @Param("offset") int offset);

    // ── count (totalElements / percentile 분모) ────────────

    @Query(value = """
            SELECT COUNT(*) FROM v_season_xp_stats
            WHERE season_key = :season AND total_xp > 0
            """, nativeQuery = true)
    long countXpParticipants(@Param("season") String season);

    @Query(value = """
            SELECT COUNT(*) FROM v_season_dog_stats
            WHERE season_key = :season AND active_area > 0
            """, nativeQuery = true)
    long countTerritoryParticipants(@Param("season") String season);

    @Query(value = """
            SELECT COUNT(*) FROM v_season_walk_stats
            WHERE season_key = :season AND total_distance > 0
            """, nativeQuery = true)
    long countDistanceParticipants(@Param("season") String season);

    @Query(value = """
            SELECT COUNT(*) FROM v_season_walk_stats
            WHERE season_key = :season AND total_duration > 0
            """, nativeQuery = true)
    long countDurationParticipants(@Param("season") String season);

    // ── /me 시즌 전체 순위표 (dogId 로 필터) ───────────────

    @Query(value = """
            SELECT dog_id AS dog_id,
                   total_xp::float8 AS value,
                   RANK() OVER (ORDER BY total_xp DESC) AS rank
            FROM v_season_xp_stats
            WHERE season_key = :season AND total_xp > 0
            """, nativeQuery = true)
    List<DogRankRow> findAllXpRanks(@Param("season") String season);

    @Query(value = """
            SELECT dog_id AS dog_id,
                   active_area::float8 AS value,
                   RANK() OVER (ORDER BY active_area DESC) AS rank
            FROM v_season_dog_stats
            WHERE season_key = :season AND active_area > 0
            """, nativeQuery = true)
    List<DogRankRow> findAllTerritoryRanks(@Param("season") String season);

    @Query(value = """
            SELECT dog_id AS dog_id,
                   total_distance::float8 AS value,
                   RANK() OVER (ORDER BY total_distance DESC) AS rank
            FROM v_season_walk_stats
            WHERE season_key = :season AND total_distance > 0
            """, nativeQuery = true)
    List<DogRankRow> findAllDistanceRanks(@Param("season") String season);

    @Query(value = """
            SELECT dog_id AS dog_id,
                   total_duration::float8 AS value,
                   RANK() OVER (ORDER BY total_duration DESC) AS rank
            FROM v_season_walk_stats
            WHERE season_key = :season AND total_duration > 0
            """, nativeQuery = true)
    List<DogRankRow> findAllDurationRanks(@Param("season") String season);

    // ── percentile: 나보다 value 큰 참가자 수 ───────────────

    @Query(value = """
            SELECT COUNT(*) FROM v_season_xp_stats
            WHERE season_key = :season AND total_xp > :value
            """, nativeQuery = true)
    long countAboveXp(@Param("season") String season, @Param("value") double value);

    @Query(value = """
            SELECT COUNT(*) FROM v_season_dog_stats
            WHERE season_key = :season AND active_area > :value
            """, nativeQuery = true)
    long countAboveTerritory(@Param("season") String season, @Param("value") double value);

    @Query(value = """
            SELECT COUNT(*) FROM v_season_walk_stats
            WHERE season_key = :season AND total_distance > :value
            """, nativeQuery = true)
    long countAboveDistance(@Param("season") String season, @Param("value") double value);

    @Query(value = """
            SELECT COUNT(*) FROM v_season_walk_stats
            WHERE season_key = :season AND total_duration > :value
            """, nativeQuery = true)

    long countAboveDuration(@Param("season") String season, @Param("value") double value);

    interface DogRankRow {
        Long getDogId();
        Double getValue();
        Integer getRank();
    }
}
