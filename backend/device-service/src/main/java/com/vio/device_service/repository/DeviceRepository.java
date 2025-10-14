package com.vio.device_service.repository;

import com.vio.device_service.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    List<Device> findByUserId(Long userId);
    List<Device> findByLocation(String location);
}
