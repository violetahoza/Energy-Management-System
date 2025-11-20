package com.vio.monitoring_service.repository;

import com.vio.monitoring_service.model.MonitoredDevice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<MonitoredDevice, Long> {
}
