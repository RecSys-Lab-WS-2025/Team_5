import * as React from "react"
import type { PoiFeature, RouteRecommendation } from "@/api/conversation"
import { Button } from "@/components/ui/button"
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
} from "@/components/ui/sidebar"
import { Navigation, ChevronDown, ChevronUp } from "lucide-react"
import { cn } from "@/lib/utils"

import { Shrub, Landmark, Kayak, Bubbles, Utensils, ShoppingCart } from "lucide-react"

type CategoryKey =
  | "NATURE"
  | "HISTORY_AND_CULTURE"
  | "ADVENTURE"
  | "RELAXATION"
  | "FOOD_AND_CULINARY"
  | "SHOPPING"

const CATEGORY_ICON: Record<CategoryKey, React.ComponentType<{ className?: string }>> = {
  NATURE: Shrub,
  HISTORY_AND_CULTURE: Landmark,
  ADVENTURE: Kayak,
  RELAXATION: Bubbles,
  FOOD_AND_CULINARY: Utensils,
  SHOPPING: ShoppingCart,
}

function isCategoryKey(v: string): v is CategoryKey {
  return v in CATEGORY_ICON
}

function CategoryIcon({ category }: { category?: string }) {
  const Icon: React.ComponentType<{ className?: string }> =
    category && isCategoryKey(category) ? CATEGORY_ICON[category] : Landmark

  return <Icon className="h-4 w-4 text-foreground/80" />
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
  routeData: RouteRecommendation
  totalDays: number
  selectedDay: number
  setSelectedDay: (v: number) => void
  displayDistance: number
  displayDuration: number
  formatDuration: (s: number) => string
  dayPois: PoiFeature[]
}) {
  const [isDescriptionExpanded, setIsDescriptionExpanded] = React.useState(false)

  const getPoiCoords = React.useCallback((poi: PoiFeature) => {
    const coords = poi.geometry?.coordinates
    if (!coords || coords.length < 2) return null
    const [lon, lat] = coords
    return { lat, lon }
  }, [])

  const navigationPoints = React.useMemo(
    () => dayPois.map(getPoiCoords).filter(Boolean) as Array<{ lat: number; lon: number }>,
    [dayPois, getPoiCoords]
  )

  const canNavigate = navigationPoints.length >= 2

  const handleStartNavigation = () => {
    if (!canNavigate) return
    const origin = navigationPoints[0]
    const destination = navigationPoints[navigationPoints.length - 1]
    const waypoints = navigationPoints.slice(1, -1)

    const params = new URLSearchParams()
    params.set("api", "1")
    params.set("origin", `${origin.lat},${origin.lon}`)
    params.set("destination", `${destination.lat},${destination.lon}`)
    if (waypoints.length > 0) {
      params.set(
        "waypoints",
        waypoints.map((p) => `${p.lat},${p.lon}`).join("|")
      )
    }
    params.set("travelmode", "walking")

    const url = `https://www.google.com/maps/dir/?${params.toString()}`
    window.open(url, "_blank", "noopener,noreferrer")
  }

  return (
    <Sidebar
      {...props}
      className={cn(
        "flex h-full flex-col transition-[width] duration-400 ease-in-out",
        "bg-white/70 backdrop-blur-xl border-l border-gray-200/50",
        "[&_[data-sidebar=rail]]:hidden",
      )}
    >
      <SidebarContent className="relative flex-1 gap-0 overflow-y-auto px-6 py-6 pb-28">
        <div className="!mb-6 !pb-6 !border-b !border-gray-200/50">
          <h1 className="!text-2xl !font-bold !text-gray-900 !leading-tight !tracking-tight">
            {routeData.title}
          </h1>
        </div>

        {totalDays > 1 && (
          <SidebarGroup className="mb-6">
            <SidebarGroupContent className="px-0">
              <div className="flex gap-2">
                {Array.from({ length: totalDays }, (_, i) => i + 1).map((day) => {
                  const active = selectedDay === day
                  return (
                    <Button
                      key={day}
                      variant="ghost"
                      size="sm"
                      onClick={() => setSelectedDay(day)}
                      className={cn(
                        "h-8 rounded-lg px-4 text-sm font-medium transition-colors",
                        active ? "!bg-gray-900 !text-white" : "!bg-gray-100/80 !text-gray-700",
                      )}
                    >
                      Day {day}
                    </Button>
                  )
                })}
              </div>
            </SidebarGroupContent>
          </SidebarGroup>
        )}

        <SidebarGroup className="mb-6">
          <SidebarGroupContent className="px-0">
            <div className="grid grid-cols-2 gap-3">
              <div className="rounded-lg border border-gray-200/50 bg-white/60 backdrop-blur-sm p-4">
                <div className="text-xs font-medium text-gray-500 mb-1.5">
                  {totalDays > 1 ? `Day ${selectedDay} Distance` : "Distance"}
                </div>
                <div className="text-xl font-medium text-gray-900">
                  {(displayDistance / 1000).toFixed(1)}{" "}
                  <span className="text-sm font-normal text-gray-500">km</span>
                </div>
              </div>

              <div className="rounded-lg border border-gray-200/50 bg-white/60 backdrop-blur-sm p-4">
                <div className="text-xs font-medium text-gray-500 mb-1.5">
                  {totalDays > 1 ? `Day ${selectedDay} Duration` : "Duration"}
                </div>
                <div className="text-xl font-medium text-gray-900">{formatDuration(displayDuration)}</div>
              </div>
            </div>
          </SidebarGroupContent>
        </SidebarGroup>

        {routeData.dayDescriptions &&
          (() => {
            const dayDescription = routeData.dayDescriptions[String(selectedDay)] ||
              Object.values(routeData.dayDescriptions)[0] ||
              "Explore and enjoy the planned activities."
            const shouldCollapse = dayDescription.length > 150
            const displayText =
              shouldCollapse && !isDescriptionExpanded ? dayDescription.substring(0, 150) + "..." : dayDescription

            return (
              <SidebarGroup className="mb-6">
                <SidebarGroupContent className="px-0">
                  <div className="rounded-lg border border-gray-200/50 bg-white/60 backdrop-blur-sm overflow-hidden">
                    {shouldCollapse ? (
                      <>
                        <button
                          type="button"
                          onClick={() => setIsDescriptionExpanded((v) => !v)}
                          className="w-full flex items-center justify-between p-4 pb-3 text-left"
                        >
                          <span className="text-sm font-medium text-gray-700">
                            {totalDays > 1 ? `Day ${selectedDay} Description` : "Description"}
                          </span>
                          {isDescriptionExpanded ? (
                            <ChevronUp className="h-4 w-4 text-gray-500" />
                          ) : (
                            <ChevronDown className="h-4 w-4 text-gray-500" />
                          )}
                        </button>
                        <div className="px-4 pt-1 pb-4">
                          <p className="text-sm leading-relaxed text-gray-600">{displayText}</p>
                        </div>
                      </>
                    ) : (
                      <div className="p-4">
                        <div className="text-sm font-medium text-gray-700 mb-3">
                          {totalDays > 1 ? `Day ${selectedDay} Description` : "Description"}
                        </div>
                        <p className="text-sm leading-relaxed text-gray-600">{dayDescription}</p>
                      </div>
                    )}
                  </div>
                </SidebarGroupContent>
              </SidebarGroup>
            )
          })()}

        <SidebarGroup>
          <SidebarGroupLabel className="px-0 text-xs font-medium text-gray-500 mb-4">
            Itinerary - Day {selectedDay}
          </SidebarGroupLabel>
          <SidebarGroupContent className="px-0">
            <div className="relative border-l border-gray-200/50 pl-7 ml-[24px] space-y-6">
              {dayPois.length > 0 ? (
                dayPois.map((poi, i) => (
                  <div key={i} className="relative">
                    <div className="absolute -left-[40px] top-1 flex h-6 w-6 items-center justify-center rounded-full border border-gray-200/50 bg-white/80 backdrop-blur-sm">
                      <CategoryIcon category={poi.properties.category} />
                    </div>

                    <div className="flex flex-col gap-1">
                      <div className="font-medium text-gray-900 leading-snug">
                        {poi.properties.displayName || poi.properties.name}
                      </div>
                      <div className="text-xs font-medium text-gray-500 uppercase tracking-wide">
                        {poi.properties.category}
                      </div>
                      {poi.properties.description && (
                        <p className="mt-1.5 text-sm text-gray-600 leading-relaxed line-clamp-2">
                          {poi.properties.description}
                        </p>
                      )}
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-sm text-gray-400 italic">No specific spots planned for this day.</div>
              )}
            </div>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>

      <div className="shrink-0 border-t border-gray-200/50 bg-white/70 backdrop-blur-xl px-6 pt-4 pb-6">
        <Button
          size="lg"
          onClick={handleStartNavigation}
          disabled={!canNavigate}
          className={cn("w-full rounded-lg py-6 font-medium !bg-gray-900 !text-white")}
        >
          <Navigation className="h-4 w-4 mr-2" />
          Start Navigation
        </Button>
      </div>
    </Sidebar>
  )
}
