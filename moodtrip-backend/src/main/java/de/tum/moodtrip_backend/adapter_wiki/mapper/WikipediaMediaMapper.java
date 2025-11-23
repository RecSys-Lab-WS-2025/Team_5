package de.tum.moodtrip_backend.adapter_wiki.mapper;

import com.fasterxml.jackson.databind.JsonNode;

public final class WikipediaMediaMapper {

    /**
     * Helper to check if a string is a valid HTTP(S) URL.
     */
    public static boolean isValidHttpUrl(String url) {
        try {
            if (url == null || url.isBlank()) {
                return false;
            }
            java.net.URI uri = new java.net.URI(url.trim());
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Normalize Commons file-page URLs to Special:FilePath direct links.
     */
    public static String normalizeCommonsFileUrl(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        // Already a Special:FilePath URL
        if (trimmed.contains("Special:FilePath/")) {
            return trimmed;
        }
        // Only handle commons.wikimedia.org file pages here
        int fileIndex = trimmed.indexOf("File:");
        if (fileIndex == -1) {
            return trimmed;
        }
        String filename = trimmed.substring(fileIndex + "File:".length());
        // Defensive cleanup
        filename = filename.replace(' ', '_');
        return "https://commons.wikimedia.org/wiki/Special:FilePath/" + filename;
    }

    /**
     * Extract an image URL from a Wikipedia REST /page/summary JSON response.
     * Prefers originalimage over thumbnail.
     *
     * @param json summary JSON
     * @return normalized image URL or null if none present
     */
    public static String mapImageFromWikipediaJson(JsonNode json) {
        if (json == null) {
            return null;
        }
        JsonNode original = json.get("originalimage");
        if (original != null && original.get("source") != null) {
            return normalizeCommonsFileUrl(original.get("source").asText());
        }
        JsonNode thumb = json.get("thumbnail");
        if (thumb != null && thumb.get("source") != null) {
            return normalizeCommonsFileUrl(thumb.get("source").asText());
        }
        return null;
    }

    /**
     * Extract an image URL from a Wikidata Special:EntityData JSON for a given entity id.
     * Uses property P18 (image) and converts the filename into a Special:FilePath URL.
     *
     * @param json full entity JSON
     * @param id   wikidata Q-ID (e.g. "Q824403")
     * @return normalized image URL or null if none present
     */
    public static String mapImageFromWikidataJson(JsonNode json, String id) {
        if (json == null || id == null) {
            return null;
        }
        JsonNode entities = json.get("entities");
        if (entities == null || entities.get(id) == null) {
            return null;
        }
        JsonNode claims = entities.get(id).get("claims");
        if (claims == null) {
            return null;
        }
        JsonNode p18Array = claims.get("P18");
        if (p18Array == null || !p18Array.isArray() || p18Array.isEmpty()) {
            return null;
        }
        JsonNode first = p18Array.get(0);
        if (first == null) {
            return null;
        }
        JsonNode mainsnak = first.get("mainsnak");
        if (mainsnak == null) {
            return null;
        }
        JsonNode datavalue = mainsnak.get("datavalue");
        if (datavalue == null) {
            return null;
        }
        JsonNode value = datavalue.get("value");
        if (value == null || !value.isTextual()) {
            return null;
        }
        String filename = value.asText();
        return "https://commons.wikimedia.org/wiki/Special:FilePath/" + filename.replace(' ', '_');
    }
}
