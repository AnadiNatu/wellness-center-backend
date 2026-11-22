package com.demo.wellness_center.adapter.web.service.impl;

import com.demo.wellness_center.adapter.security.JwtService;
import com.demo.wellness_center.adapter.web.dto.AuthDto;
import com.demo.wellness_center.adapter.web.service.AuthService;
import com.demo.wellness_center.adapter.web.service.UserService;
import com.demo.wellness_center.domain.enums.AuthProvider;
import com.demo.wellness_center.domain.enums.UserRole;
import com.demo.wellness_center.domain.models.User;
import com.demo.wellness_center.domain.port.UserPort;
import com.demo.wellness_center.exception.GlobalExceptionHandler;
import com.demo.wellness_center.infra.NotificationService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserPort userPort;
    private final UserService userService;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final StringRedisTemplate redis;
    private final NotificationService notifier;

    @Value("${app.approval-required:false}")
    private boolean approvalRequired;

    @Value("${app.notify-on-login:false}")
    private boolean notifyOnLogin;

    // ================= registration & password login =================

    @Override
    public AuthDto.UserResponse register(AuthDto.RegisterRequest r) {
        AuthDto.UserResponse created = userService.register(r);
        sendEmailOtp(created.email());
        return created;
    }

    @Override
    public AuthDto.AuthResponse login(AuthDto.LoginRequest r) {
        String email = lower(r.email());
        String lockId = "login:" + email;
        assertNotLocked(lockId);
        User u = userPort.findByEmail(email).orElse(null);
        if (u == null || u.getPassword() == null || !encoder.matches(r.password(), u.getPassword())) {
            recordFailure(lockId);
            throw new GlobalExceptionHandler.UnauthorizedException("Invalid email or password");
        }
        redis.delete("fails:" + lockId);
        assertCanLogin(u);
        return issue(u);
    }

    // ================= email OTP =================

    @Override
    public AuthDto.MessageResponse sendEmailOtp(String email) {
        userPort.findByEmail(lower(email)).ifPresent(u -> {
            String code = newOtp("email:" + u.getEmail());
            notifier.sendEmail(u.getEmail(), "Your verification code",
                    "Your code is " + code + ". It expires in 5 minutes. Do not share it with anyone.");
        });
        return new AuthDto.MessageResponse("If the account exists, an OTP has been sent");
    }

    /** Verifies the email and logs the user in. */
    @Override
    public AuthDto.AuthResponse verifyEmailOtp(AuthDto.EmailOtpRequest r) {
        User u = userPort.findByEmail(lower(r.email())).orElseThrow(() -> new GlobalExceptionHandler.InvalidOperationException("Invalid OTP"));
        checkOtp("email:" + u.getEmail(), r.otp());
        assertCanLogin(u);
        u.setEmailVerified(true);
        return issue(u);
    }

    // ================= phone OTP =================
    @Override
    public AuthDto.MessageResponse sendPhoneOtp(String phone) {
        userPort.findByPhoneNumber(phone.trim()).ifPresent(u -> {
            String code = newOtp("phone:" + u.getPhoneNumber());
            notifier.sendSms(e164(u), "Your verification code is " + code + ". It expires in 5 minutes.");
        });
        return new AuthDto.MessageResponse("If the number is registered, an OTP has been sent");
    }

    /** Verifies the phone and logs the user in. */
    @Override
    public AuthDto.AuthResponse verifyPhoneOtp(AuthDto.PhoneOtpRequest r) {
        User u = userPort.findByPhoneNumber(r.phoneNumber().trim()).orElseThrow(() -> new GlobalExceptionHandler.InvalidOperationException("Invalid OTP"));
        checkOtp("phone:" + u.getPhoneNumber(), r.otp());
        assertCanLogin(u);
        u.setPhoneVerified(true);
        return issue(u);
    }

    // ================= Google =================
    @Override
    public AuthDto.AuthResponse loginWithGoogle(Map<String, Object> attrs) {
        String email = lower((String) attrs.get("email"));
        String sub = (String) attrs.get("sub");
        if (email == null || sub == null || !Boolean.TRUE.equals(attrs.get("email_verified"))) {
            throw new GlobalExceptionHandler.UnauthorizedException("Google email is not verified");
        }

        User u = userPort.findByEmail(email).orElse(null);
        if (u == null) {
            u = new User();
            u.setEmail(email);
            u.setFirstName(str(attrs.get("given_name"), "Google"));
            u.setLastName(str(attrs.get("family_name"), "User"));
            u.setProfilePhotoUrl((String) attrs.get("picture"));
            u.setRole(UserRole.USER);
            u.setProvider(AuthProvider.GOOGLE);
            u.setProviderId(sub);
            u.setEmailVerified(true);
            u.setApproved(!approvalRequired);
        } else if (u.getProviderId() != null) {
            if (!u.getProviderId().equals(sub)) {
                throw new GlobalExceptionHandler.UnauthorizedException("This account is linked to a different Google account");
            }
        } else {
            // Link only when the existing local account already proved email ownership.
            if (!Boolean.TRUE.equals(u.getEmailVerified())) {
                throw new GlobalExceptionHandler.ForbiddenException("Verify your email first, then sign in with Google");
            }
            u.setProviderId(sub);
        }
        assertCanLogin(u);
        return issue(u);
    }

    // ================= tokens =================
    @Override
    public AuthDto.AuthResponse refresh(String refreshToken) {
        Claims c = jwt.parse(refreshToken, JwtService.REFRESH);
        User u = userPort.findById(UUID.fromString(c.getSubject()))
                .orElseThrow(() -> new GlobalExceptionHandler.UnauthorizedException("Invalid token"));
        assertCanLogin(u);
        jwt.revoke(refreshToken); // rotation: the old refresh token cannot be reused
        return tokens(u);
    }

    @Override
    public AuthDto.MessageResponse logout(String authorizationHeader, String refreshToken) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt.revoke(authorizationHeader.substring(7));
        }
        if (refreshToken != null) {
            jwt.revoke(refreshToken);
        }
        return new AuthDto.MessageResponse("Logged out");
    }

    // ================= password management =================
    @Override
    public AuthDto.MessageResponse forgotPassword(String email) {
        userPort.findByEmail(lower(email)).ifPresent(u -> {
            String code = newOtp("reset:" + u.getEmail());
            notifier.sendEmail(u.getEmail(), "Password reset code",
                    "Your password reset code is " + code + ". It expires in 5 minutes.");
        });
        return new AuthDto.MessageResponse("If the account exists, a reset code has been sent");
    }

    @Override
    public AuthDto.MessageResponse resetPassword(AuthDto.ResetPasswordRequest r) {
        User u = userPort.findByEmail(lower(r.email())).orElseThrow(() -> new GlobalExceptionHandler.InvalidOperationException("Invalid OTP"));
        checkOtp("reset:" + u.getEmail(), r.otp());
        u.setPassword(encoder.encode(r.newPassword()));
        userPort.save(u);
        return new AuthDto.MessageResponse("Password updated");
    }

    @Override
    public AuthDto.MessageResponse changePassword(UUID userId, AuthDto.ChangePasswordRequest r) {
        User u = userPort.findById(userId).orElseThrow(() -> new GlobalExceptionHandler.ResourceNotFoundException("User not found"));
        // Google-only accounts have no password yet, so they may set one without a current password.
        if (u.getPassword() != null && !encoder.matches(r.currentPassword(), u.getPassword())) {
            throw new GlobalExceptionHandler.UnauthorizedException("Current password is incorrect");
        }
        u.setPassword(encoder.encode(r.newPassword()));
        userPort.save(u);
        return new AuthDto.MessageResponse("Password changed");
    }

    // ================= the one common path =================

    private AuthDto.AuthResponse issue(User u) {
        u.setLastLoginAt(LocalDateTime.now());
        u = userPort.save(u);
        if (notifyOnLogin && Boolean.TRUE.equals(u.getNotificationEnabled())) {
            notifier.sendEmail(u.getEmail(), "New login to your account",
                    "A login to your account was detected at " + LocalDateTime.now() + ".");
        }
        return tokens(u);
    }

    private AuthDto.AuthResponse tokens(User u) {
        return new AuthDto.AuthResponse(jwt.create(u, JwtService.ACCESS), jwt.create(u, JwtService.REFRESH), "Bearer",
                jwt.accessSeconds(), jwt.refreshSeconds(), AuthDto.UserResponse.from(u));
    }

    private void assertCanLogin(User u) {
        if (!Boolean.TRUE.equals(u.getActive())) throw new GlobalExceptionHandler.ForbiddenException("Account is disabled");
        if (!Boolean.TRUE.equals(u.getApproved())) throw new GlobalExceptionHandler.ForbiddenException("Account is awaiting admin approval");
    }

    // ================= Redis OTP + lock helpers (atomic INCR) =================

    private String newOtp(String id) {
        assertNotLocked(id);
        Long sent = redis.opsForValue().increment("sent:" + id);
        if (sent != null && sent == 1) redis.expire("sent:" + id, Duration.ofMinutes(10));
        if (sent != null && sent > 3) throw new GlobalExceptionHandler.TooManyRequestsException("Too many OTP requests. Try again in a few minutes");
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redis.opsForValue().set("otp:" + id, code, Duration.ofMinutes(5));
        redis.delete("fails:" + id);
        return code;
    }

    private void checkOtp(String id, String code) {
        assertNotLocked(id);
        String saved = redis.opsForValue().get("otp:" + id);
        if (saved == null) throw new GlobalExceptionHandler.InvalidOperationException("OTP expired or not requested");
        boolean ok = MessageDigest.isEqual(saved.getBytes(StandardCharsets.UTF_8), code.trim().getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            recordFailure(id);
            throw new GlobalExceptionHandler.InvalidOperationException("Invalid OTP");
        }
        redis.delete("otp:" + id); // single use
        redis.delete("fails:" + id);
    }

    private void assertNotLocked(String id) {
        if (Boolean.TRUE.equals(redis.hasKey("lock:" + id))) {
            throw new GlobalExceptionHandler.TooManyRequestsException("Too many failed attempts. Try again in 15 minutes");
        }
    }

    private void recordFailure(String id) {
        Long n = redis.opsForValue().increment("fails:" + id);
        if (n != null && n == 1) redis.expire("fails:" + id, Duration.ofMinutes(15));
        if (n != null && n >= MAX_ATTEMPTS) {
            redis.opsForValue().set("lock:" + id, "1", Duration.ofMinutes(15));
            redis.delete("fails:" + id);
            redis.delete("otp:" + id);
        }
    }

    // ================= small helpers =================

    private String e164(User u) {
        String p = u.getPhoneNumber();
        if (p.startsWith("+")) return p;
        return (u.getCountryCode() == null ? "" : u.getCountryCode()) + p;
    }

    private String lower(String s) { return s == null ? null : s.trim().toLowerCase(); }

    private String str(Object o, String fallback) { return o == null ? fallback : o.toString(); }
}