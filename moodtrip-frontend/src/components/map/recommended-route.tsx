import { useEffect, useMemo, useRef } from "react";
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

    const layer = L.geoJSON(data as GeoJsonObject);
    const bounds = layer.getBounds();

    if (bounds.isValid()) {
      map.fitBounds(bounds, { padding: [32, 32] });
    }

    // Recalculate after paint to avoid size issues
    requestAnimationFrame(() => map.invalidateSize());

    return () => {
      map.removeLayer(layer);
    };
    // intentionally run once
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return null;
}

export function RecommendedRouteMap({ data }: Props) {
  // Try to grab a reasonable initial center from the first coordinate
  const initialCenter = useMemo(() => {
    if (!data?.features?.length)
      return [48.137154, 11.576124] as [number, number];

    const coords = L.geoJSON(data as GeoJsonObject)
      .getBounds()
      .getCenter();

    return [coords.lat, coords.lng] as [number, number];
  }, [data]);

  // Extract POIs from the FeatureCollection
  const pois = useMemo(
    () =>
      data?.features
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
    [data]
  );

  const poisLogRef = useRef<string | null>(null);
  useEffect(() => {
    const serialized = JSON.stringify(pois);
    if (serialized !== poisLogRef.current) {
      console.log("pois ", pois);
      poisLogRef.current = serialized;
    }
  }, [pois]);
  console.log("pois ", pois);

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

        {data && (
          <>
            <GeoJSON
              data={data as GeoJsonObject}
              style={() => ({
                color: "#2563eb",
                weight: 4,
              })}
              // Do not render Point features via GeoJSON; we have custom markers for POIs
              filter={(feature) => feature.geometry?.type !== "Point"}
            />
            <FitToData data={data} />
          </>
        )}
      </Map>
    </div>
  );
}
