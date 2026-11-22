package com.demo.wellness_center.domain.port;


import com.demo.wellness_center.domain.models.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPort {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    List<User> findAll(int page, int size);
}