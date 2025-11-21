import { RouteMap } from "@/components/map/RouteMap";
import { dummyRoute } from "@/data/dummyRoute";

export default function RoutePreview() {
  return (
    <div className="p-6">
      <h1 className="mb-4 text-xl font-semibold">Recommended Route</h1>
      <p className="mb-4 text-sm text-muted-foreground">
        This map uses dummy GeoJSON data.
      </p>

      <RouteMap data={dummyRoute} />

      <div className="mt-6 text-sm opacity-60">
        Additional route details can be shown here (distance, duration, mood, etc.).
      </div>
    </div>
  );
}