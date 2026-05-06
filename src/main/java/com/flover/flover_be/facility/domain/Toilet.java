package com.flover.flover_be.facility.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "toilets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Toilet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "road_address")
    private String roadAddress;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "toilet_type")
    private String toiletType;

    @Column(name = "open_time_type")
    private String openTimeType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
