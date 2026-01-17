package de.tum.moodtrip_backend.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_profile")
public class UserEntity {

    @Id
    private Long id;

    private String username;

    private String email;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("password_hash")
    private String passwordHash;

    @Column("spotify_token_id")
    private Long spotifyTokenId;

    @Column("avatar_url")
    private String avatarUrl;

    public UserEntity() {}

    public UserEntity(
            Long id,
            String username,
            String email,
            LocalDateTime createdAt,
            String passwordHash,
            Long spotifyTokenId,
            String avatarUrl
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.createdAt = createdAt;
        this.passwordHash = passwordHash;
        this.spotifyTokenId = spotifyTokenId;
        this.avatarUrl = avatarUrl;
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPasswordHash() { return passwordHash; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Long getSpotifyTokenId() { return spotifyTokenId; }

    public void setSpotifyTokenId(Long spotifyTokenId) { this.spotifyTokenId = spotifyTokenId; }

    public String getAvatarUrl() { return avatarUrl; }

    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
