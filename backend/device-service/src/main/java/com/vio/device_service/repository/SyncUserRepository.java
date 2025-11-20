package com.vio.device_service.repository;

import com.vio.device_service.model.SyncUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyncUserRepository extends JpaRepository<SyncUser, Long> {
    Optional<SyncUser> findByUsername(String username);
    boolean existsByUsername(String username);
}