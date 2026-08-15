package com.ne7k.safepaw.crew.repository;

import com.ne7k.safepaw.crew.domain.Crew;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CrewRepository extends JpaRepository<Crew, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByInviteCode(String inviteCode);

    Optional<Crew> findByInviteCode(String inviteCode);

    @Query("SELECT c FROM Crew c JOIN FETCH c.leader WHERE c.id = :id")
    Optional<Crew> findWithLeaderById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Crew c WHERE c.id = :id")
    Optional<Crew> findByIdForUpdate(@Param("id") Long id);
}
