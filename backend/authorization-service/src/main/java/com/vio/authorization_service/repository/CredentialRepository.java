package com.vio.authorization_service.repository;

import com.vio.authorization_service.model.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
    Optional<Credential> findByUsername(String username);
    Optional<Credential> findByUserId(Long userId);
    boolean existsByUsername(String username);
    boolean existsByUserId(Long userId);
}
