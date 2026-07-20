package com.plover.plover_be.crew.domain;

import com.plover.plover_be.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "crew_members", uniqueConstraints = @UniqueConstraint(columnNames = {"crew_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrewRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrewMemberStatus status;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    public static CrewMember create(Crew crew, User user, CrewRole role) {
        CrewMember member = new CrewMember();
        member.crew = crew;
        member.user = user;
        member.role = role;
        member.status = CrewMemberStatus.ACTIVE;
        return member;
    }

    public void reactivate() {
        this.status = CrewMemberStatus.ACTIVE;
        this.leftAt = null;
    }

    public void withdraw(LocalDateTime now) {
        this.status = CrewMemberStatus.WITHDRAWN;
        this.leftAt = now;
    }
}
