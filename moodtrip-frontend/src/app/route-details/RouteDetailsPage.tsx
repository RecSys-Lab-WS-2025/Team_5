"use client";

import * as React from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { MoveLeft, ArrowLeft } from "lucide-react";
import { cn } from "@/lib/utils";

import { Button } from "@/components/ui/button";
import {
  SidebarInset,
  SidebarProvider,
  SidebarTrigger,
  useSidebar,
} from "@/components/ui/sidebar";

import { RecommendedRouteMap } from "@/components/map/recommended-route";
import { AppSidebar } from "@/components/route-details/app-sidebar";

import type { FeatureCollection } from "geojson";
import type { RouteRecommendation, PoiFeature, RouteFeature } from "@/api/conversation";

function MapPane({
  geoJson,
  selectedDay,
}: {
  geoJson: FeatureCollection;
  selectedDay: number;
}) {
  const { isMobile, openMobile, state } = useSidebar();
  const invalidateKey = isMobile ? (openMobile ? "open" : "closed") : state;

  return (
    <div className="h-full w-full min-w-0">
      <RecommendedRouteMap
        data={geoJson}
        invalidateKey={invalidateKey}
        selectedDay={selectedDay}
      />
    </div>
  );
}

type RouteDetailsState = RouteRecommendation | null;

export const RouteDetailsPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const routeData = (location.state as RouteDetailsState) ?? null;

  const [selectedDay, setSelectedDay] = React.useState(1);

  if (!routeData) {
    return (
      <div className="flex h-screen items-center justify-center bg-white p-4">
        <div className="text-center space-y-4">
          <h2 className="text-xl font-semibold text-foreground">Route not found</h2>
          <Button
            onClick={() => navigate(-1)}
            className={cn(
              "inline-flex h-10 w-10 items-center justify-center rounded-full",
              "!bg-white text-foreground",
              "border border-black/10",
              "shadow-sm shadow-black/10",
              "hover:bg-white hover:border-black/20",
              "transition-colors"
            )}
          >
            <ArrowLeft className="h-5 w-5" />
          </Button>
        </div>
      </div>
    );
  }

  const allFeatures = routeData.geoJson?.features || [];
  const pois = allFeatures.filter((f): f is PoiFeature => f.properties?.type === "poi");
  const routeFeature = allFeatures.find((f): f is RouteFeature => f.properties?.type === "route");

  const totalDays = routeFeature?.properties?.tripDays || 1;
  const dailyStats = routeFeature?.properties?.dailyStats || [];

  const dayPois = pois.filter((poi) => poi.properties.day === selectedDay);
  const selectedDayStats = dailyStats.find((s) => s.day === selectedDay);

  const displayDistance = selectedDayStats?.distanceMeters ?? routeData.distanceMeters ?? 0;
  const displayDuration = selectedDayStats?.durationSeconds ?? routeData.durationSeconds ?? 0;

  const formatDuration = (seconds: number) => {
    const hours = Math.floor(seconds / 3600);
    const mins = Math.round((seconds % 3600) / 60);
    if (hours === 0) return `${mins} min`;
    return `${hours} h ${mins} min`;
  };

  return (
    <SidebarProvider
      defaultOpen
      style={
        {
          "--sidebar-width": "32rem",
          "--sidebar-width-icon": "3rem",
        } as React.CSSProperties
      }
      className="h-screen"
    >
      <SidebarInset className="h-screen min-h-0 w-full overflow-hidden bg-gradient-to-br from-gray-50 to-white">
        <header className="relative z-20 flex h-16 shrink-0 items-center gap-3 border-b border-gray-200/50 bg-white/80 backdrop-blur-xl px-4">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => navigate(-1)}
            className="!bg-white text-gray-600 hover:text-gray-900 hover:!bg-white"
          >
            <MoveLeft className="h-5 w-5" />
          </Button>

          <SidebarTrigger className="ml-auto rotate-180 text-gray-600 hover:text-gray-900" />
        </header>

        <main className="h-[calc(100vh-4rem)] min-h-0 w-full">
          {routeData.geoJson ? (
            <MapPane geoJson={routeData.geoJson} selectedDay={selectedDay} />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-gray-400">
              Map Preview
            </div>
          )}
        </main>
      </SidebarInset>

      <AppSidebar
        side="right"
        routeData={routeData}
        totalDays={totalDays}
        selectedDay={selectedDay}
        setSelectedDay={setSelectedDay}
        displayDistance={displayDistance}
        displayDuration={displayDuration}
        formatDuration={formatDuration}
        dayPois={dayPois}
      />
    </SidebarProvider>
  );
};
