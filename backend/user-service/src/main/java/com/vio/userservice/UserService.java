package com.vio.userservice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;

    public List<UserDTOResponse> getAllUsers() {
        return repository
                .findAll()
                .stream()
                .map(user -> new UserDTOResponse(user.getUserId(), user.getName()))
                .toList();
    }

    public UserDTOResponse findById(Long userId) {
        UserEntity user = repository
                .findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return new UserDTOResponse(user.getUserId(), user.getName());
    }

    public UserDTOResponse createUser(UserDTORequest request) {
        UserEntity user = UserEntity.builder()
                .name(request.name())
                .build();
        UserEntity savedUser = repository.save(user);
        return new UserDTOResponse(savedUser.getUserId(), savedUser.getName());
    }

    public UserDTOResponse updateById(Long userId, UserDTORequest request) {
        UserEntity user =
                repository
                .findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setName(request.name());
        UserEntity updatedUser = repository.save(user);
        return new UserDTOResponse(updatedUser.getUserId(), updatedUser.getName());
    }

    public void deleteById(Long userId) {
        UserEntity user = repository
                .findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        repository.delete(user);
    }
}
