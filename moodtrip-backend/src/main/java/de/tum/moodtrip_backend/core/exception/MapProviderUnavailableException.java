package de.tum.moodtrip_backend.core.exception;

/**
 * Signals that the upstream map/POI provider (e.g., Overpass) is unavailable or repeatedly timed out.
 * Lives in the core to avoid service-layer dependencies on adapter implementations.
 */
public class MapProviderUnavailableException extends RuntimeException {

    public MapProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public MapProviderUnavailableException(Throwable cause) {
        super("Map provider unavailable", cause);
    }
}
