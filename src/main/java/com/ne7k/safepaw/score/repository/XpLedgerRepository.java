package com.ne7k.safepaw.score.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface XpLedgerRepository extends org.springframework.data.jpa.repository.JpaRepository
        <com.ne7k.safepaw.score.domain.XpLedger, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update XpLedger x
       set x.refWalkSession = null
     where x.refWalkSession.dog.id = :dogId
    """)
    int clearWalkSessionRefsByDogId(@Param("dogId") Long dogId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update XpLedger x
       set x.refTerritory = null
     where x.refTerritory.dog.id = :dogId
    """)
    int clearTerritoryRefsByDogId(@Param("dogId") Long dogId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from XpLedger x where x.dog.id = :dogId")
    int deleteByDogId(@Param("dogId") Long dogId);

}