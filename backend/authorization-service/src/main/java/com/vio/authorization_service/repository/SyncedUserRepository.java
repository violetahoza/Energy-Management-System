package com.vio.authorization_service.repository;

import com.vio.authorization_service.model.SyncedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SyncedUserRepository extends JpaRepository<SyncedUser, Long> {
    Optional<SyncedUser> findByUsername(String username);
    boolean existsByUsername(String username);
    java.util.List<SyncedUser> findByRole(String role);
}