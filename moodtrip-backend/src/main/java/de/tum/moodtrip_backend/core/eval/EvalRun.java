package de.tum.moodtrip_backend.core.eval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class EvalRun {

    public static final String CTX_KEY = "EVAL_RUN";
    public static final String CTX_ROUTE_TYPE_KEY = "EVAL_ROUTE_TYPE"; // BALANCED/YOUR_PICKS/DISCOVERY

    private final String runId = UUID.randomUUID().toString();
    private final Instant startTime = Instant.now();
    private volatile Instant endTime;

    // HTTP
    private final AtomicReference<String> httpMethod = new AtomicReference<>("");
    private final AtomicReference<String> httpPath = new AtomicReference<>("");

    // Request inputs (we fill from RouteService)
    private final Map<String, Object> request = new ConcurrentHashMap<>();

    // Overpass
    private final AtomicLong overpassLatencyMs = new AtomicLong(-1);
    private final AtomicLong overpassRawCount = new AtomicLong(-1);
    private final AtomicLong overpassTrustFilteredCount = new AtomicLong(-1);

    // OSRM metrics per routeType
    private final Map<String, Object> osrmByRouteType = new ConcurrentHashMap<>();

    // Final itinerary metrics per routeType
    private final Map<String, Object> itineraryByRouteType = new ConcurrentHashMap<>();

    // Total latency
    private final AtomicLong totalLatencyMs = new AtomicLong(-1);

    public String runId() { return runId; }

    public void setHttp(String method, String path) {
        httpMethod.set(method);
        httpPath.set(path);
    }

    public void putRequest(String key, Object value) {
        if (key != null) request.put(key, value);
    }

    public void markOverpass(long latencyMs, long rawCount, long trustFilteredCount) {
        overpassLatencyMs.set(latencyMs);
        overpassRawCount.set(rawCount);
        overpassTrustFilteredCount.set(trustFilteredCount);
    }

    // ---- OSRM per routeType ----
    public void markOsrmOk(String routeType, long latencyMs, double totalDistanceM, int legs) {
        if (routeType == null) routeType = "UNKNOWN";
        Map<String, Object> m = routeTypeMap(osrmByRouteType, routeType);
        m.put("status", "ok");
        m.put("latencyMs", latencyMs);
        m.put("totalDistanceM", totalDistanceM);
        m.put("legs", legs);
    }

    public void markOsrmFail(String routeType, long latencyMs, String error) {
        if (routeType == null) routeType = "UNKNOWN";
        Map<String, Object> m = routeTypeMap(osrmByRouteType, routeType);
        m.put("status", "fail");
        m.put("latencyMs", latencyMs);
        m.put("error", error);
    }

    public void markOsrmRouteStats(String routeType, double meanInterPoiDistM, double maxInterPoiDistM, int geometryPoints) {
        if (routeType == null) routeType = "UNKNOWN";
        Map<String, Object> m = routeTypeMap(osrmByRouteType, routeType);
        m.put("meanInterPoiDistM", meanInterPoiDistM);
        m.put("maxInterPoiDistM", maxInterPoiDistM);
        m.put("geometryPoints", geometryPoints);
    }

    // ---- Final itinerary per routeType ----
    public void markItineraryMetrics(String routeType, Map<String, Object> metrics) {
        if (routeType == null) routeType = "UNKNOWN";
        itineraryByRouteType.put(routeType, metrics);
    }

    public void finish(long totalLatencyMs) {
        this.endTime = Instant.now();
        this.totalLatencyMs.set(totalLatencyMs);
    }

    private static Map<String, Object> routeTypeMap(Map<String, Object> container, String routeType) {
        Object existing = container.get(routeType);
        if (existing instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) existing;
            return m;
        }
        Map<String, Object> created = new ConcurrentHashMap<>();
        container.put(routeType, created);
        return created;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "EVAL_RUN");
        root.put("runId", runId);
        root.put("startTime", startTime.toString());
        root.put("endTime", endTime != null ? endTime.toString() : null);
        root.put("httpMethod", httpMethod.get());
        root.put("httpPath", httpPath.get());
        root.put("totalLatencyMs", totalLatencyMs.get());

        Map<String, Object> overpass = new LinkedHashMap<>();
        overpass.put("latencyMs", overpassLatencyMs.get());
        overpass.put("rawCount", overpassRawCount.get());
        overpass.put("trustFilteredCount", overpassTrustFilteredCount.get());
        root.put("overpass", overpass);

        root.put("request", new LinkedHashMap<>(request));
        root.put("osrmByRouteType", new LinkedHashMap<>(osrmByRouteType));
        root.put("itineraryByRouteType", new LinkedHashMap<>(itineraryByRouteType));
        return root;
    }

    public String toJsonLine(ObjectMapper om) {
        try {
            return om.writeValueAsString(toMap());
        } catch (JsonProcessingException e) {
            return "EVAL_RUN_JSON_FAILED runId=" + runId + " err=" + e;
        }
    }
}
