package com.demo.wellness_center.adapter.persistence.mapper;

import com.demo.wellness_center.adapter.persistence.entity.UserEntity;
import com.demo.wellness_center.domain.models.User;
import org.springframework.beans.BeanUtils;

public class UserMapper {
    public User toDomain(UserEntity e) {
        User u = new User();
        BeanUtils.copyProperties(e, u);
        return u;
    }

    public static UserEntity toEntity(User u) {
        UserEntity e = new UserEntity();
        BeanUtils.copyProperties(u, e);
        return e;
    }
}