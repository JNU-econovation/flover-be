package com.plover.plover_be.crew.domain;

import com.plover.plover_be.crew.exception.CrewErrorCode;
import com.plover.plover_be.crew.exception.CrewException;
import com.plover.plover_be.plogging.domain.PloggingSession;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "crew_plogging_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewPloggingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrewPloggingStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "submission_deadline_at")
    private LocalDateTime submissionDeadlineAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representative_plogging_session_id")
    private PloggingSession representativePloggingSession;

    @Column(name = "representative_user_id_snapshot")
    private Long representativeUserIdSnapshot;

    @Column(name = "representative_nickname_snapshot")
    private String representativeNicknameSnapshot;

    @Column(name = "representative_step_count_snapshot")
    private Integer representativeStepCountSnapshot;

    @Column(name = "representative_distance_meters_snapshot")
    private Integer representativeDistanceMetersSnapshot;

    @Column(name = "representative_plogging_seconds_snapshot")
    private Integer representativePloggingSecondsSnapshot;

    @Column(name = "participant_count_snapshot")
    private Integer participantCountSnapshot;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static CrewPloggingSession create(Crew crew) {
        CrewPloggingSession session = new CrewPloggingSession();
        session.crew = crew;
        session.status = CrewPloggingStatus.RECRUITING;
        return session;
    }

    public void start(LocalDateTime now) {
        if (status != CrewPloggingStatus.RECRUITING) {
            throw new CrewException(CrewErrorCode.SESSION_NOT_RECRUITING);
        }
        this.status = CrewPloggingStatus.IN_PROGRESS;
        this.startedAt = now;
    }

    public void end(LocalDateTime now, LocalDateTime submissionDeadlineAt) {
        if (status != CrewPloggingStatus.IN_PROGRESS) {
            throw new CrewException(CrewErrorCode.SESSION_NOT_IN_PROGRESS);
        }
        this.status = CrewPloggingStatus.COMPLETING;
        this.endedAt = now;
        this.submissionDeadlineAt = submissionDeadlineAt;
    }

    public void cancel(LocalDateTime now) {
        if (status == CrewPloggingStatus.CANCELED) {
            return;
        }
        if (status != CrewPloggingStatus.RECRUITING) {
            throw new CrewException(CrewErrorCode.INVALID_SESSION_STATE);
        }
        this.status = CrewPloggingStatus.CANCELED;
        this.canceledAt = now;
    }

    public void selectRepresentative(PloggingSession record, Long userId, String nickname) {
        this.representativePloggingSession = record;
        this.representativeUserIdSnapshot = userId;
        this.representativeNicknameSnapshot = nickname;
        this.representativeStepCountSnapshot = record.getStepCount();
        this.representativeDistanceMetersSnapshot = record.getDistanceMeters();
        this.representativePloggingSecondsSnapshot = record.getPloggingSeconds();
    }

    public void complete(LocalDateTime completedAt, int participantCount) {
        if (status != CrewPloggingStatus.IN_PROGRESS && status != CrewPloggingStatus.COMPLETING) {
            throw new CrewException(CrewErrorCode.INVALID_SESSION_STATE);
        }
        if (this.endedAt == null) {
            this.endedAt = completedAt;
        }
        this.status = CrewPloggingStatus.COMPLETED;
        this.participantCountSnapshot = participantCount;
    }

    public void clearRepresentativeRecordReference() {
        this.representativePloggingSession = null;
    }

    public boolean hasRepresentativeSnapshot() {
        return representativeUserIdSnapshot != null;
    }
}
