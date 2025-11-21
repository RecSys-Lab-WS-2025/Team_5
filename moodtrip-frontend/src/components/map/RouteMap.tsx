import { useEffect } from "react";
import { MapContainer, TileLayer, GeoJSON, useMap } from "react-leaflet";
import type { FeatureCollection } from "geojson";
import L from "leaflet";

type RouteMapProps = {
  data: FeatureCollection;
};

function FitBounds({ data }: { data: FeatureCollection }) {
  const map = useMap();

  useEffect(() => {
    const layer = L.geoJSON(data as any);
    const bounds = layer.getBounds();
    if (bounds.isValid()) {
      map.fitBounds(bounds, { padding: [24, 24] });
    }
  }, [data, map]);

  return null;
}

export function RouteMap({ data }: RouteMapProps) {
  return (
    <div className="h-[400px] w-full">
      <MapContainer
        center={[48.2457, 11.5625]}
        zoom={13}
        scrollWheelZoom
        className="h-full w-full rounded-xl border"
      >
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution="&copy; OpenStreetMap contributors"
        />

        <GeoJSON
          data={data as any}
          style={() => ({
            color: "#2563eb",
            weight: 4,
          })}
        />

        <FitBounds data={data} />
      </MapContainer>
    </div>
  );
}

export default RouteMap;