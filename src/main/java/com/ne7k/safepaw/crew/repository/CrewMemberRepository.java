package com.ne7k.safepaw.crew.repository;

import com.ne7k.safepaw.crew.domain.CrewMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {

    boolean existsByUser_Id(Long userId);

    Optional<CrewMember> findByUser_Id(Long userId);

    Optional<CrewMember> findByCrew_IdAndUser_Id(Long crewId, Long userId);

    long countByCrew_Id(Long crewId);

    @Query("""
            SELECT m FROM CrewMember m
            JOIN FETCH m.crew c
            JOIN FETCH c.leader
            JOIN FETCH m.user
            WHERE m.user.id = :userId
            """)
    Optional<CrewMember> findWithCrewByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT m FROM CrewMember m
            JOIN FETCH m.crew c
            JOIN FETCH m.user
            WHERE m.user.id IN :userIds
            """)
    List<CrewMember> findAllByUser_IdIn(@Param("userIds") Collection<Long> userIds);

    @Query("""
            SELECT m FROM CrewMember m
            JOIN FETCH m.user
            WHERE m.crew.id = :crewId
            ORDER BY m.joinedAt ASC
            """)
    List<CrewMember> findAllWithUserByCrewId(@Param("crewId") Long crewId);

    @Query("""
            SELECT m.crew.id AS crewId, COUNT(m) AS cnt
            FROM CrewMember m
            WHERE m.crew.id IN :crewIds
            GROUP BY m.crew.id
            """)
    List<CrewCountRow> countGroupedByCrewId(@Param("crewIds") Collection<Long> crewIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CrewMember m WHERE m.crew.id = :crewId AND m.user.id = :userId")
    int deleteByCrewIdAndUserId(@Param("crewId") Long crewId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CrewMember m WHERE m.crew.id = :crewId")
    int deleteAllByCrewId(@Param("crewId") Long crewId);

    interface CrewCountRow {
        Long getCrewId();
        long getCnt();
    }
}
