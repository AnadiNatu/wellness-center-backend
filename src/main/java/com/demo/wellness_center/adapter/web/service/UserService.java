package com.demo.wellness_center.adapter.web.service;

import com.demo.wellness_center.adapter.web.dto.AuthDto;

import java.util.List;
import java.util.UUID;

public interface UserService {
    public AuthDto.UserResponse register(AuthDto.RegisterRequest r);
    public AuthDto.UserResponse getProfile(UUID id);
    public AuthDto.UserResponse updateProfile(UUID id, AuthDto.UpdateProfileRequest r);
    public List<AuthDto.UserResponse> listUsers(int page, int size);
    public AuthDto.UserResponse updatePermissions(UUID id, AuthDto.PermissionsRequest r);

}