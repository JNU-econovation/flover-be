package com.plover.plover_be.crew.domain;

import com.plover.plover_be.crew.exception.CrewErrorCode;
import com.plover.plover_be.crew.exception.CrewException;
import com.plover.plover_be.plogging.domain.PloggingSession;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "crew_plogging_participants", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"crew_plogging_session_id", "user_id"}),
        @UniqueConstraint(columnNames = {"plogging_session_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewPloggingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_plogging_session_id", nullable = false)
    private CrewPloggingSession crewPloggingSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_leader", nullable = false)
    private boolean leader;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrewPloggingParticipantStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plogging_session_id")
    private PloggingSession ploggingSession;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    public static CrewPloggingParticipant create(CrewPloggingSession session, User user, boolean leader) {
        CrewPloggingParticipant participant = new CrewPloggingParticipant();
        participant.crewPloggingSession = session;
        participant.user = user;
        participant.leader = leader;
        participant.status = CrewPloggingParticipantStatus.JOINED;
        return participant;
    }

    public void rejoin() {
        if (status != CrewPloggingParticipantStatus.CANCELED) {
            throw new CrewException(CrewErrorCode.ALREADY_SESSION_PARTICIPANT);
        }
        this.status = CrewPloggingParticipantStatus.JOINED;
    }

    public void cancel() {
        if (leader) {
            throw new CrewException(CrewErrorCode.LEADER_CANNOT_CANCEL_PARTICIPATION);
        }
        if (status != CrewPloggingParticipantStatus.JOINED) {
            throw new CrewException(CrewErrorCode.SESSION_NOT_RECRUITING);
        }
        this.status = CrewPloggingParticipantStatus.CANCELED;
    }

    public void cancelBySession() {
        if (status == CrewPloggingParticipantStatus.CANCELED) {
            return;
        }
        if (status != CrewPloggingParticipantStatus.JOINED) {
            throw new CrewException(CrewErrorCode.INVALID_SESSION_STATE);
        }
        this.status = CrewPloggingParticipantStatus.CANCELED;
    }

    public void start() {
        if (status == CrewPloggingParticipantStatus.JOINED) {
            this.status = CrewPloggingParticipantStatus.PARTICIPATING;
        }
    }

    public void submit(PloggingSession record, LocalDateTime submittedAt) {
        if (status == CrewPloggingParticipantStatus.SUBMITTED || ploggingSession != null) {
            throw new CrewException(CrewErrorCode.PLOGGING_ALREADY_SUBMITTED);
        }
        if (status != CrewPloggingParticipantStatus.PARTICIPATING) {
            throw new CrewException(CrewErrorCode.SESSION_PARTICIPANT_ONLY);
        }
        this.ploggingSession = record;
        this.submittedAt = submittedAt;
        this.status = CrewPloggingParticipantStatus.SUBMITTED;
    }

    public void markNotSubmitted() {
        if (status == CrewPloggingParticipantStatus.PARTICIPATING) {
            this.status = CrewPloggingParticipantStatus.NOT_SUBMITTED;
        }
    }
}
