package com.vio.monitoring_service.model;

import jakarta.persistence.*;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Id
    @Column(nullable = false)
    private Long deviceId;
}