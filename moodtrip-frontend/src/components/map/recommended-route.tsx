import { useEffect, useMemo, useState, useRef } from "react";
import type {Feature, FeatureCollection, GeoJsonObject, Point} from "geojson";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import {GeoJSON, useMap} from "react-leaflet";
import {
  Map,
  MapLayers,
  MapLayersControl,
  MapLocateControl,
  MapMarker,
  MapPopup,
  MapTileLayer,
  MapZoomControl,
} from "@/components/ui/map";
import {Loader2, MapPin, Star, Navigation2 } from "lucide-react";
import {fetchPoiRating, submitPoiRating} from "@/api/emotion";
import {useUserLocation} from "@/hooks/useUserLocation";
import {cn} from "@/lib/utils";

type Props = {
  data?: FeatureCollection | null;
  emotion?: string;
  invalidateKey?: unknown;
  selectedDay?: number;
  activePoiId?: string | null;
};

function FitToData({ data }: { data?: FeatureCollection | null }) {
  const map = useMap();

  useEffect(() => {
    if (!map || !data) return;

    let layer: L.GeoJSON | null = null;
    try {
      layer = L.geoJSON(data as GeoJsonObject);
      const bounds = layer.getBounds();
      if (bounds.isValid()) {
        map.fitBounds(bounds, { padding: [32, 32] });
      }
    } catch (error) {
      console.error("Unable to fit map to provided data", error);
    }

    requestAnimationFrame(() => map.invalidateSize());

    return () => {
      if (layer) map.removeLayer(layer);
    };
  }, [map, data]);

  return null;
}

function InvalidateOnResize({ dep }: { dep: unknown }) {
  const map = useMap();

  useEffect(() => {
    if (!map) return;

    let raf = 0;
    const start = performance.now();
    const durationMs = 420;

    const tick = (now: number) => {
      map.invalidateSize(false);
      if (now - start < durationMs) {
        raf = requestAnimationFrame(tick);
      }
    };

    raf = requestAnimationFrame(tick);

    const t1 = window.setTimeout(() => map.invalidateSize(false), 120);
    const t2 = window.setTimeout(() => map.invalidateSize(false), 240);
    const t3 = window.setTimeout(() => map.invalidateSize(false), 420);

    return () => {
      cancelAnimationFrame(raf);
      window.clearTimeout(t1);
      window.clearTimeout(t2);
      window.clearTimeout(t3);
    };
  }, [map, dep]);

  return null;
}

function StarItem({
  value,
  hoverValue,
  ratingValue,
  onHover,
  onClick,
  disabled,
}: {
  value: number;
  hoverValue: number;
  ratingValue: number;
  onHover: (v: number) => void;
  onClick: (v: number) => void;
  disabled: boolean;
}) {
  const displayValue = hoverValue || ratingValue;
  const isFull = displayValue >= value;
  const isHalf = !isFull && displayValue >= value - 0.5;

  return (
    <div className="relative h-5 w-5 group">
      <Star className="h-5 w-5 text-muted-foreground/30" />
      <div
        className={cn(
          "absolute inset-0 overflow-hidden transition-all duration-200",
          isHalf ? "w-1/2" : isFull ? "w-full" : "w-0"
        )}
      >
        <Star className="h-5 w-5 fill-yellow-400 text-yellow-400" />
      </div>

      {!disabled && (
        <div className="absolute inset-0 flex z-10" onMouseLeave={() => onHover(0)}>
          <div
            className="w-1/2 h-full cursor-pointer"
            onMouseEnter={() => onHover(value - 0.5)}
            onClick={() => onClick(value - 0.5)}
          />
          <div
            className="w-1/2 h-full cursor-pointer"
            onMouseEnter={() => onHover(value)}
            onClick={() => onClick(value)}
          />
        </div>
      )}
    </div>
  );
}

