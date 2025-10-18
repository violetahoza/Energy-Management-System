package com.vio.authorization_service.service;

import com.vio.authorization_service.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final CredentialRepository credentialRepository;

}
