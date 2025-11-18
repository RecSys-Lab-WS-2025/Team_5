package de.tum.moodtrip_backend.adapter_user.mapper;

import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.adapter_user.dto.UserResponse;
import de.tum.moodtrip_backend.core.model.UserProfile;

@Component
public class UserDtoMapper {

    public UserResponse toResponse(UserProfile domain) {
        if (domain == null) return null;
        return new UserResponse(domain.id(), domain.username(), domain.email(), domain.createdAt());
    }
}
