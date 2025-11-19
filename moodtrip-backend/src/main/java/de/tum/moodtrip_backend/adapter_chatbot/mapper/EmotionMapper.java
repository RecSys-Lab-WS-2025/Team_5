package de.tum.moodtrip_backend.adapter_chatbot.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.EmotionResult;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class EmotionMapper {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static EmotionResult fromJson(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode scoresNode = root.get("scores");
            if (scoresNode == null || !scoresNode.isObject()) {
                throw new RuntimeException("Invalid emotion JSON: missing or invalid 'scores' field");
            }
            if (root.get("top_score") == null
                    || root.get("top_label") == null
                    || root.get("success") == null
                    || root.get("content") == null) {
                throw new RuntimeException("Invalid emotion JSON: missing field");
            }
            Map<Emotion, Double> scores = new HashMap<>();

            Iterator<String> fieldNames = scoresNode.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                scores.put(Emotion.valueOf(key), scoresNode.get(key).asDouble());
            }

            Emotion topLabel = Emotion.valueOf(root.get("top_label").asText());
            double topScore = root.get("top_score").asDouble();
            boolean success = root.get("success").asBoolean();
            String content = root.get("content").asText();

            return new EmotionResult(scores, topLabel, topScore, content, success);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse emotion JSON", e);
        }
    }
}
