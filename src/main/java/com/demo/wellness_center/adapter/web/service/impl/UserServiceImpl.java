package com.demo.wellness_center.adapter.web.service.impl;

import com.demo.wellness_center.adapter.web.dto.AuthDto;
import com.demo.wellness_center.adapter.web.service.UserService;
import com.demo.wellness_center.domain.enums.AuthProvider;
import com.demo.wellness_center.domain.enums.UserRole;
import com.demo.wellness_center.domain.models.User;
import com.demo.wellness_center.domain.port.UserPort;
import com.demo.wellness_center.exception.GlobalExceptionHandler;
import com.demo.wellness_center.infra.CloudinaryService;
import com.demo.wellness_center.infra.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserPort userPort;
    private final PasswordEncoder encoder;
    private final NotificationService notificationService;
    private final CloudinaryService cloudinaryService;

    @Value("${app.approval-required:false}")
    private boolean approvalRequired;

    @Transactional
    public AuthDto.UserResponse register(AuthDto.RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (userPort.existsByEmail(email)) {
            throw new GlobalExceptionHandler.DuplicateResourceException("Email already registered");
        }
        String phone = blankToNull(r.phoneNumber());
        if (phone != null && userPort.existsByPhoneNumber(phone)) {
            throw new GlobalExceptionHandler.DuplicateResourceException("Phone number already registered");
        }

        User u = new User();
        u.setFirstName(r.firstName());
        u.setLastName(r.lastName());
        u.setEmail(email);
        u.setPassword(encoder.encode(r.password()));
        u.setPhoneNumber(phone);
        u.setCountryCode(r.countryCode());
        u.setRole(UserRole.USER);              // always USER
        u.setProvider(AuthProvider.LOCAL);
        u.setApproved(!approvalRequired);
        return AuthDto.UserResponse.from(userPort.save(u));
    }

    public AuthDto.UserResponse getProfile(UUID id) {
        return AuthDto.UserResponse.from(find(id));
    }

    @Transactional
    public AuthDto.UserResponse updateProfile(UUID id, AuthDto.UpdateProfileRequest r) {
        User u = find(id);
        String phone = blankToNull(r.phoneNumber());
        if (phone != null && !phone.equals(u.getPhoneNumber()) && userPort.existsByPhoneNumber(phone)) {
            throw new GlobalExceptionHandler.DuplicateResourceException("Phone number already registered");
        }
        if (r.firstName() != null) u.setFirstName(r.firstName());
        if (r.lastName() != null) u.setLastName(r.lastName());
        if (phone != null && !phone.equals(u.getPhoneNumber())) {
            u.setPhoneNumber(phone);
            u.setPhoneVerified(false);
        }
        if (r.countryCode() != null) u.setCountryCode(r.countryCode());
        if (r.dateOfBirth() != null) u.setDateOfBirth(r.dateOfBirth());
        if (r.gender() != null) u.setGender(r.gender());
        if (r.bio() != null) u.setBio(r.bio());
        if (r.address() != null) u.setAddress(r.address());
        if (r.city() != null) u.setCity(r.city());
        if (r.state() != null) u.setState(r.state());
        if (r.country() != null) u.setCountry(r.country());
        if (r.postalCode() != null) u.setPostalCode(r.postalCode());
        return AuthDto.UserResponse.from(userPort.save(u));
    }

    // ---- admin operations ----

    public List<AuthDto.UserResponse> listUsers(int page, int size) {
        return userPort.findAll(page, size).stream().map(AuthDto.UserResponse::from).toList();
    }

    @Transactional
    public AuthDto.UserResponse updatePermissions(UUID id, AuthDto.PermissionsRequest r) {
        User u = find(id);
        if (r.isActive() != null) u.setActive(r.isActive());
        if (r.isApproved() != null) u.setApproved(r.isApproved());
        if (r.isBookingEnabled() != null) u.setBookingEnabled(r.isBookingEnabled());
        if (r.isReviewEnabled() != null) u.setReviewEnabled(r.isReviewEnabled());
        if (r.isNotificationEnabled() != null) u.setNotificationEnabled(r.isNotificationEnabled());
        return AuthDto.UserResponse.from(userPort.save(u));
    }

    private User find(UUID id) {
        return userPort.findById(id).orElseThrow(() -> new GlobalExceptionHandler.ResourceNotFoundException("User not found"));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

}
