package com.plover.plover_be.crew.repository;

import com.plover.plover_be.crew.domain.CrewMember;
import com.plover.plover_be.crew.domain.CrewMemberStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM CrewMember m WHERE m.crew.id = :crewId AND m.user.id = :userId")
    Optional<CrewMember> findByCrewIdAndUserIdForUpdate(
            @Param("crewId") Long crewId,
            @Param("userId") Long userId
    );

    Optional<CrewMember> findByCrewIdAndUserIdAndStatus(Long crewId, Long userId, CrewMemberStatus status);

    @EntityGraph(attributePaths = {"crew", "crew.leader"})
    List<CrewMember> findAllByUserIdAndStatusOrderByJoinedAtDesc(Long userId, CrewMemberStatus status);

    @EntityGraph(attributePaths = "user")
    List<CrewMember> findAllByCrewIdAndStatusOrderByJoinedAtAsc(Long crewId, CrewMemberStatus status);

    long countByCrewIdAndStatus(Long crewId, CrewMemberStatus status);

    @Query("""
            SELECT m.crew.id AS crewId, m.user.profileImageUrl AS profileImageUrl
            FROM CrewMember m
            WHERE m.crew.id IN :crewIds
              AND m.status = :status
            ORDER BY m.crew.id, m.joinedAt
            """)
    List<CrewProfileImageView> findProfileImagesByCrewIdInAndStatus(
            @Param("crewIds") List<Long> crewIds,
            @Param("status") CrewMemberStatus status
    );

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CrewMember m WHERE m.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CrewMember m WHERE m.crew.id = :crewId")
    void deleteByCrewId(@Param("crewId") Long crewId);

    interface CrewProfileImageView {
        Long getCrewId();
        String getProfileImageUrl();
    }
}
