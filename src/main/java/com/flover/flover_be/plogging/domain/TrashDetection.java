package com.flover.flover_be.plogging.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trash_detections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrashDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double latitude;
    private Double longitude;
    private String trashType;
    private Integer count;
    private Double confidence;

    public static TrashDetection create(Double latitude, Double longitude, String trashType, Integer count, Double confidence) {
        TrashDetection detection = new TrashDetection();
        detection.latitude = latitude;
        detection.longitude = longitude;
        detection.trashType = trashType;
        detection.count = count;
        detection.confidence = confidence;
        return detection;
    }
}
