package com.vio.userservice.controller;

import com.vio.userservice.dto.UserDTORequest;
import com.vio.userservice.dto.UserDTOResponse;
import com.vio.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;

//    @GetMapping
//    public ResponseEntity<List<UserDTOResponse>> getAllUsers() {
//        List<UserDTOResponse> users = service.getAllUsers();
//        return ResponseEntity.ok(users);
//    }

    @GetMapping("/id={userId}")
    public ResponseEntity<UserDTOResponse> findById(@PathVariable Long userId) {
        UserDTOResponse user = service.findById(userId);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/id={userId}")
    public ResponseEntity<UserDTOResponse> updateById(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> updates) {
        UserDTOResponse user = service.updateById(userId, updates);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<UserDTOResponse> createUser(@RequestBody @Valid UserDTORequest request) {
        UserDTOResponse user = service.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }


//    @DeleteMapping("/id={userId}")
//    public ResponseEntity<Void> deleteById(@PathVariable Long userId) {
//        service.deleteById(userId);
//        return ResponseEntity.noContent().build();
//    }
}