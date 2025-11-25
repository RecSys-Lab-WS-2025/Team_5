package de.tum.moodtrip_backend.adapter_user.dto;

public record LoginResponse(
        String token,
        UserDto user
) {
    public record UserDto(
            Long id,
            String username,
            String email
    ) {}
}
