package com.vio.monitoring_service.repository;

import com.vio.monitoring_service.model.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeasurementRepository extends JpaRepository<Measurement, Long> {
}
