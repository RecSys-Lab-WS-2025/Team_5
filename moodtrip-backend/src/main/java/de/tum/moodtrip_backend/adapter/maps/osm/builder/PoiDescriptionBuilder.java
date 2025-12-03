package de.tum.moodtrip_backend.adapter.maps.osm.builder;

import de.tum.moodtrip_backend.core.model.POI;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public final class PoiDescriptionBuilder {

    private PoiDescriptionBuilder() {
    }

    /**
     * Returns a safe display name for the POI.
     * If OSM name is null, derives something from tags (e.g. "Table tennis table", "Basketball court", "Sports shop",
     * "Guidepost", "Viewpoint", "Beach", "Memorial", etc.).
     */
    public static String buildDisplayName(POI poi) {
        if (poi.name() != null && !poi.name().isBlank()) {
            return poi.name();
        }

        Map<String, String> t = poi.tags();
        String type = mainType(t);

        // If there is a human-friendly description, use it as a fallback name
        String description = t.get("description");
        if (description != null && !description.isBlank()) {
            return StringUtils.capitalize(description);
        }

        // Tourism / information specific
        if ("information".equals(t.get("tourism"))) {
            String infoType = t.get("information");
            if ("guidepost".equals(infoType)) {
                return "Guidepost";
            }
            return "Tourist information";
        }

        // Viewpoint
        if ("viewpoint".equals(t.get("tourism"))) {
            return "Viewpoint";
        }

        // Beach resorts
        if ("beach_resort".equals(t.get("leisure"))) {
            return "Beach";
        }

        // Memorials
        if ("memorial".equals(t.get("historic"))) {
            String memorialType = t.get("memorial");
            if ("bust".equals(memorialType)) {
                return "Memorial bust";
            }
            if ("sculpture".equals(memorialType)) {
                return "Memorial sculpture";
            }
            return "Memorial";
        }

        // Natural peaks
        if ("peak".equals(t.get("natural"))) {
            return "Peak";
        }

        // Try to refine type for common sports pitch cases
        if ("pitch".equals(t.get("leisure")) && t.containsKey("sport")) {
            String sport = t.get("sport");
            return switch (sport) {
                case "table_tennis" -> "Table tennis table";
                case "basketball" -> "Basketball court";
                case "soccer", "football" -> "Football pitch";
                default -> StringUtils.capitalize(wordsFromKey(sport) + " pitch");
            };
        }

        // Sports shop
        if ("sports".equals(t.get("shop"))) {
            String sport = t.get("sport");
            if (sport != null && !sport.isBlank()) {
                return StringUtils.capitalize(wordsFromKey(sport) + " shop");
            }
            return "Sports shop";
        }

        // Hotels (if we get here without a name/description)
        if ("hotel".equals(t.get("tourism"))) {
            return "Hotel";
        }

        // Generic fallback based on the main type tag
        if (type != null) {
            return StringUtils.capitalize(wordsFromKey(type));
        }

        // Very generic fallback
        return "Place of interest";
    }

    /**
     * Builds a short description, preferring a Wikipedia summary if provided, and
     * falling back to the tag-based description.
     *
     * @param poi              the POI
     * @param wikipediaSummary optional short summary text from Wikipedia (already fetched)
     */
    public static String buildShortDescription(POI poi, String wikipediaSummary) {
        if (StringUtils.isNotBlank(wikipediaSummary)) {
            // Combine display name and short Wikipedia extract, truncated to a reasonable length
            return buildDisplayName(poi) + ". " + wikipediaSummary.trim();
        }
        return buildShortDescription(poi);
    }

    /**
     * Builds a short, one-line description based purely on OSM tags.
     * Example: "Table tennis table at Alte Schönhauser Straße 55 (Berlin). Public and free. Outdoor. Wheelchair accessible."
     */
    public static String buildShortDescription(POI poi) {
        Map<String, String> t = poi.tags();

        StringBuilder sb = new StringBuilder();

        // 1) Subject: type + location
        String displayName = buildDisplayName(poi);
        String street = streetText(t);
        String city = cityText(t);

        sb.append(displayName);

        if (street != null || city != null) {
            sb.append(" at ");
            if (street != null) {
                sb.append(street);
                if (city != null) {
                    sb.append(" (").append(city).append(")");
                }
            } else {
                sb.append(city);
            }
        }

        sb.append(".");

        // 2) Access / fee
        String access = t.get("access");
        String fee = t.get("fee");

        if (access != null || fee != null) {
            sb.append(" ");

            if (access != null) {
                // Only mention if not the default "yes"
                switch (access) {
                    case "private" -> sb.append("Private access");
                    case "customers" -> sb.append("Access for customers");
                    case "no" -> sb.append("No public access");
                    default -> sb.append("Public access"); // "yes" or other – usually you can treat as public
                }
            }

            if (fee != null) {
                if (access != null) sb.append(", ");
                if ("no".equals(fee)) {
                    sb.append("free");
                } else if ("yes".equals(fee)) {
                    sb.append("fee required");
                } else {
                    sb.append("fee: ").append(fee);
                }
            }

            sb.append(".");
        }

        // 3) Indoor / outdoor / covered
        String indoor = t.get("indoor");
        String covered = t.get("covered");

        if (indoor != null || covered != null) {
            sb.append(" ");
            if ("yes".equals(indoor)) {
                sb.append("Indoor");
            } else if ("no".equals(indoor)) {
                sb.append("Outdoor");
            } else if (indoor != null) {
                sb.append("Indoor: ").append(indoor);
            }

            if (covered != null) {
                if (indoor != null) sb.append(", ");
                if ("yes".equals(covered)) {
                    sb.append("covered");
                } else if ("no".equals(covered)) {
                    sb.append("not covered");
                } else {
                    sb.append("covered: ").append(covered);
                }
            }

            sb.append(".");
        }

        // 4) Wheelchair
        String wheelchair = t.get("wheelchair");
        if (wheelchair != null) {
            sb.append(" ");
            switch (wheelchair) {
                case "yes" -> sb.append("Wheelchair accessible.");
                case "no" -> sb.append("Not wheelchair accessible.");
                case "limited" -> sb.append("Partially wheelchair accessible.");
                default -> sb.append("Wheelchair: ").append(wheelchair).append(".");
            }
        }

        // 5) Opening hours (if it is something like a shop/bar etc.)
        String opening = t.get("opening_hours");
        if (opening != null && !opening.isBlank()) {
            sb.append(" Open ").append(opening).append(".");
        }

        // 6) Type-specific extra hints

        // Hotels – mention star rating if present
        if ("hotel".equals(t.get("tourism"))) {
            String stars = t.get("stars");
            if (stars != null && !stars.isBlank()) {
                sb.append(" ").append(stars).append("-star hotel.");
            } else {
                sb.append(" Hotel.");
            }
        }

        // Beach resorts – add a short hint and optionally the description if it wasn't used as the name
        if ("beach_resort".equals(t.get("leisure"))) {
            sb.append(" Beach area");
            String desc = t.get("description");
            boolean nameIsEmpty = poi.name() == null || poi.name().isBlank();
            if (desc != null && !desc.isBlank() && !nameIsEmpty) {
                sb.append(" (").append(desc).append(")");
            }
            sb.append(".");
        }

        // Guideposts
        if ("information".equals(t.get("tourism")) && "guidepost".equals(t.get("information"))) {
            sb.append(" Guidepost");
            if ("yes".equals(t.get("bicycle"))) {
                sb.append(" for cyclists");
            }
            sb.append(".");
        }

        // Historic memorials
        if ("memorial".equals(t.get("historic"))) {
            sb.append(" Memorial site");
            String memorialType = t.get("memorial");
            if (memorialType != null && !memorialType.isBlank()) {
                sb.append(" (").append(wordsFromKey(memorialType)).append(")");
            }
            sb.append(".");
        }

        // Natural peaks – mention elevation if available
        if ("peak".equals(t.get("natural"))) {
            String ele = t.get("ele");
            sb.append(" ");
            if (ele != null && !ele.isBlank()) {
                sb.append("Elevation ").append(ele).append(" m.");
            } else {
                sb.append("Small hill / peak.");
            }
        }

        return sb.toString().trim();
    }

    // ----------------- helpers -----------------

    private static String mainType(Map<String, String> t) {
        if (t.containsKey("amenity")) return t.get("amenity");
        if (t.containsKey("tourism")) return t.get("tourism");
        if (t.containsKey("leisure")) return t.get("leisure");
        if (t.containsKey("shop")) return t.get("shop");
        if (t.containsKey("natural")) return t.get("natural");
        if (t.containsKey("historic")) return t.get("historic");
        return null;
    }

    private static String streetText(Map<String, String> t) {
        String street = t.get("addr:street");
        String house = t.get("addr:housenumber");
        if (street == null || street.isBlank()) return null;
        return (house != null && !house.isBlank()) ? street + " " + house : street;
    }

    private static String cityText(Map<String, String> t) {
        String city = t.get("addr:city");
        String suburb = t.get("addr:suburb");
        if (city != null && suburb != null) return city + " – " + suburb;
        return city != null ? city : suburb;
    }

    private static String wordsFromKey(String key) {
        if (key == null || key.isBlank()) return "";
        return key.replace('_', ' ');
    }
}