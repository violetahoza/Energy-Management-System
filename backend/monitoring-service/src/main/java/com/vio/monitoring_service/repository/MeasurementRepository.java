package com.vio.monitoring_service.repository;

import com.vio.monitoring_service.dto.HourlyConsumptionDto;
import com.vio.monitoring_service.model.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    @Query("SELECT new com.vio.monitoring_service.dto.HourlyConsumptionDto(HOUR(m.timestamp), SUM(m.measuredValue)) " +
            "FROM Measurement m " +
            "WHERE m.deviceId = :deviceId " +
            "AND m.timestamp BETWEEN :startDate AND :endDate " +
            "GROUP BY HOUR(m.timestamp) " +
            "ORDER BY HOUR(m.timestamp)")
    List<HourlyConsumptionDto> findHourlyConsumption(@Param("deviceId") Long deviceId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);
}