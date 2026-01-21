import { RouteCard } from "./route-card";
import type { FeatureCollection, Feature, Geometry } from "geojson";
import type { AppRouteType } from "@/api/conversation";

export interface RouteCardData {
  id: string;
  title: string;
  dayDescriptions: Record<string, string>;
  imageUrl: string;
  distanceMeters: number;
  durationSeconds: number;
  geoJson?: FeatureCollection;
  routeType?: AppRouteType;
  routeTypeTitle?: string;
}

interface RouteCarouselProps {
  routes: RouteCardData[];
  onRouteClick: (route: RouteCardData) => void;
}

export function RouteCarousel({ routes, onRouteClick }: RouteCarouselProps) {
  // Get the first day's description for card preview
  const getPreviewDescription = (dayDescriptions: Record<string, string>) => {
    const firstDayDescription = dayDescriptions?.["1"];
    if (firstDayDescription) {
      return firstDayDescription;
    }

    const descriptions = Object.values(dayDescriptions || {});
    if (descriptions.length > 0 && descriptions[0]) {
      return descriptions[0];
    }

    return "A personalized route based on your mood.";
  };

  // Extract tripDays from geoJson route feature
  const getTripDays = (geoJson?: FeatureCollection): number => {
    if (!geoJson?.features) return 1;
    const routeFeature = geoJson.features.find(
      (f: Feature<Geometry>) => (f.properties as Record<string, unknown>)?.type === "route"
    );
    const tripDays = (routeFeature?.properties as Record<string, unknown>)?.tripDays;
    return typeof tripDays === "number" ? tripDays : 1;
  };

  return (
    <div className="w-full">
      <div className="w-full flex flex-wrap justify-center gap-4 pb-4">
        {routes.map((route, i) => (
          <div
            key={i}
            className="
              flex-shrink-0
              w-full
              sm:w-[360px]
              lg:w-[320px]
              xl:w-[300px]
              2xl:w-[320px]
            "
          >
            <RouteCard
              index={i + 1}
              title={route.title}
              description={getPreviewDescription(route.dayDescriptions)}
              imageUrl={route.imageUrl || "/placeholder-route.jpg"}
              distanceMeters={route.distanceMeters}
              durationSeconds={route.durationSeconds}
              tripDays={getTripDays(route.geoJson)}
              routeType={route.routeType}
              routeTypeTitle={route.routeTypeTitle}
              onClick={() => onRouteClick(route)}
            />
          </div>
        ))}
      </div>
    </div>
  );
}
