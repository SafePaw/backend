package com.ne7k.safepaw.walk.repository;

import com.ne7k.safepaw.walk.repository.redis.RedisWalkPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * walk_points 중복 없는 일괄 삽입.
 * ON CONFLICT DO NOTHING: GPS 배치 업로드(append) + finish 양쪽에서 호출해도 안전.
 */
@Repository
@RequiredArgsConstructor
public class WalkPointBatchInsert {

    private final JdbcTemplate jdbcTemplate;

    private static final String SQL = """
            INSERT INTO walk_points
              (walk_session_id, geom, accuracy_meters, speed_kmh, recorded_at)
            VALUES
              (?, ST_SetSRID(ST_MakePoint(?, ?), 4326), ?, ?, ?)
            ON CONFLICT (walk_session_id, recorded_at) DO NOTHING
            """;

    public void batchInsert(Long walkSessionId, List<RedisWalkPoint> points) {
        if (points == null || points.isEmpty()) return;
        jdbcTemplate.batchUpdate(SQL, points, points.size(), (ps, p) -> {
            ps.setLong(1, walkSessionId);
            ps.setDouble(2, p.lng());
            ps.setDouble(3, p.lat());
            ps.setFloat(4, p.accuracyMeters());
            if (p.speedKmh() != null) {
                ps.setFloat(5, p.speedKmh());
            } else {
                ps.setNull(5, java.sql.Types.FLOAT);
            }
            ps.setObject(6, p.recordedAt());
        });
    }
}
