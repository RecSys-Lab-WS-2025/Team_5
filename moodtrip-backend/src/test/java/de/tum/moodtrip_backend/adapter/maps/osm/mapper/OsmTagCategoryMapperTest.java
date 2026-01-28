package de.tum.moodtrip_backend.adapter.maps.osm.mapper;

import de.tum.moodtrip_backend.core.model.PoiCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OsmTagCategoryMapperTest {

    @Test
    @DisplayName("Should map restaurant related tags to FOOD_AND_CULINARY")
    void prioritizesFoodOverOtherCategories() {
        // Given
        Map<String, String> tags = Map.of(
                "amenity", "restaurant",
                "tourism", "museum",
                "shop", "gift"
        );

        // When & Then
        assertThat(OsmTagCategoryMapper.map(tags)).contains(PoiCategory.FOOD_AND_CULINARY);
    }

    @Test
    @DisplayName("Should map historic tags to HISTORY_AND_CULTURE")
    void mapsHistoricPlacesToHistory() {
        // Given
        Map<String, String> tags = Map.of(
                "historic", "castle",
                "name", "Old Castle"
        );

        // When & Then
        assertThat(OsmTagCategoryMapper.map(tags)).contains(PoiCategory.HISTORY_AND_CULTURE);
    }

    @Test
    @DisplayName("Should map park related tags to RELAXATION")
    void mapsParkToRelaxation() {
        // Given
        Map<String, String> tags = Map.of(
                "leisure", "park",
                "name", "Central Park"
        );

        // When & Then
        assertThat(OsmTagCategoryMapper.map(tags)).contains(PoiCategory.RELAXATION);
    }

    @Test
    @DisplayName("Should return empty for unknown or irrelevant tags")
    void returnsEmptyForUnknownTags() {
        // Given, When & Then
        assertThat(OsmTagCategoryMapper.map(Map.of("amenity", "parking"))).isEmpty();
    }
}
