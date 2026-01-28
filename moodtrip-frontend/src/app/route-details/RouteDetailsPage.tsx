"use client";

import * as React from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { MoveLeft, ArrowLeft } from "lucide-react";
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
  activePoiId,
}: {
  geoJson: FeatureCollection;
  selectedDay: number;
  activePoiId: string | null;
}) {
  const { isMobile, openMobile, state } = useSidebar();
  const invalidateKey = isMobile ? (openMobile ? "open" : "closed") : state;

  return (
    <div className="h-full w-full min-w-0">
      <RecommendedRouteMap
        data={geoJson}
        invalidateKey={invalidateKey}
        selectedDay={selectedDay}
        activePoiId={activePoiId}
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
  const [activePoiId, setActivePoiId] = React.useState<string | null>(null);

  if (!routeData) {
    return (
      <div className="flex h-screen items-center justify-center bg-white p-4">
        <div className="text-center space-y-4">
          <h2 className="text-xl font-semibold text-slate-900">Route not found</h2>
          <button
            onClick={() => navigate(-1)}
            className="inline-flex items-center justify-center p-2 rounded-full text-slate-400 hover:text-slate-900 transition-colors"
          >
            <ArrowLeft className="h-6 w-6" />
          </button>
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
      className="h-screen w-full"
    >
      <SidebarInset className="relative h-screen min-h-0 w-full overflow-hidden bg-slate-50">
        <header className="relative z-20 flex h-10 shrink-0 items-center justify-between bg-white/70 backdrop-blur-md px-6">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate(-1)}
              className="flex items-center justify-center p-1.5 -ml-1 text-slate-400 hover:text-slate-900 transition-colors"
            >
              <MoveLeft className="h-5 w-5" />
            </button>
          </div>

          <SidebarTrigger className="rotate-180 text-slate-400 hover:text-slate-900 transition-colors" />
        </header>

        <main className="h-[calc(100vh-3.5rem)] min-h-0 w-full">
          {routeData.geoJson ? (
            <MapPane 
              geoJson={routeData.geoJson} 
              selectedDay={selectedDay} 
              activePoiId={activePoiId} 
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-slate-300 text-[10px] font-bold uppercase tracking-widest">
              Initializing Map
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
        onPoiClick={(id) => setActivePoiId(id)}
      />
    </SidebarProvider>
  );
};
