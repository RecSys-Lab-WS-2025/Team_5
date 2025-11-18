package de.tum.moodtrip_backend.adapter_music.pojo;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("spotify_tokens")
public class SpotifyToken {
    @Id
    @Column("user_id")
    private String userId;
    @Column("access_token")
    private String accessToken;
    @Column("refresh_token")
    private String refreshToken;
    @Column("expires_in")
    private Long expiresIn;
    @Column("fetched_at")
    private Long fetchedAt;

    public SpotifyToken() {
    }

    public SpotifyToken(String accessToken, Long expiresIn, Long fetchedAt, String refreshToken, String userId) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.fetchedAt = fetchedAt;
        this.refreshToken = refreshToken;
        this.userId = userId;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}