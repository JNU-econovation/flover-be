package com.plover.plover_be.crew.repository;

import com.plover.plover_be.crew.domain.Crew;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CrewRepository extends JpaRepository<Crew, Long> {

    Optional<Crew> findByJoinCode(String joinCode);

    boolean existsByJoinCode(String joinCode);

    @Query("SELECT c.id FROM Crew c WHERE c.leader.id = :userId")
    List<Long> findIdsByLeaderId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Crew c WHERE c.id = :crewId")
    void deleteByCrewId(@Param("crewId") Long crewId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Crew c WHERE c.id = :crewId")
    Optional<Crew> findByIdForUpdate(@Param("crewId") Long crewId);
}
