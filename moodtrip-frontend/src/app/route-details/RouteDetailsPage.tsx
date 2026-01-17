import * as React from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { MoveLeft } from "lucide-react";
import { cn } from "@/lib/utils";
import { ArrowLeft } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  SidebarInset,
  SidebarProvider,
  SidebarTrigger,
  useSidebar,
} from "@/components/ui/sidebar";

import { RecommendedRouteMap } from "@/components/map/recommended-route";
import { AppSidebar } from "@/components/route-details/app-sidebar";

import type {
  RouteRecommendation,
  PoiFeature,
  RouteFeature,
} from "@/api/conversation";

function MapPane({ geoJson }: { geoJson: any }) {
  const { isMobile, openMobile, state } = useSidebar();
  const invalidateKey = isMobile ? (openMobile ? "open" : "closed") : state;

  return (
    <div className="h-full w-full min-w-0">
      <RecommendedRouteMap data={geoJson} invalidateKey={invalidateKey} />
    </div>
  );
}

export const RouteDetailsPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const routeData = location.state as RouteRecommendation;

  const [selectedDay, setSelectedDay] = React.useState(1);

if (!routeData) {
  return (
    <div className="flex h-screen items-center justify-center bg-white p-4">
      <div className="text-center space-y-4">
        <h2 className="text-xl font-semibold text-foreground">
          Route not found
        </h2>

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
  const pois = allFeatures.filter(
    (f): f is PoiFeature => f.properties?.type === "poi"
  );
  const routeFeature = allFeatures.find(
    (f): f is RouteFeature => f.properties?.type === "route"
  );

  const totalDays = routeFeature?.properties?.tripDays || 1;
  const dailyStats = routeFeature?.properties?.dailyStats || [];

  const dayPois = pois.filter((poi) => poi.properties.day === selectedDay);
  const selectedDayStats = dailyStats.find((s) => s.day === selectedDay);

  const displayDistance =
    selectedDayStats?.distanceMeters ?? routeData.distanceMeters ?? 0;
  const displayDuration =
    selectedDayStats?.durationSeconds ?? routeData.durationSeconds ?? 0;

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
      <SidebarInset className="h-screen min-h-0 w-full overflow-hidden !bg-white">
        <header className="relative z-20 flex h-16 shrink-0 items-center gap-3 border-b !bg-white px-4 backdrop-blur">
          <Button variant="ghost" size="icon" onClick={() => navigate(-1)}>
            <MoveLeft className="h-5 w-5" />
          </Button>

          <h1 className="flex-1 truncate !text-3xl font-semibold">
            {routeData.title}
          </h1>

          <SidebarTrigger className="ml-auto rotate-180" />
        </header>

        <main className="h-[calc(100vh-4rem)] min-h-0 w-full">
          {routeData.geoJson ? (
            <MapPane geoJson={routeData.geoJson} />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-muted-foreground">
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
