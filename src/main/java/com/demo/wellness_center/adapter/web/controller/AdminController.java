package com.demo.wellness_center.adapter.web.controller;

import com.demo.wellness_center.adapter.web.dto.AuthDto;
import com.demo.wellness_center.adapter.web.service.AuthService;
import com.demo.wellness_center.adapter.web.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserService userService;

    @GetMapping("/users")
    public List<AuthDto.UserResponse> users(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return userService.listUsers(page, size);
    }

    @GetMapping("/users/{id}")
    public AuthDto.UserResponse user(@PathVariable UUID id) {
        return userService.getProfile(id);
    }

    @PatchMapping("/users/{id}/permissions")
    public AuthDto.UserResponse permissions(@PathVariable UUID id, @RequestBody AuthDto.PermissionsRequest r) {
        return userService.updatePermissions(id, r);
    }
}