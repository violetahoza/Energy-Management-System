package com.vio.authorization_service.repository;

import com.vio.authorization_service.model.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
}
