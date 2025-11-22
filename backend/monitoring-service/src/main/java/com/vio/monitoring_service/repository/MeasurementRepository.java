package com.vio.monitoring_service.repository;

import com.vio.monitoring_service.model.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MeasurementRepository extends JpaRepository<Measurement, Long> {
    Optional<Measurement> findByDeviceIdAndDateAndHour(Long deviceId, LocalDate date, Integer hour);
    List<Measurement> findByDeviceIdAndDateOrderByHourAsc(Long deviceId, LocalDate date);

    @Query("SELECT m FROM Measurement m WHERE m.deviceId = :deviceId AND m.date BETWEEN :startDate AND :endDate ORDER BY m.date, m.hour")
    List<Measurement> findByDeviceIdAndDateRange(@Param("deviceId") Long deviceId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
