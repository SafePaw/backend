package com.ne7k.safepaw.score.repository;

import com.ne7k.safepaw.score.domain.SeasonPushDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeasonPushDeliveryRepository extends JpaRepository<SeasonPushDelivery, Long> {

    boolean existsByUserIdAndSeasonKeyAndReminderKind(Long userId, String seasonKey, String reminderKind);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from SeasonPushDelivery d
            where d.userId = :userId and d.seasonKey = :seasonKey and d.reminderKind = :kind
            """)
    int deleteByUserIdAndSeasonKeyAndReminderKind(
            @Param("userId") Long userId,
            @Param("seasonKey") String seasonKey,
            @Param("kind") String kind);
}
