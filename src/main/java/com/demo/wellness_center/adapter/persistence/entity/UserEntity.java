package com.demo.wellness_center.adapter.persistence.entity;
import com.demo.wellness_center.domain.enums.AuthProvider;
import com.demo.wellness_center.domain.enums.Gender;
import com.demo.wellness_center.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = @Index(name = "idx_users_role", columnList = "role"))
@Getter
@Setter
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String firstName;
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Column(unique = true)
    private String phoneNumber;

    private String countryCode;
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    private String providerId;
    private String profilePhotoUrl;
    private String profilePhotoPublicId;

    @Column(columnDefinition = "text")
    private String bio;

    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;

    private Boolean emailVerified;
    private Boolean phoneVerified;
    private Boolean isActive;
    private Boolean isApproved;
    private Boolean isBookingEnabled;
    private Boolean isReviewEnabled;
    private Boolean isNotificationEnabled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
