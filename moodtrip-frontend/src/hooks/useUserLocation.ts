import * as React from "react";

export type UserLocation = {
  latitude: number;
  longitude: number;
  accuracy?: number;
};

export function useUserLocation(opts?: {
  enableHighAccuracy?: boolean;
  timeout?: number;
  maximumAge?: number;
  watch?: boolean;
}) {
  const {
    enableHighAccuracy = true,
    timeout = 10_000,
    maximumAge = 10_000,
    watch = false,
  } = opts ?? {};

  const [location, setLocation] = React.useState<UserLocation | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const watchIdRef = React.useRef<number | null>(null);

  const requestOnce = React.useCallback(() => {
    if (!navigator.geolocation) {
      setError("Geolocation is not supported in this browser.");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setLocation({
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
          accuracy: pos.coords.accuracy,
        });
        setError(null);
      },
      (err) => {
        setError(err.message || "Unable to fetch location.");
      },
      { enableHighAccuracy, timeout, maximumAge }
    );
  }, [enableHighAccuracy, timeout, maximumAge]);

  const startWatch = React.useCallback(() => {
    if (!navigator.geolocation) {
      setError("Geolocation is not supported in this browser.");
      return;
    }
    if (watchIdRef.current != null) return;

    watchIdRef.current = navigator.geolocation.watchPosition(
      (pos) => {
        setLocation({
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
          accuracy: pos.coords.accuracy,
        });
        setError(null);
      },
      (err) => {
        setError(err.message || "Unable to fetch location.");
      },
      { enableHighAccuracy, timeout, maximumAge }
    );
  }, [enableHighAccuracy, timeout, maximumAge]);

  const stopWatch = React.useCallback(() => {
    if (watchIdRef.current != null) {
      navigator.geolocation.clearWatch(watchIdRef.current);
      watchIdRef.current = null;
    }
  }, []);

  React.useEffect(() => {
    if (!watch) return;
    startWatch();
    return () => stopWatch();
  }, [watch, startWatch, stopWatch]);

  return { location, error, requestOnce, startWatch, stopWatch };
}
