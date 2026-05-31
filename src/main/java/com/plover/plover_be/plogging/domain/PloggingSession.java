package com.plover.plover_be.plogging.domain;

import com.plover.plover_be.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "plogging_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PloggingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PloggingMode mode;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt;

    @Column(name = "distance_meters", nullable = false)
    private int distanceMeters;

    @Column(name = "step_count", nullable = false)
    private int stepCount;

    @Column(name = "calories_burned", nullable = false)
    private int caloriesBurned;

    @Column(name = "plogging_seconds", nullable = false)
    private int ploggingSeconds;

    @Column(name = "rest_seconds", nullable = false)
    private int restSeconds;

    @Column(name = "place_name")
    private String placeName;

    @Column(name = "start_latitude", nullable = false)
    private double startLatitude;

    @Column(name = "start_longitude", nullable = false)
    private double startLongitude;

    @Column(name = "end_latitude", nullable = false)
    private double endLatitude;

    @Column(name = "end_longitude", nullable = false)
    private double endLongitude;

    @Column(name = "map_image_url")
    private String mapImageUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static PloggingSession create(
            User user, PloggingMode mode,
            LocalDateTime startedAt, LocalDateTime finishedAt,
            int distanceMeters, int stepCount, int caloriesBurned,
            int ploggingSeconds, int restSeconds,
            String placeName,
            double startLatitude, double startLongitude,
            double endLatitude, double endLongitude,
            String mapImageUrl
    ) {
        PloggingSession session = new PloggingSession();
        session.user = user;
        session.mode = mode;
        session.startedAt = startedAt;
        session.finishedAt = finishedAt;
        session.distanceMeters = distanceMeters;
        session.stepCount = stepCount;
        session.caloriesBurned = caloriesBurned;
        session.ploggingSeconds = ploggingSeconds;
        session.restSeconds = restSeconds;
        session.placeName = placeName;
        session.startLatitude = startLatitude;
        session.startLongitude = startLongitude;
        session.endLatitude = endLatitude;
        session.endLongitude = endLongitude;
        session.mapImageUrl = mapImageUrl;
        return session;
    }
}
