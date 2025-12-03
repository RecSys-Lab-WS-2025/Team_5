package de.tum.moodtrip_backend.adapter.music.spotify.mapper;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class PlaylistMapper {

    private static final Logger logger = LoggerFactory.getLogger(PlaylistMapper.class);


    public List<String> extractTrackIdsFromJson(JsonNode recommendationJson) {
        List<String> trackIds = new ArrayList<>();

        if (recommendationJson == null || !recommendationJson.has("content")) {
            logger.warn("No content field in recommendation JSON");
            return trackIds;
        }

        for (JsonNode item : recommendationJson.get("content")) {
            String href = item.path("href").asText(null);

            if (href != null && href.contains("/track/")) {
                String trackId = href.substring(href.lastIndexOf("/") + 1);
                trackIds.add(trackId);
            }
        }

        logger.debug("Track IDs {}", trackIds);
        return trackIds;
    }


    public List<String> toSpotifyUris(List<String> trackIds) {
        List<String> uris = new ArrayList<>();
        for (String id : trackIds) {
            uris.add("spotify:track:" + id);
        }
        return uris;
    }
}