package de.tum.moodtrip_backend.adapter.maps.osm.mapper;

import de.tum.moodtrip_backend.core.model.PoiCategory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OsmTagCategoryMapperTest {

    @Test
    void prioritizesFoodOverOtherCategories() {
        Map<String, String> tags = Map.of(
                "amenity", "restaurant",
                "tourism", "museum",
                "shop", "gift"
        );

        assertThat(OsmTagCategoryMapper.map(tags)).contains(PoiCategory.FOOD_AND_CULINARY);
    }

    @Test
    void mapsHistoricPlacesToHistory() {
        Map<String, String> tags = Map.of(
                "historic", "castle",
                "name", "Old Castle"
        );

        assertThat(OsmTagCategoryMapper.map(tags)).contains(PoiCategory.HISTORY_AND_CULTURE);
    }

    @Test
    void mapsParkToRelaxation() {
        Map<String, String> tags = Map.of(
                "leisure", "park",
                "name", "Central Park"
        );

        assertThat(OsmTagCategoryMapper.map(tags)).contains(PoiCategory.RELAXATION);
    }

    @Test
    void returnsEmptyForUnknownTags() {
        assertThat(OsmTagCategoryMapper.map(Map.of("amenity", "parking"))).isEmpty();
    }
}
