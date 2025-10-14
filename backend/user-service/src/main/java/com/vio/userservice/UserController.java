package com.vio.userservice;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;

    @GetMapping
    public List<UserDTOResponse> getAllUsers() {
        return service.getAllUsers();
    }

    @GetMapping("/id={userId}")
    public UserDTOResponse findById(@PathVariable Long userId) {
        return service.findById(userId);
    }

    @PostMapping
    public UserDTOResponse createUser(@RequestBody @Valid UserDTORequest request) {
        return service.createUser(request);
    }

    @PutMapping("/id={userId}")
    public UserDTOResponse updateById(@PathVariable Long userId, @RequestBody @Valid UserDTORequest request) {
        return service.updateById(userId, request);
    }

    @DeleteMapping("/id={userId}")
    public void deleteById(@PathVariable Long userId) {
        service.deleteById(userId);
    }
}
