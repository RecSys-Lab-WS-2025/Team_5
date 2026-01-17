import { RouteCard } from "./route-card";
import type { FeatureCollection } from "geojson";

export interface RouteCardData {
    id: string;
    title: string;
    description: string;
    imageUrl: string;
    distanceMeters: number;
    durationSeconds: number;
    geoJson?: FeatureCollection;
}

interface RouteCarouselProps {
    routes: RouteCardData[];
    onRouteClick: (route: RouteCardData) => void;
}

export function RouteCarousel({ routes, onRouteClick }: RouteCarouselProps) {
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
              description={route.description}
              imageUrl={route.imageUrl || "/placeholder-route.jpg"}
              distanceMeters={route.distanceMeters}
              durationSeconds={route.durationSeconds}
              onClick={() => onRouteClick(route)}
            />
          </div>
        ))}
      </div>
    </div>
  );
}
