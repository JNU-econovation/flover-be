package com.flover.flover_be.plogging.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    public static PloggingPhoto create(
            PloggingSession ploggingSession,
            int sequence,
            String imageUrl
    ) {
        PloggingPhoto photo = new PloggingPhoto();
        photo.ploggingSession = ploggingSession;
        photo.sequence = sequence;
        photo.imageUrl = imageUrl;
        return photo;
    }
}
