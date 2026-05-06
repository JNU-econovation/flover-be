package com.flover.flover_be.facility.repository;

import com.flover.flover_be.facility.domain.Toilet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ToiletRepository extends JpaRepository<Toilet, Long> {

    @Query(value = """
            SELECT id, name, road_address AS roadAddress, latitude, longitude,
                   toilet_type AS toiletType, open_time_type AS openTimeType,
                   ST_Distance_Sphere(POINT(longitude, latitude), POINT(:lng, :lat)) AS distanceMeters
            FROM toilets
            WHERE ST_Distance_Sphere(POINT(longitude, latitude), POINT(:lng, :lat)) <= :radius
            ORDER BY distanceMeters ASC
            """, nativeQuery = true)
    List<ToiletView> findNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radius") int radius);

    interface ToiletView {
        Long getId();
        String getName();
        String getRoadAddress();
        double getLatitude();
        double getLongitude();
        String getToiletType();
        String getOpenTimeType();
        double getDistanceMeters();
    }
}
