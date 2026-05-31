package com.plover.plover_be.facility.repository;

import com.plover.plover_be.facility.domain.TrashBin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrashBinRepository extends JpaRepository<TrashBin, Long> {

    @Query(value = """
            SELECT id, name, road_address AS roadAddress, latitude, longitude, trash_type AS trashType,
                   ST_Distance_Sphere(POINT(longitude, latitude), POINT(:lng, :lat)) AS distanceMeters
            FROM trash_bins
            WHERE ST_Distance_Sphere(POINT(longitude, latitude), POINT(:lng, :lat)) <= :radius
            ORDER BY distanceMeters ASC
            """, nativeQuery = true)
    List<TrashBinView> findNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radius") int radius);

    interface TrashBinView {
        Long getId();
        String getName();
        String getRoadAddress();
        double getLatitude();
        double getLongitude();
        String getTrashType();
        double getDistanceMeters();
    }
}
