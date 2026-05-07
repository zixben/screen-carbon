package com.lks.dto;

import com.lks.bean.User;

public record UserResponse(
        Integer id,
        String fullName,
        String username,
        String email,
        Boolean valid,
        String description,
        String role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getValid(),
                user.getDescription(),
                user.getRole()
        );
    }
}
