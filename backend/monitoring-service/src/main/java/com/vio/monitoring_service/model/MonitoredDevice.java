package com.vio.monitoring_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "monitored_devices")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonitoredDevice {
    @Id
    private Long deviceId;

    private double hourlyConsumption;
}