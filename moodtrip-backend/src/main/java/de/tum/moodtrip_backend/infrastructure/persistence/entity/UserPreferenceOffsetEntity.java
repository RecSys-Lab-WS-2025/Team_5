package de.tum.moodtrip_backend.infrastructure.persistence.entity;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;

@Table("user_preference_offsets")
public class UserPreferenceOffsetEntity {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;
    private String emotion;
    private String category;
    @Column("preference_offset")
    private Double preferenceOffset;
    private Long count;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    public UserPreferenceOffsetEntity() {
    }

    public UserPreferenceOffsetEntity(Long id, Long userId, String emotion, String category, Double preferenceOffset, Long count, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.emotion = emotion;
        this.category = category;
        this.preferenceOffset = preferenceOffset;
        this.count = count;
        this.updatedAt = updatedAt;
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

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getPreferenceOffset() {
        return preferenceOffset;
    }

    public void setPreferenceOffset(Double preferenceOffset) {
        this.preferenceOffset = preferenceOffset;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
