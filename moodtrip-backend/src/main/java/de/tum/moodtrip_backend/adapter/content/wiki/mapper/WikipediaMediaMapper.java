package de.tum.moodtrip_backend.adapter.content.wiki.mapper;

import com.fasterxml.jackson.databind.JsonNode;

public final class WikipediaMediaMapper {

    /**
     * Helper to check if a string is a valid HTTP(S) URL.
     *
     * @param url the URL to validate, may be null or blank
     * @return true if url is a valid HTTP(S) URL, false otherwise
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
     *
     * @param url the Commons file URL to normalize, may be null
     * @return normalized Special:FilePath URL, or the original URL if not a file page, or null if input is null
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
        if (trimmed.contains("//upload.wikimedia.org/")) {
            return trimmed;
        }

        // Handle index.php?title=File:... / Datei:... style URLs
        String fromTitleParam = extractFilenameFromTitleParam(trimmed);
        if (fromTitleParam != null) {
            return "https://commons.wikimedia.org/wiki/Special:FilePath/" + fromTitleParam;
        }

        int wikiIndex = trimmed.indexOf("/wiki/");
        if (wikiIndex == -1) {
            return trimmed;
        }

        String filename = extractFilename(trimmed, wikiIndex);

        return "https://commons.wikimedia.org/wiki/Special:FilePath/" + filename;
    }

    private static String extractFilename(String trimmed, int wikiIndex) {
        String titleSegment = trimmed.substring(wikiIndex + "/wiki/".length());

        int hashIndex = titleSegment.indexOf('#');
        if (hashIndex >= 0) {
            titleSegment = titleSegment.substring(0, hashIndex);
        }
        int queryIndex = titleSegment.indexOf('?');
        if (queryIndex >= 0) {
            titleSegment = titleSegment.substring(0, queryIndex);
        }

        // Title can be "File:Foo.jpg", "Datei:Foo.jpg", "Fichier:Foo.jpg", etc.
        // We only care about the part after the first ':' which is the actual filename.
        int colonIndex = titleSegment.indexOf(':');
        String filename = (colonIndex >= 0) ? titleSegment.substring(colonIndex + 1) : titleSegment;

        // Defensive cleanup
        filename = filename.replace(' ', '_');
        return filename;
    }

    private static String extractFilenameFromTitleParam(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String query = uri.getRawQuery();
            if (query == null || !query.contains("title=")) {
                return null;
            }
            for (String param : query.split("&")) {
                int eq = param.indexOf('=');
                if (eq <= 0) continue;
                String key = param.substring(0, eq);
                if (!"title".equalsIgnoreCase(key)) continue;
                String value = java.net.URLDecoder.decode(param.substring(eq + 1), java.nio.charset.StandardCharsets.UTF_8);
                if (value.isBlank()) return null;
                int colon = value.indexOf(':');
                String filename = colon >= 0 ? value.substring(colon + 1) : value;
                return filename.replace(' ', '_');
            }
        } catch (Exception ignored) {
        }
        return null;
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
