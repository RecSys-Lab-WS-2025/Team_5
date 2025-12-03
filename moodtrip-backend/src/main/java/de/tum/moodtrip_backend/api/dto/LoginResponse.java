package de.tum.moodtrip_backend.api.dto;

public record LoginResponse(
        String token,
        UserDto user
) {
    public record UserDto(Long id, String username, String email) {
    }
}