function normalizeLineCoords(coords: unknown): [number, number][] | null {
  if (!Array.isArray(coords)) return null;
  const normalized: [number, number][] = [];
  for (const coord of coords) {
    if (!Array.isArray(coord) || coord.length < 2) continue;
    const lon = coord[0];
    const lat = coord[1];
  if (typeof lon !== "number" || typeof lat !== "number") continue;
    normalized.push([lon, lat]);
  }
  return normalized.length ? normalized : null;
}

function PoiRating({
  poiId,
  category,
  emotion,
}: {
  poiId: string;
  category: string;
  emotion: string;
}) {
  const [rating, setRating] = useState<number>(0);
  const [hover, setHover] = useState<number>(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);

  useEffect(() => {
    if (!poiId || !emotion) return;
    let active = true;
    fetchPoiRating(poiId, emotion).then((res) => {
      if (active && res) setRating(res.rating);
    });
    return () => {
      active = false;
    };
  }, [poiId, emotion]);

  const handleRatingClick = async (val: number) => {
    if (isSubmitting) return;

    const previousRating = rating;
    setRating(val);
    setIsSubmitting(true);
    setShowSuccess(false);

    try {
      const success = await submitPoiRating({
        poiId: String(poiId),
        category,
        emotion,
        rating: val,
      });

      setIsSubmitting(false);
      if (success) {
        setShowSuccess(true);
        window.setTimeout(() => {
          setShowSuccess(false);
        }, 3000);
      } else {
        setRating(previousRating);
      }
    } catch (error) {
      setIsSubmitting(false);
      setRating(previousRating);
    }
  };

  return (
    <div
      className="mt-4 pt-4 border-t border-slate-100 flex flex-col items-center justify-center space-y-3 w-full"
      onClick={(e) => {
        e.stopPropagation();
      }}
    >
      <div className="flex flex-col items-center">
        <p className="text-[10px] font-bold uppercase tracking-widest text-slate-400 mb-1">
          {showSuccess ? "Success" : "Your Experience"}
        </p>
        <div className="h-0.5 w-6 bg-amber-400/40 rounded-full" />
      </div>

      <div
        className={`flex items-center gap-0.5 ${
          isSubmitting ? "opacity-50 pointer-events-none" : ""
        }`}
      >
        {[1, 2, 3, 4, 5].map((star) => (
          <StarItem
            key={star}
            value={star}
            hoverValue={hover}
            ratingValue={rating}
            onHover={setHover}
            onClick={handleRatingClick}
            disabled={isSubmitting}
          />
        ))}
      </div>

      <div className="flex items-center h-4">
        {isSubmitting ? (
          <Loader2 className="h-3 w-3 animate-spin text-slate-400" />
        ) : (hover || rating) > 0 ? (
          <span className="text-xs font-bold tabular-nums text-amber-600">
            {(hover || rating).toFixed(1)} / 5.0
          </span>
        ) : (
          <span className="text-[10px] text-slate-300">Tap to rate</span>
        )}
      </div>
    </div>
  );
}

