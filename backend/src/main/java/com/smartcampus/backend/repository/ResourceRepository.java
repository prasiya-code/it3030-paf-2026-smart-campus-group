package com.smartcampus.backend.repository;

import com.smartcampus.backend.entity.Resource;
import com.smartcampus.backend.enums.ResourceStatus;
import com.smartcampus.backend.enums.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    Optional<Resource> findByResourceCode(String resourceCode);

    List<Resource> findByType(ResourceType type);

    List<Resource> findByStatus(ResourceStatus status);

    @Query("SELECT r FROM Resource r WHERE " +
            "(:type IS NULL OR r.type = :type) AND " +
            "(:location IS NULL OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
            "(:minCapacity IS NULL OR r.capacity >= :minCapacity) AND " +
            "(:status IS NULL OR r.status = :status)")
    List<Resource> searchResources(
            @Param("type") ResourceType type,
            @Param("location") String location,
            @Param("minCapacity") Integer minCapacity,
            @Param("status") ResourceStatus status
    );
}
