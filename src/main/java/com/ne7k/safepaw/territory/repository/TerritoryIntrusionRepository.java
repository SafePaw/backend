package com.ne7k.safepaw.territory.repository;

import com.ne7k.safepaw.territory.domain.TerritoryIntrusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TerritoryIntrusionRepository extends JpaRepository<TerritoryIntrusion, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    delete from TerritoryIntrusion i
    where i.victimTerritory.id in (select t.id from Territory t where t.dog.id = :dogId)
       or i.intruderTerritory.id in (select t.id from Territory t where t.dog.id = :dogId)
    """)
    void deleteAllByDogId(@Param("dogId") Long dogId);

}
