import * as React from "react";
import type { PoiFeature, RouteRecommendation } from "@/api/conversation";
import { Button } from "@/components/ui/button";
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
} from "@/components/ui/sidebar";
import { Navigation } from "lucide-react";
import { cn } from "@/lib/utils";

import {
  Shrub,
  Landmark,
  Kayak,
  Bubbles,
  Utensils,
  ShoppingCart,
} from "lucide-react";

const CATEGORY_ICON: Record<
  "NATURE" | "HISTORY_AND_CULTURE" | "ADVENTURE" | "RELAXATION" | "FOOD_AND_CULINARY" | "SHOPPING",
  React.ComponentType<{ className?: string }>
> = {
  NATURE: Shrub,
  HISTORY_AND_CULTURE: Landmark,
  ADVENTURE: Kayak,
  RELAXATION: Bubbles,
  FOOD_AND_CULINARY: Utensils,
  SHOPPING: ShoppingCart,
};

function CategoryIcon({ category }: { category?: string }) {
  const Icon =
    (category && (CATEGORY_ICON as Record<string, any>)[category]) ?? Landmark;

  return <Icon className="h-4 w-4 text-foreground/80" />;
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
}) {
  return (
    <Sidebar
      {...props}
      className={cn(
        "flex h-full flex-col transition-[width] duration-400 ease-in-out",
        "bg-background/55 backdrop-blur-xl supports-[backdrop-filter]:bg-background/35",
        "border-l border-white/10 shadow-2xl shadow-black/10",
        "[&_[data-sidebar=rail]]:hidden"
      )}
    >
      <SidebarContent className="relative flex-1 gap-0 overflow-y-auto px-5 py-4 pb-28">
        {totalDays > 1 && (
          <SidebarGroup>
            <SidebarGroupLabel className="px-0 text-[11px] font-semibold tracking-wider text-muted-foreground/80 uppercase">
              Days
            </SidebarGroupLabel>

            <SidebarGroupContent className="px-0">
              <div className="flex gap-2">
                {Array.from({ length: totalDays }, (_, i) => i + 1).map((day) => {
                  const active = selectedDay === day;

                  return (
                    <Button
                      key={day}
                      variant="ghost"
                      size="sm"
                      onClick={() => setSelectedDay(day)}
                      className={cn(
                        "h-9 rounded-full px-5 text-sm font-medium",
                        "backdrop-blur-md transition-colors duration-200",
                        active
                          ? "bg-slate-900/80 text-black shadow-sm shadow-black/15"
                          : "bg-white/10 text-slate-800 hover:bg-white/20 hover:text-slate-900"
                      )}
                    >
                      Day {day}
                    </Button>
                  );
                })}
              </div>
            </SidebarGroupContent>
          </SidebarGroup>
        )}

        <SidebarGroup>
          <SidebarGroupLabel className="px-0 text-[11px] font-semibold tracking-wider text-muted-foreground/90 uppercase">
            Stats
          </SidebarGroupLabel>
          <SidebarGroupContent className="px-0">
            <div className="grid grid-cols-2 gap-3">
              <div className="rounded-2xl border border-white/10 bg-white/10 p-4 backdrop-blur-md shadow-sm shadow-black/5">
                <div className="text-[10px] font-semibold text-muted-foreground/80 uppercase tracking-wider mb-1">
                  {totalDays > 1 ? `Day ${selectedDay} Distance` : "Distance"}
                </div>
                <div className="text-2xl font-semibold text-foreground">
                  {(displayDistance / 1000).toFixed(1)}{" "}
                  <span className="text-sm font-normal text-muted-foreground/80">
                    km
                  </span>
                </div>
              </div>

              <div className="rounded-2xl border border-white/10 bg-white/10 p-4 backdrop-blur-md shadow-sm shadow-black/5">
                <div className="text-[10px] font-semibold text-muted-foreground/80 uppercase tracking-wider mb-1">
                  {totalDays > 1 ? `Day ${selectedDay} Duration` : "Duration"}
                </div>
                <div className="text-2xl font-semibold text-foreground">
                  {formatDuration(displayDuration)}
                </div>
              </div>
            </div>
          </SidebarGroupContent>
        </SidebarGroup>

        {routeData.description && (
          <SidebarGroup>
            <SidebarGroupLabel className="px-0 text-[11px] font-semibold tracking-wider text-muted-foreground/90 uppercase">
              Description
            </SidebarGroupLabel>
            <SidebarGroupContent className="px-0">
              <div className="rounded-2xl border border-white/10 bg-white/10 p-4 backdrop-blur-md shadow-sm shadow-black/5">
                <p className="text-sm leading-relaxed text-foreground/80">
                  {routeData.description}
                </p>
              </div>
            </SidebarGroupContent>
          </SidebarGroup>
        )}

        <SidebarGroup>
          <SidebarGroupLabel className="px-0 text-[11px] font-semibold tracking-wider text-muted-foreground/90 uppercase">
            Itinerary - Day {selectedDay}
          </SidebarGroupLabel>
          <SidebarGroupContent className="px-0">
            <div className="relative border-l border-white/15 pl-6 ml-3 space-y-8">
              {dayPois.length > 0 ? (
                dayPois.map((poi, i) => (
                  <div key={i} className="relative">
                    <div className="absolute -left-[40px] top-1 flex h-7 w-7 items-center justify-center rounded-full border border-white/15 bg-white/10 backdrop-blur-md shadow-sm shadow-black/5">
                        <div className="-translate-x-[1px] -translate-y-[4px]">
                            <CategoryIcon category={poi.properties.category} />
                        </div>
                    </div>

                    <div className="flex flex-col gap-1">
                      <div className="font-semibold text-foreground leading-snug">
                        {poi.properties.displayName || poi.properties.name}
                      </div>
                      <div className="text-[11px] font-semibold text-muted-foreground/80 uppercase tracking-wider">
                        {poi.properties.category}
                      </div>
                      {poi.properties.description && (
                        <p className="mt-1 text-sm text-foreground/70 line-clamp-2">
                          {poi.properties.description}
                        </p>
                      )}
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-sm text-muted-foreground/80 italic">
                  No specific spots planned for this day.
                </div>
              )}
            </div>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>

      <div className="shrink-0 border-t border-white/10 bg-background/55 supports-[backdrop-filter]:bg-background/35 backdrop-blur-xl px-5 pt-1 pb-8">
        <Button
          size="lg"
          className={cn("w-full rounded-2xl py-5 font-medium", "!bg-blue-600 text-white")}
        >
          <Navigation className="h-4 w-4" />
          Start Navigation
        </Button>
      </div>
    </Sidebar>
  );
}
