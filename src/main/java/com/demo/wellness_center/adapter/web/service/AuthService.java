package com.demo.wellness_center.adapter.web.service;

import com.demo.wellness_center.adapter.web.dto.AuthDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AuthService {
    public AuthDto.UserResponse register(AuthDto.RegisterRequest r);
    public AuthDto.AuthResponse login(AuthDto.LoginRequest r);
    public AuthDto.MessageResponse sendEmailOtp(String email);
    public AuthDto.AuthResponse verifyEmailOtp(AuthDto.EmailOtpRequest r);
    public AuthDto.MessageResponse sendPhoneOtp(String phone);
    public AuthDto.AuthResponse verifyPhoneOtp(AuthDto.PhoneOtpRequest r);
    public AuthDto.AuthResponse loginWithGoogle(Map<String, Object> attrs);
    public AuthDto.AuthResponse refresh(String refreshToken);
    public AuthDto.MessageResponse logout(String authorizationHeader, String refreshToken);
    public AuthDto.MessageResponse forgotPassword(String email);
    public AuthDto.MessageResponse resetPassword(AuthDto.ResetPasswordRequest r);
    public AuthDto.MessageResponse changePassword(UUID userId, AuthDto.ChangePasswordRequest r);


//    public AuthDto.UserResponse getProfile(UUID id);
//    public List<AuthDto.UserResponse> listUsers(int page, int size);
//    public AuthDto.UserResponse updatePermissions(UUID id, AuthDto.PermissionsRequest r);
//    public AuthDto.UserResponse updateProfile(UUID id, AuthDto.UpdateProfileRequest r);
}