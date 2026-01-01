package de.tum.moodtrip_backend.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("emotion_category_scores")
public class EmotionCategoryScoreEntity {
    @Id
    private Long id;
    private String emotion;
    private String category;
    private Double score;

    @Column("rating_count")
    private Long ratingCount;

    public EmotionCategoryScoreEntity() {
    }

    public EmotionCategoryScoreEntity(Long id, String emotion, String category, Double score, Long ratingCount) {
        this.id = id;
        this.emotion = emotion;
        this.category = category;
        this.score = score;
        this.ratingCount = ratingCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Long getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Long ratingCount) {
        this.ratingCount = ratingCount;
    }
}
