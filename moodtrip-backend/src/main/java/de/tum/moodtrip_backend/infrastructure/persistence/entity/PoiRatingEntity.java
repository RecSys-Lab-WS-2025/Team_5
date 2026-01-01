package de.tum.moodtrip_backend.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("poi_ratings")
public class PoiRatingEntity {
    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("poi_id")
    private String poiId;

    private String category;
    private String emotion;
    private Double rating;

    @Column("created_at")
    private LocalDateTime createdAt;

    public PoiRatingEntity() {
    }

    public PoiRatingEntity(Long id, Long userId, String poiId, String category, String emotion, Double rating, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.poiId = poiId;
        this.category = category;
        this.emotion = emotion;
        this.rating = rating;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPoiId() {
        return poiId;
    }

    public void setPoiId(String poiId) {
        this.poiId = poiId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
