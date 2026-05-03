package com.flover.flover_be.plogging.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plogging_route_points")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PloggingRoutePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plogging_session_id", nullable = false)
    private PloggingSession ploggingSession;

    @Column(nullable = false)
    private int sequence;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    public static PloggingRoutePoint create(
            PloggingSession ploggingSession,
            int sequence,
            double latitude,
            double longitude
    ) {
        PloggingRoutePoint point = new PloggingRoutePoint();
        point.ploggingSession = ploggingSession;
        point.sequence = sequence;
        point.latitude = latitude;
        point.longitude = longitude;
        return point;
    }
}
