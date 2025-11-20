package com.vio.monitoring_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "measurements")
public class Measurement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long measurementId;

    @Column(nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private int hour;

    @Column(nullable = false)
    private Double hourlyConsumption;
}
