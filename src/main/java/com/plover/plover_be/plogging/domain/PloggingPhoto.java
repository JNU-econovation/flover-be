package com.plover.plover_be.plogging.domain;

import com.plover.plover_be.crew.domain.CrewPloggingSession;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "plogging_photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PloggingPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plogging_session_id", nullable = false)
    private PloggingSession ploggingSession;

    @Column(nullable = false)
    private int sequence;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_plogging_session_id")
    private CrewPloggingSession crewPloggingSession;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static PloggingPhoto create(
            PloggingSession ploggingSession,
            int sequence,
            String imageUrl
    ) {
        return create(ploggingSession, null, sequence, imageUrl);
    }

    public static PloggingPhoto create(
            PloggingSession ploggingSession,
            CrewPloggingSession crewPloggingSession,
            int sequence,
            String imageUrl
    ) {
        PloggingPhoto photo = new PloggingPhoto();
        photo.ploggingSession = ploggingSession;
        photo.crewPloggingSession = crewPloggingSession;
        photo.sequence = sequence;
        photo.imageUrl = imageUrl;
        return photo;
    }
}
