package com.vio.device_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deviceId;


    private String name;

    private String description;

    private String location;

    private Double maximumConsumption;

    private Long userId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private boolean isActive;
}
