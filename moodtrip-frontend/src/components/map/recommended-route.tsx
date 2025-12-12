import { useEffect, useMemo } from "react";
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

type Props = {
  data?: FeatureCollection | null;
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

export function RecommendedRouteMap({ data }: Props) {
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
            id: props.osmId ?? props.id ?? `${lat}-${lon}`,
            name: props.displayName ?? "Unknown POI",
            description: props.description ?? "",
            imageUrl: props.imageUrl ?? "",
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
                  <h3 className="text-sm font-semibold">{poi.name}</h3>
                  {poi.description && (
                <p className="mt-1 text-xs text-muted-foreground">
                  {poi.description}
                </p>
              )}
            </div>
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
