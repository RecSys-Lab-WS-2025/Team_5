package de.tum.moodtrip_backend.adapter_music.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PlaylistMapper {


    public List<String> extractTrackIdsFromJson(JsonNode recommendationJson) {
        List<String> trackIds = new ArrayList<>();

        if (recommendationJson == null || !recommendationJson.has("content")) {
            System.err.println("No content field in recommendation JSON");
            return trackIds;
        }

        for (JsonNode item : recommendationJson.get("content")) {
            String href = item.path("href").asText(null);

            if (href != null && href.contains("/track/")) {
                String trackId = href.substring(href.lastIndexOf("/") + 1);
                trackIds.add(trackId);
            }
        }

        System.out.println("Track IDs " + trackIds);
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