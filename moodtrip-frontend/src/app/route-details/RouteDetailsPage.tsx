import { useLocation, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { MoveLeft, Navigation } from "lucide-react";
import { RecommendedRouteMap } from "@/components/map/recommended-route";
import { useState } from "react";
import { cn } from "@/lib/utils";
import type { RouteRecommendation, PoiFeature, RouteFeature } from "@/api/conversation";

export const RouteDetailsPage = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const routeData = location.state as RouteRecommendation;

    const [selectedDay, setSelectedDay] = useState(1);

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

    // Extract POIs and route info from GeoJSON
    const allFeatures = routeData.geoJson?.features || [];
    const pois = allFeatures.filter((f): f is PoiFeature => f.properties?.type === "poi");
    const routeFeature = allFeatures.find((f): f is RouteFeature => f.properties?.type === "route");
    const totalDays = routeFeature?.properties?.tripDays || 1;
    const dailyStats = routeFeature?.properties?.dailyStats || [];

    // Filter POIs for the selected day
    const dayPois = pois.filter((poi) => poi.properties.day === selectedDay);
    const selectedDayStats = dailyStats.find(s => s.day === selectedDay);

    const displayDistance = selectedDayStats?.distanceMeters ?? routeData.distanceMeters ?? 0;
    const displayDuration = selectedDayStats?.durationSeconds ?? routeData.durationSeconds ?? 0;

    const formatDuration = (seconds: number) => {
        const hours = Math.floor(seconds / 3600);
        const mins = Math.round((seconds % 3600) / 60);
        if (hours === 0) return `${mins} min`;
        return `${hours} h ${mins} min`;
    };

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
                    <div className="h-64 w-full overflow-hidden rounded-lg border bg-muted">
                        {routeData.geoJson ? (
                            <RecommendedRouteMap data={routeData.geoJson} />
                        ) : (
                            <div className="flex h-full items-center justify-center text-muted-foreground">
                                Map Preview
                            </div>
                        )}
                    </div>

                    {/* Day Selection */}
                    {totalDays > 1 && (
                        <div className="flex gap-3 overflow-x-auto py-2 pb-6 -mx-4 px-4 scrollbar-hide items-center">
                            {Array.from({ length: totalDays }, (_, i) => i + 1).map((day) => (
                                <Button
                                    key={day}
                                    variant="outline"
                                    size="sm"
                                    onClick={() => setSelectedDay(day)}
                                    className={cn(
                                        "px-7 py-2 h-auto rounded-full transition-all duration-200 border text-sm flex-shrink-0",
                                        selectedDay === day
                                            ? "!bg-slate-900 !text-white !border-slate-900 font-bold shadow-md scale-105"
                                            : "bg-slate-100 text-slate-500 border-slate-200 hover:bg-slate-200"
                                    )}
                                >
                                    Day {day}
                                </Button>
                            ))}
                        </div>
                    )}

                    {/* Stats */}
                    <div className="grid grid-cols-2 gap-4">
                        <div className="rounded-xl border bg-card p-4 shadow-sm transition-all hover:shadow-md">
                            <div className="text-xs font-medium text-muted-foreground uppercase tracking-wider mb-1">
                                {totalDays > 1 ? `Day ${selectedDay} Distance` : 'Distance'}
                            </div>
                            <div className="text-2xl font-bold text-slate-900">
                                {(displayDistance / 1000).toFixed(1)} <span className="text-sm font-normal text-muted-foreground">km</span>
                            </div>
                        </div>
                        <div className="rounded-xl border bg-card p-4 shadow-sm transition-all hover:shadow-md">
                            <div className="text-xs font-medium text-muted-foreground uppercase tracking-wider mb-1">
                                {totalDays > 1 ? `Day ${selectedDay} Duration` : 'Duration'}
                            </div>
                            <div className="text-2xl font-bold text-slate-900">
                                {formatDuration(displayDuration)}
                            </div>
                        </div>
                    </div>

                    {/* Daily Summary Placeholder */}
                    <div className="rounded-xl border bg-slate-50/50 p-4 border-dashed border-slate-300">
                        <h3 className="text-sm font-semibold text-slate-900 mb-2 flex items-center gap-2">
                            <span className="h-2 w-2 rounded-full bg-slate-900"></span>
                            Day {selectedDay} Summary
                        </h3>
                        <p className="text-sm text-slate-500 italic leading-relaxed">
                            Generating AI summary for your day {selectedDay} journey... This space will feature a personalized overview of your highlights and travel mood.
                        </p>
                    </div>

                    {/* Itinerary */}
                    <div>
                        <h3 className="mb-4 font-semibold">Itinerary - Day {selectedDay}</h3>
                        <div className="relative border-l-2 border-muted pl-6 ml-3 space-y-8">
                            {dayPois.length > 0 ? (
                                dayPois.map((poi, i) => (
                                    <div key={i} className="relative">
                                        <div className="absolute -left-[31px] top-1 flex h-6 w-6 items-center justify-center rounded-full border-2 border-background bg-primary text-[10px] font-bold text-primary-foreground shadow-sm">
                                            {i + 1}
                                        </div>
                                        <div className="flex flex-col gap-1">
                                            <div className="font-bold text-foreground leading-none">{poi.properties.displayName || poi.properties.name}</div>
                                            <div className="text-xs font-medium text-muted-foreground uppercase tracking-wider">{poi.properties.category}</div>
                                            {poi.properties.description && (
                                                <p className="mt-1 text-sm text-muted-foreground line-clamp-2">
                                                    {poi.properties.description}
                                                </p>
                                            )}
                                        </div>
                                    </div>
                                ))
                            ) : (
                                <div className="text-sm text-muted-foreground italic">No specific spots planned for this day.</div>
                            )}
                        </div>
                    </div>
                </div>
            </div>

            {/* Footer Action */}
            <div className="border-t p-4 bg-background">
                <Button
                    className="w-full gap-2 shadow-sm bg-slate-900 hover:bg-slate-800 text-white rounded-xl py-6"
                    size="lg"
                >
                    <Navigation className="h-4 w-4" />
                    Start Navigation
                </Button>
            </div>
        </div>
    );
};
