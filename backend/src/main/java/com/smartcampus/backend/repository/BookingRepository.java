package com.smartcampus.backend.repository;

import com.smartcampus.backend.entity.Booking;
import com.smartcampus.backend.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingCode(String bookingCode);

    List<Booking> findByUserId(Long userId);

    List<Booking> findByResourceId(Long resourceId);

    List<Booking> findByStatus(BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT b FROM Booking b WHERE " +
            "(:status IS NULL OR b.status = :status) AND " +
            "(:userId IS NULL OR b.user.id = :userId) AND " +
            "(:resourceId IS NULL OR b.resource.id = :resourceId)")
    List<Booking> findBookingsWithFilters(
            @Param("status") BookingStatus status,
            @Param("userId") Long userId,
            @Param("resourceId") Long resourceId
    );

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
            "WHERE b.resource.id = :resourceId AND b.bookingDate = :bookingDate " +
            "AND b.status IN ('PENDING', 'APPROVED') " +
            "AND b.startTime < :endTime AND b.endTime > :startTime " +
            "AND b.id != :excludeBookingId")
    boolean hasOverlappingBooking(
            @Param("resourceId") Long resourceId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeBookingId") Long excludeBookingId
    );

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
            "WHERE b.resource.id = :resourceId AND b.bookingDate = :bookingDate " +
            "AND b.status IN ('PENDING', 'APPROVED') " +
            "AND b.startTime < :endTime AND b.endTime > :startTime")
    boolean hasOverlappingBooking(
            @Param("resourceId") Long resourceId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}
