import { useEffect, useMemo, useState } from "react";
import type { FeatureCollection, GeoJsonObject, Feature, Point } from "geojson";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { GeoJSON, useMap } from "react-leaflet";
import {
  Map,
  MapLayers,
  MapLayersControl,
  MapLocateControl,
  MapTileLayer,
  MapZoomControl,
  MapMarker,
  MapPopup,
} from "@/components/ui/map";
import { Star, Loader2 } from "lucide-react";
import { submitPoiRating, fetchPoiRating } from "@/api/emotion";

type Props = {
  data?: FeatureCollection | null;
  emotion?: string;
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

    // Recalculate after paint to avoid size issues
    requestAnimationFrame(() => map.invalidateSize());

    return () => {
      if (layer) {
        map.removeLayer(layer);
      }
    };
  }, [map, data]);

  return null;
}

function StarItem({
  value,
  hoverValue,
  ratingValue,
  onHover,
  onClick,
  disabled
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
      <div className={`absolute inset-0 overflow-hidden transition-all duration-200 ${isHalf ? "w-1/2" : isFull ? "w-full" : "w-0"}`}>
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
    if (!poiId || !emotion) {
      console.log("PoiRating: Initial fetch skipped - missing poiId or emotion", { poiId, emotion });
      return;
    }
    let active = true;
    fetchPoiRating(poiId, emotion).then((res) => {
      if (active && res) {
        console.log("PoiRating: Initial rating loaded", res);
        setRating(res.rating);
      }
    });
    return () => {
      active = false;
    };
  }, [poiId, emotion]);

  const handleRatingClick = async (val: number) => {
    if (isSubmitting) return;

    if (!poiId || !category || !emotion) {
      console.error("PoiRating: Cannot submit - missing required data", { poiId, category, emotion });
      alert("Error: Rating context missing (emotion/category). Please try again or refresh.");
      return;
    }

    const previousRating = rating;
    setRating(val);
    setIsSubmitting(true);
    setShowSuccess(false);

    console.log("PoiRating: Submitting rating...", { poiId, category, emotion, rating: val });

    const success = await submitPoiRating({
      poiId: String(poiId),
      category,
      emotion,
      rating: val,
    });

    setIsSubmitting(false);
    if (success) {
      console.log("PoiRating: Submit SUCCESS");
      setShowSuccess(true);
      setTimeout(() => setShowSuccess(false), 3000);
    } else {
      console.error("PoiRating: Submit FAILED");
      setRating(previousRating);
    }
  };

  return (
    <div className="mt-2 space-y-2 border-t pt-2">
      <div className="flex items-center justify-between">
        <p className="text-[10px] font-medium uppercase tracking-wider text-muted-foreground">
          Rate this place
        </p>
        <div className="flex items-center gap-2">
          {showSuccess && (
            <span className="text-[10px] font-medium text-green-600 animate-in fade-in slide-in-from-right-1">
              Saved
            </span>
          )}
          {isSubmitting && <Loader2 className="h-3 w-3 animate-spin text-muted-foreground" />}
        </div>
      </div>
      <div className={`flex items-center gap-0.5 ${isSubmitting ? "opacity-50 pointer-events-none" : ""}`}>
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
        {(hover || rating) > 0 && (
          <span className="ml-2 text-xs font-semibold tabular-nums text-yellow-600">
            {hover || rating}
          </span>
        )}
      </div>
    </div>
  );
}

export function RecommendedRouteMap({ data, emotion }: Props) {
  const activeEmotion = useMemo(() => {
    if (emotion) return emotion;
    // Fallback: look for route feature with emotion property
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

  // Try to grab a reasonable initial center from the first coordinate
  const initialCenter = useMemo(() => {
    if (!safeData?.features?.length)
      return [48.137154, 11.576124] as [number, number];

    try {
      const coords = L.geoJSON(safeData as GeoJsonObject)
        .getBounds()
        .getCenter();

      return [coords.lat, coords.lng] as [number, number];
    } catch (error) {
      console.error("Failed to derive map center from data", error);
      return [48.137154, 11.576124] as [number, number];
    }
  }, [safeData]);

  // Extract POIs from the FeatureCollection
  const pois = useMemo(
    () =>
      safeData?.features
        ?.filter(
          (f): f is Feature<Point> =>
            f.geometry?.type === "Point" && f.properties?.type === "poi"
        )
        .map((f) => {
          const props = f.properties ?? {};
          const [lon, lat] = (f.geometry as Point).coordinates;

          return {
            id: props.osmId?.toString() ?? props.id?.toString() ?? `${lat}-${lon}`,
            name: props.displayName ?? "Unknown POI",
            description: props.description ?? "",
            imageUrl: props.imageUrl ?? "",
            category: props.category ?? "NATURE", // Fallback to nature if missing
            position: [lat, lon] as [number, number], // Leaflet expects [lat, lon]
          };
        }) ?? [],
    [safeData?.features]
  );

  return (
    <div className="w-full min-w-[240px] h-[360px] md:h-[420px]">
      <Map
        center={initialCenter}
        zoom={13}
        className="h-full w-full rounded-xl border"
      >
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

        {pois.map((poi) => (
          <MapMarker key={poi.id} position={poi.position}>
            <MapPopup className="w-64">
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
                  <PoiRating
                    poiId={poi.id}
                    category={poi.category}
                    emotion={activeEmotion}
                  />
                )}
              </div>
            </MapPopup>
          </MapMarker>
        ))}

        {safeData && (
          <>
            <GeoJSON
              data={safeData as GeoJsonObject}
              style={() => ({
                color: "#2563eb",
                weight: 4,
              })}
              // Do not render Point features via GeoJSON; we have custom markers for POIs
              filter={(feature) => feature.geometry?.type !== "Point"}
            />
            <FitToData data={safeData} />
          </>
        )}
      </Map>
    </div>
  );
}
