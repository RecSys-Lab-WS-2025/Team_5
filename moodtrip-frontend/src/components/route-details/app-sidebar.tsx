"use client";

import * as React from "react";
import type { PoiFeature, RouteRecommendation, AppRouteType } from "@/api/conversation";
import { Button } from "@/components/ui/button";
import { Sidebar, SidebarContent, SidebarGroup, SidebarGroupContent } from "@/components/ui/sidebar";
import { Navigation, ChevronDown, ChevronUp } from "lucide-react";
import { cn } from "@/lib/utils";
import { Shrub, Landmark, Kayak, Bubbles, Utensils, ShoppingCart } from "lucide-react";

type CategoryKey =
  | "NATURE"
  | "HISTORY_AND_CULTURE"
  | "ADVENTURE"
  | "RELAXATION"
  | "FOOD_AND_CULINARY"
  | "SHOPPING";

const CATEGORY_ICON: Record<CategoryKey, React.ComponentType<{ className?: string }>> = {
  NATURE: Shrub,
  HISTORY_AND_CULTURE: Landmark,
  ADVENTURE: Kayak,
  RELAXATION: Bubbles,
  FOOD_AND_CULINARY: Utensils,
  SHOPPING: ShoppingCart,
};

function isCategoryKey(v: string): v is CategoryKey {
  return v in CATEGORY_ICON;
}

function CategoryIcon({ category }: { category?: string }) {
  const Icon = category && isCategoryKey(category) ? CATEGORY_ICON[category] : Landmark;
  return <Icon className="h-4 w-4 text-slate-400 group-hover:text-slate-600 transition-colors duration-300" />;
}

const routeTypeColors: Record<AppRouteType, string> = {
  BALANCED: "bg-emerald-400",
  YOUR_PICKS: "bg-sky-400",
  DISCOVERY: "bg-indigo-400",
};

function splitTitleAndBadge(title?: string, routeTypeTitle?: string) {
  const raw = (title ?? "").trim();
  if (!raw) return { badgeText: routeTypeTitle ?? null, pureTitle: "" };
  if (routeTypeTitle) {
    const prefix = `${routeTypeTitle}:`;
    if (raw.toLowerCase().startsWith(prefix.toLowerCase())) {
      return { badgeText: routeTypeTitle, pureTitle: raw.slice(prefix.length).trim() };
    }
    return { badgeText: routeTypeTitle, pureTitle: raw };
  }
  const m = raw.match(/^\s*(Balanced Route|Your Picks|Discovery)\s*:\s*(.+)\s*$/i);
  if (m?.[2]) return { badgeText: m[1], pureTitle: m[2].trim() };
  return { badgeText: null, pureTitle: raw };
}

function inferRouteTypeFromBadgeText(badgeText?: string | null): AppRouteType | null {
  const t = (badgeText ?? "").toLowerCase();
  if (!t) return null;
  if (t.includes("balanced")) return "BALANCED";
  if (t.includes("your picks") || t.includes("picks")) return "YOUR_PICKS";
  if (t.includes("discovery")) return "DISCOVERY";
  return null;
}

