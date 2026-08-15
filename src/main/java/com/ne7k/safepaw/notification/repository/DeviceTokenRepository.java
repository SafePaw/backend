package com.ne7k.safepaw.notification.repository;

import com.ne7k.safepaw.notification.domain.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    List<DeviceToken> findByUser_Id(Long userId);

    Optional<DeviceToken> findByToken(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from DeviceToken d where d.token = :token")
    int deleteByToken(@Param("token") String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from DeviceToken d where d.user.id = :userId")
    int deleteByUser_Id(@Param("userId") Long userId);

    @Query("""
            select distinct dt.user.id from DeviceToken dt
            where exists (select 1 from Dog d where d.owner.id = dt.user.id)
            """)
    List<Long> findDistinctUserIdsWithDogs();
}