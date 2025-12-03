package de.tum.moodtrip_backend.adapter.music.spotify.mapper;


import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.adapter.music.spotify.pojo.FeaturePair;

@Component
public class EmotionToFeatureMapper {

    private static final Logger logger = LoggerFactory.getLogger(EmotionToFeatureMapper.class);


    private static final Map<String, FeaturePair> EMOTION_FEATURE_MAP = Map.ofEntries(
            Map.entry("JOYFUL", new FeaturePair(0.8f, 0.9f)),
            Map.entry("ENERGIZED", new FeaturePair(0.9f, 0.8f)),
            Map.entry("CALM", new FeaturePair(0.3f, 0.7f)),
            Map.entry("CONTENT", new FeaturePair(0.5f, 0.8f)),
            Map.entry("HOPEFUL", new FeaturePair(0.6f, 0.8f)),
            Map.entry("GRATEFUL", new FeaturePair(0.4f, 0.9f)),
            Map.entry("CURIOUS", new FeaturePair(0.7f, 0.7f)),
            Map.entry("NOSTALGIC", new FeaturePair(0.4f, 0.6f)),
            Map.entry("NEUTRAL", new FeaturePair(0.5f, 0.5f)),
            Map.entry("CONFUSED", new FeaturePair(0.3f, 0.4f)),
            Map.entry("BORED", new FeaturePair(0.2f, 0.3f)),
            Map.entry("TIRED", new FeaturePair(0.2f, 0.5f)),
            Map.entry("LONELY", new FeaturePair(0.3f, 0.3f)),
            Map.entry("SAD", new FeaturePair(0.25f, 0.2f)),
            Map.entry("ANXIOUS", new FeaturePair(0.6f, 0.3f)),
            Map.entry("STRESSED", new FeaturePair(0.7f, 0.3f)),
            Map.entry("FRUSTRATED", new FeaturePair(0.8f, 0.2f)),
            Map.entry("ANGRY", new FeaturePair(0.9f, 0.2f)),
            Map.entry("OVERWHELMED", new FeaturePair(0.7f, 0.3f))
    );


    public FeaturePair map(String emotionLabel) {
        logger.debug("Mapping emotion label: {}", emotionLabel);
        return EMOTION_FEATURE_MAP.getOrDefault(
                emotionLabel.toUpperCase(),
                new FeaturePair(0.5f, 0.5f)
        );
    }
}
