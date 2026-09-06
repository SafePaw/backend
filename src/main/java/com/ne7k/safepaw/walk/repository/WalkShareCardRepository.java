package com.ne7k.safepaw.walk.repository;

import com.ne7k.safepaw.walk.domain.WalkShareCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalkShareCardRepository extends JpaRepository<WalkShareCard, Long> {

    Optional<WalkShareCard> findByWalkSession_Id(Long walkSessionId);

    boolean existsByWalkSession_Id(Long walkSessionId);
}
