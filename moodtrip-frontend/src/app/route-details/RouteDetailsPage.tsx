import { useLocation, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { MoveLeft, Navigation } from "lucide-react";
import { RecommendedRouteMap } from "@/components/map/recommended-route";

export const RouteDetailsPage = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const routeData = location.state;

    // TODO: Ideally we should fetch route details by ID if state is missing
    // or if we want to ensure fresh data. 
    // const { id } = useParams();

    if (!routeData) {
        return (
            <div className="flex h-screen items-center justify-center p-4">
                <div className="text-center">
                    <h2 className="text-xl font-bold">Route not found</h2>
                    <Button variant="link" onClick={() => navigate(-1)}>
                        Go back
                    </Button>
                </div>
            </div>
        );
    }

    // TODO: Fetch real itinerary steps from backend based on the route POIs or segments
    // Mock Itinerary Steps
    const steps = [
        { instruction: "Start at Marienplatz", distance: "0 m" },
        { instruction: "Walk towards Viktualienmarkt", distance: "350 m" },
        { instruction: "Turn left onto Tal", distance: "100 m" },
        { instruction: "Arrive at destination", distance: "0 m" },
    ];

    return (
        <div className="flex h-screen flex-col bg-background">
            {/* Header */}
            <header className="flex items-center gap-4 border-b p-4">
                <Button variant="ghost" size="icon" onClick={() => navigate(-1)}>
                    <MoveLeft className="h-5 w-5" />
                </Button>
                <h1 className="text-lg font-semibold flex-1 truncate">
                    {routeData.title}
                </h1>
            </header>

            {/* Content */}
            <div className="flex-1 overflow-y-auto">
                <div className="p-4 space-y-6">
                    {/* Map Section */}
                    {/* TODO: Ensure the map respects the route geometry passed */}
                    <div className="h-64 w-full overflow-hidden rounded-lg border bg-muted">
                        {routeData.geoJson ? (
                            <RecommendedRouteMap data={routeData.geoJson} />
                        ) : (
                            <div className="flex h-full items-center justify-center text-muted-foreground">
                                Map Preview
                            </div>
                        )}
                    </div>

                    {/* Stats */}
                    <div className="grid grid-cols-2 gap-4">
                        <div className="rounded-lg border p-3">
                            <div className="text-sm text-muted-foreground">Distance</div>
                            <div className="text-xl font-bold">
                                {(routeData.distanceMeters / 1000).toFixed(1)} km
                            </div>
                        </div>
                        <div className="rounded-lg border p-3">
                            <div className="text-sm text-muted-foreground">Duration</div>
                            <div className="text-xl font-bold">
                                {Math.round(routeData.durationSeconds / 60)} min
                            </div>
                        </div>
                    </div>

                    {/* Description */}
                    <div>
                        <h3 className="mb-2 font-semibold">Description</h3>
                        <p className="text-muted-foreground">
                            {routeData.description}
                        </p>
                    </div>

                    {/* Itinerary - Mock Data */}
                    <div>
                        <h3 className="mb-2 font-semibold">Itinerary (Mock)</h3>
                        <div className="relative border-l-2 border-muted pl-4 ml-2 space-y-6">
                            {steps.map((step, i) => (
                                <div key={i} className="relative">
                                    <div className="absolute -left-[21px] top-1 h-3 w-3 rounded-full bg-primary" />
                                    <div className="font-medium">{step.instruction}</div>
                                    <div className="text-xs text-muted-foreground">{step.distance}</div>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>

            {/* Footer Action */}
            <div className="border-t p-4">
                <Button className="w-full gap-2">
                    <Navigation className="h-4 w-4" />
                    Start Navigation
                </Button>
            </div>
        </div>
    );
};
