package com.ne7k.safepaw.dog.repository;

import com.ne7k.safepaw.dog.domain.Dog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DogRepository extends JpaRepository<Dog, Long> {

    // 해당 유저의 강아지 조회
    List<Dog> findAllByOwner_IdOrderByIdAsc(Long ownerId);

    // 해당 유저의 강아지 마릿수
    long countByOwner_Id(Long ownerId);

    // 강아지 1마리라도 존재하면 true
    boolean existsByOwner_Id(Long ownerId);

    // dog id + owner id 일치하는 행만 조회
    Optional<Dog> findByIdAndOwner_Id(Long id, Long ownerId);

}
