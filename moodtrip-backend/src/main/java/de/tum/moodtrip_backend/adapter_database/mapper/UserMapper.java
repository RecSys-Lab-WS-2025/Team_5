package de.tum.moodtrip_backend.adapter_database.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.adapter_database.entity.UserEntity;
import de.tum.moodtrip_backend.core.model.UserProfile;

@Component
public class UserMapper {
    
    public UserProfile toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserProfile(
            entity.getId(),
            entity.getUsername(),
            entity.getEmail(),
            entity.getCreatedAt()
        );
    }
    
    public UserEntity toEntity(UserProfile domain) {
        if (domain == null) {
            return null;
        }
        return new UserEntity(
            domain.id(),
            domain.username(),
            domain.email(),
            domain.createdAt() != null ? domain.createdAt() : LocalDateTime.now()
        );
    }
}