export function AppSidebar({
  routeData,
  totalDays,
  selectedDay,
  setSelectedDay,
  displayDistance,
  displayDuration,
  formatDuration,
  dayPois,
  onPoiClick,
  ...props
}: React.ComponentProps<typeof Sidebar> & {
  routeData: RouteRecommendation;
  totalDays: number;
  selectedDay: number;
  setSelectedDay: (v: number) => void;
  displayDistance: number;
  displayDuration: number;
  formatDuration: (s: number) => string;
  dayPois: PoiFeature[];
  onPoiClick?: (poiId: string) => void;
}) {
  const [isDescriptionExpanded, setIsDescriptionExpanded] = React.useState(false);

  const getPoiCoords = React.useCallback((poi: PoiFeature) => {
    const coords = poi.geometry?.coordinates;
    if (!coords || coords.length < 2) return null;
    return { lat: coords[1], lon: coords[0] };
  }, []);

  const navigationPoints = React.useMemo(
    () => dayPois.map(getPoiCoords).filter(Boolean) as Array<{ lat: number; lon: number }>,
    [dayPois, getPoiCoords]
  );

  const canNavigate = navigationPoints.length >= 2;

  const handleStartNavigation = () => {
    if (!canNavigate) return;
    const destination = navigationPoints[navigationPoints.length - 1];
    const params = new URLSearchParams();
    params.set("api", "1");
    params.set("travelmode", "walking");
    params.set("origin", "Current+Location");
    params.set("destination", `${destination.lat},${destination.lon}`);
    const waypointsArr = navigationPoints.slice(0, -1);
    if (waypointsArr.length > 0) {
      params.set("waypoints", waypointsArr.map((p) => `${p.lat},${p.lon}`).join("|"));
    }
    window.open(`https://www.google.com/maps/dir/?${params.toString()}`, "_blank", "noopener,noreferrer");
  };

  const { badgeText, pureTitle } = splitTitleAndBadge(routeData.title, routeData.routeTypeTitle);
  const computedRouteType =
    routeData.routeType ?? inferRouteTypeFromBadgeText(routeData.routeTypeTitle ?? badgeText);
  const badgeLabel = routeData.routeTypeTitle ?? badgeText;
  const titleLabel = pureTitle || routeData.title || "";

  return (
    <Sidebar {...props} className="border-none bg-white/80 backdrop-blur-2xl">
      <SidebarContent className="sidebar-custom-scroll relative flex-1 overflow-y-auto px-5 py-4 pb-20">
        <div className="sticky top-0 z-20 -mx-5 mb-2 bg-white/40 px-5 pb-3 pt-1 backdrop-blur-xl">
          {badgeLabel && (
            <div
              className={cn(
                "mb-2 inline-flex rounded-full px-2 py-0.5 text-[9px] font-bold uppercase tracking-wider text-white",
                computedRouteType ? routeTypeColors[computedRouteType] : "bg-slate-400"
              )}
            >
              {badgeLabel}
            </div>
          )}

          <h1 className="text-2xl font-bold tracking-tight text-slate-800 leading-tight">
            {titleLabel}
          </h1>

          <div className="mt-1 text-[10px] font-bold uppercase tracking-widest text-slate-400">
            {totalDays > 1 ? `${totalDays} Days Trip` : "One Day Trip"}
          </div>

          {totalDays > 1 && (
            <div className="mt-4 flex flex-wrap gap-1.5">
              {Array.from({ length: totalDays }, (_, i) => i + 1).map((day) => (
                <button
                  key={day}
                  onClick={() => setSelectedDay(day)}
                  className={cn(
                    "px-4 py-1.5 rounded-full text-xs font-bold tracking-tight transition-all duration-300",
                    selectedDay === day
                      ? "bg-slate-500 text-white shadow-sm shadow-slate-200"
                      : "bg-slate-100/50 text-slate-400 hover:bg-slate-200 hover:text-slate-600"
                  )}
                >
                  Day {day}
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="grid grid-cols-2 gap-4 border-y border-slate-100/60 py-4">
          <div className="flex flex-col">
            <span className="text-[9px] font-bold uppercase tracking-widest text-slate-400 mb-0.5">Distance</span>
            <div className="flex items-baseline gap-0.5">
              <span className="text-xl font-bold tabular-nums text-slate-700">{(displayDistance / 1000).toFixed(1)}</span>
              <span className="text-[10px] font-medium text-slate-400">km</span>
            </div>
          </div>
          <div className="flex flex-col">
            <span className="text-[9px] font-bold uppercase tracking-widest text-slate-400 mb-0.5">Duration</span>
            <div className="text-xl font-bold text-slate-700">{formatDuration(displayDuration)}</div>
          </div>
        </div>

        {routeData.dayDescriptions && (
          <div className="py-4 border-b border-slate-100/60">
            <div className="mb-1.5 flex items-center justify-between">
              <span className="text-[9px] font-bold uppercase tracking-widest text-slate-400">Insights</span>
              {routeData.dayDescriptions[String(selectedDay)]?.length > 150 && (
                <button onClick={() => setIsDescriptionExpanded((v) => !v)} className="p-1 hover:bg-slate-100 rounded-md">
                  {isDescriptionExpanded ? <ChevronUp className="h-3.5 w-3.5 text-slate-400" /> : <ChevronDown className="h-3.5 w-3.5 text-slate-400" />}
                </button>
              )}
            </div>
            <p className={cn("text-[13px] leading-relaxed text-slate-500 font-medium", !isDescriptionExpanded && "line-clamp-2")}>
              {routeData.dayDescriptions[String(selectedDay)] || Object.values(routeData.dayDescriptions)[0]}
            </p>
          </div>
        )}

        <SidebarGroup className="pt-5 px-0">
          <SidebarGroupContent>
            <div className="mb-4 text-[9px] font-bold uppercase tracking-widest text-slate-400 px-1">Itinerary</div>
            <div className="relative space-y-1">
              {dayPois.length > 0 ? (
                dayPois.map((poi, i) => {
                  const currentPoiId = poi.properties.osmId?.toString() || `poi-${i}`;
                  return (
                    <div
                      key={i}
                      onClick={() => onPoiClick?.(currentPoiId)}
                      className="group relative flex items-start gap-4 p-3 rounded-2xl border border-transparent hover:bg-white hover:shadow-[0_2px_15px_-3px_rgba(0,0,0,0.03)] transition-all duration-300 cursor-pointer active:scale-[0.99]"
                    >
                      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-slate-50 border border-slate-100 group-hover:bg-white group-hover:border-slate-200 transition-colors duration-300">
                        <CategoryIcon category={poi.properties.category} />
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="text-[8px] font-bold uppercase tracking-widest text-slate-400 group-hover:text-slate-600 transition-colors">
                          {poi.properties.category?.replace(/_/g, " ")}
                        </div>
                        <div className="mt-0.5 text-[14px] font-bold text-slate-700">
                          {poi.properties.displayName || poi.properties.name}
                        </div>
                        {poi.properties.description && (
                          <div className="mt-1 text-[12px] leading-snug text-slate-400 line-clamp-1 font-medium italic">
                            {poi.properties.description}
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })
              ) : (
                <div className="text-xs text-slate-400 italic px-2">No scheduled stops.</div>
              )}
            </div>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>

      <div className="sticky bottom-4 z-30 bg-white/60 p-5 backdrop-blur-xl border-t border-slate-100/60">
        <Button
          size="lg"
          onClick={handleStartNavigation}
          disabled={!canNavigate}
          className="h-11 w-full rounded-xl bg-slate-800 text-sm font-semibold text-slate-50 shadow-md shadow-slate-200/50 hover:bg-slate-900 active:scale-[0.98] transition-all border border-slate-700/50"
        >
          <Navigation className="mr-2 h-4 w-4 fill-white/10" />
          Start Navigation
        </Button>
      </div>
    </Sidebar>
  );
}