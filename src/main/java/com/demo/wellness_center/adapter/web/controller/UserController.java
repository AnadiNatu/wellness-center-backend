package com.demo.wellness_center.adapter.web.controller;

import com.demo.wellness_center.adapter.web.dto.AuthDto;
import com.demo.wellness_center.adapter.web.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/profile")
    public AuthDto.UserResponse profile(Authentication a) {
        return userService.getProfile(UUID.fromString(a.getName()));
    }

    @PutMapping("/profile/update")
    public AuthDto.UserResponse update(Authentication a, @RequestBody AuthDto.UpdateProfileRequest r) {
        return userService.updateProfile(UUID.fromString(a.getName()), r);
    }
}