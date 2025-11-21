package de.tum.moodtrip_backend.adapter_database.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("spotify_tokens")
public class SpotifyTokenEntity {
    @Id
    private Long id;
    @Column("access_token")
    private String accessToken;
    @Column("refresh_token")
    private String refreshToken;
    @Column("expires_in")
    private Long expiresIn;
    @Column("fetched_at")
    private Long fetchedAt;
    @Column("spotify_user_id")
    private String spotifyUserId;
    @Column("spotify_email")
    private String spotifyEmail;
    @Column("spotify_display_name")
    private String spotifyDisplayName;

    public SpotifyTokenEntity() {
    }

    public SpotifyTokenEntity(Long id, String accessToken, String refreshToken, Long expiresIn, Long fetchedAt, String spotifyUserId, String spotifyEmail, String spotifyDisplayName) {
        this.id = id;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.fetchedAt = fetchedAt;
        this.spotifyUserId = spotifyUserId;
        this.spotifyEmail = spotifyEmail;
        this.spotifyDisplayName = spotifyDisplayName;
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

    public String getSpotifyUserId() {
        return spotifyUserId;
    }

    public void setSpotifyUserId(String spotifyUserId) {
        this.spotifyUserId = spotifyUserId;
    }

    public String getSpotifyEmail() {
        return spotifyEmail;
    }

    public void setSpotifyEmail(String spotifyEmail) {
        this.spotifyEmail = spotifyEmail;
    }

    public String getSpotifyDisplayName() {
        return spotifyDisplayName;
    }

    public void setSpotifyDisplayName(String spotifyDisplayName) {
        this.spotifyDisplayName = spotifyDisplayName;
    }
}