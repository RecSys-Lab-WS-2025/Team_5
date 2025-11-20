package de.tum.moodtrip_backend.adapter_database.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("spotify_tokens")
public class SpotifyTokenEntity {
    @Id
    private Long id;
    @Column("user_id")
    private Long userId;
    @Column("access_token")
    private String accessToken;
    @Column("refresh_token")
    private String refreshToken;
    @Column("expires_in")
    private Long expiresIn;
    @Column("fetched_at")
    private Long fetchedAt;

    public SpotifyTokenEntity() {
    }

    public SpotifyTokenEntity(Long id, Long userId, String accessToken, String refreshToken, Long expiresIn, Long fetchedAt) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.fetchedAt = fetchedAt;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Long getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Long fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}