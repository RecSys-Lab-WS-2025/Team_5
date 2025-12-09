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
        <div className="w-full overflow-x-auto pb-4 -mx-4 px-4 scroll-smooth no-scrollbar snap-x snap-mandatory flex gap-4">
            {routes.map((route, i) => (
                <div key={i} className="snap-center flex-shrink-0 w-[280px] sm:w-[320px]">
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
    );
}