export function RecommendedRouteMap({
  data,
  emotion,
  invalidateKey,
  selectedDay,
  activePoiId,
}: Props) {
  const { location: userLoc } = useUserLocation({ watch: true });
  const markerRefs = useRef<Record<string, L.Marker | null>>({});

  useEffect(() => {
    if (activePoiId && markerRefs.current[activePoiId]) {
      markerRefs.current[activePoiId]?.openPopup();
    }
  }, [activePoiId]);

  const activeEmotion = useMemo(() => {
    if (emotion) return emotion;
    const routeFeature = data?.features.find((f) => f.properties?.type === "route");
    return routeFeature?.properties?.emotion;
  }, [data, emotion]);

  const safeData = useMemo(() => {
    if (!data) return null;
    try {
      const layer = L.geoJSON(data as GeoJsonObject);
      layer.remove();
      return data;
    } catch (error) {
      console.error("Invalid GeoJSON data for map rendering", error);
      return null;
    }
  }, [data]);

  const initialCenter = useMemo(() => {
    if (!safeData?.features?.length) return [48.137154, 11.576124] as [number, number];
    try {
      const coords = L.geoJSON(safeData as GeoJsonObject).getBounds().getCenter();
      return [coords.lat, coords.lng] as [number, number];
    } catch (error) {
      return [48.137154, 11.576124] as [number, number];
    }
  }, [safeData]);

  const pois = useMemo(() => {
    if (!safeData || !safeData.features) return [];
    return safeData.features
      .filter((f): f is Feature<Point> => f.geometry?.type === "Point" && f.properties?.type === "poi")
      .map((f) => {
        const props = f.properties ?? {};
        const [lon, lat] = (f.geometry as Point).coordinates;
        return {
          id: props.osmId?.toString() ?? props.id?.toString() ?? `${lat}-${lon}`,
          name: props.displayName ?? "Unknown POI",
          description: props.description ?? "",
          imageUrl: props.imageUrl ?? "",
          category: props.category ?? "NATURE",
          position: [lat, lon] as [number, number],
          day: typeof props.day === "number" ? props.day : 1,
        };
      });
  }, [safeData]);

  const routeLineCoords = useMemo(() => {
    if (!safeData || !safeData.features) return null;
    const routeFeature = safeData.features.find(
      (f) => f.properties?.type === "route" && f.geometry?.type === "LineString"
    );
    if (!routeFeature || routeFeature.geometry?.type !== "LineString") return null;
    return normalizeLineCoords(
        (routeFeature.geometry as { coordinates?: unknown }).coordinates
    );
  }, [safeData?.features]);

  const activeDay =
    Number.isFinite(selectedDay) && Number(selectedDay) > 0
      ? Number(selectedDay)
      : null;
  const dayPois = useMemo(
    () => (activeDay ? pois.filter((poi) => poi.day === activeDay) : pois),
    [activeDay, pois]
  );
  const inactivePois = useMemo(
    () => (activeDay ? pois.filter((poi) => poi.day !== activeDay) : []),
    [activeDay, pois]
  );

  const findNearestLineIndex = (
    point: [number, number],
    coords: [number, number][]
  ) => {
    let closestIndex = -1;
    let closestDistance = Number.POSITIVE_INFINITY;
    const [lat, lon] = point;

    for (let i = 0; i < coords.length; i++) {
      const [coordLon, coordLat] = coords[i];
      const dLat = lat - coordLat;
      const dLon = lon - coordLon;
      const distance = dLat * dLat + dLon * dLon;
      if (distance < closestDistance) {
        closestDistance = distance;
        closestIndex = i;
      }
    }

    return closestIndex;
  };

  const dayLineData = useMemo(() => {
    if (!activeDay || dayPois.length < 2 || !routeLineCoords) return null;
    const indices = dayPois
      .map((poi) => findNearestLineIndex(poi.position, routeLineCoords))
      .filter((idx) => idx >= 0);
    if (indices.length < 2) return null;

    const minIdx = Math.min(...indices);
    const maxIdx = Math.max(...indices);
    if (maxIdx <= minIdx) return null;

    return {
      type: "FeatureCollection",
      features: [
        {
          type: "Feature",
          geometry: {
            type: "LineString",
            coordinates: routeLineCoords.slice(minIdx, maxIdx + 1),
          },
          properties: { type: "day-route" },
        },
      ],
    } as FeatureCollection;
  }, [activeDay, dayPois, routeLineCoords]);

  const fitData = useMemo(() => {
    if (!safeData) return null;
    if (!activeDay) return safeData;
    const features: Feature[] = [];
    if (dayLineData?.features?.length) {
      features.push(...(dayLineData.features as Feature[]));
    }
    for (const poi of dayPois) {
      features.push({
        type: "Feature",
        geometry: {
          type: "Point",
          coordinates: [poi.position[1], poi.position[0]],
        },
        properties: { type: "poi" },
      } as Feature);
    }
    return { type: "FeatureCollection", features } as FeatureCollection;
  }, [activeDay, dayLineData, dayPois, safeData]);

  const renderMarkerIcon = (isActive: boolean, isFirst: boolean = false, isLast: boolean = false) => {
    let colorClass = "text-slate-400 opacity-50";
    if (isActive) {
      if (isFirst) colorClass = "text-green-600 drop-shadow-md";
      else if (isLast) colorClass = "text-red-600 drop-shadow-md";
      else colorClass = "text-blue-600 drop-shadow";
    }
    return <MapPin className={`h-6 w-6 ${colorClass}`} />;
  };

  return (
    <div className="h-full w-full min-w-0">
      <Map center={initialCenter} zoom={13} className="h-full w-full">
        <InvalidateOnResize dep={invalidateKey} />

        <MapLayers>
          <MapLayersControl />
          <MapTileLayer />
          <MapTileLayer
            name="Satellite"
            url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
            attribution="Tiles &copy; Esri &mdash; Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community"
          />
          <MapLocateControl />
        </MapLayers>
        
        <MapZoomControl />

        {userLoc && (
          <MapMarker
            position={[userLoc.latitude, userLoc.longitude]}
            icon={
              <div className="relative flex items-center justify-center">
                <div className="absolute h-4 w-4 rounded-full bg-blue-500 animate-ping opacity-75"></div>
                <div className="relative h-4 w-4 rounded-full bg-blue-600 border-2 border-white shadow-md flex items-center justify-center">
                  <Navigation2 className="h-2.5 w-2.5 text-white fill-current" />
                </div>
              </div>
            }
          />
        )}

        {[...inactivePois, ...dayPois].map((poi) => {
          const isActive = dayPois.some((p) => p.id === poi.id);
          const isFirst = isActive && dayPois[0]?.id === poi.id;
          const isLast = isActive && dayPois[dayPois.length - 1]?.id === poi.id;

          return (
            <MapMarker
              key={poi.id}
              position={poi.position}
              icon={renderMarkerIcon(isActive, isFirst, isLast)}
              ref={(ref) => {
                markerRefs.current[poi.id] = ref;
              }}
            >
              <MapPopup
                className="w-64 rounded-xl overflow-hidden border-none shadow-xl [&_.leaflet-popup-close-button]:hidden"
                closeButton={false}
              >
                <div className="space-y-2">
                  {poi.imageUrl && (
                    <img
                      src={poi.imageUrl}
                      alt={poi.name}
                    className="h-32 w-full rounded-md object-cover"
                    />
                  )}
                  <div>
                  <h3 className="line-clamp-1 text-sm font-semibold">{poi.name}</h3>
                    {poi.description && (
                    <p className="mt-1 line-clamp-2 text-xs text-muted-foreground">
                        {poi.description}
                      </p>
                    )}
                  </div>

                {activeEmotion && (
                  <PoiRating poiId={poi.id} category={poi.category} emotion={activeEmotion} />
                )}
                </div>
              </MapPopup>
            </MapMarker>
          );
        })}

        {safeData && (
          <>
            <GeoJSON
              data={safeData as GeoJsonObject}
              style={() => ({
                color: activeDay ? "#94a3b8" : "#2563eb",
                weight: activeDay ? 3 : 4,
                opacity: activeDay ? 0.5 : 1,
              })}
              filter={(feature) => feature.geometry?.type !== "Point"}
            />
            {dayLineData && (
              <GeoJSON
                key={`day-route-${activeDay ?? "all"}`}
                data={dayLineData as GeoJsonObject}
                style={() => ({ color: "#2563eb", weight: 5, opacity: 0.95 })}
                filter={(feature) => feature.geometry?.type !== "Point"}
              />
            )}
            <FitToData data={fitData} />
          </>
        )}
      </Map>
    </div>
  );
}
