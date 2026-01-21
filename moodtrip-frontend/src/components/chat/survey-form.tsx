import * as React from "react";
import { Loader2, LocateFixed, MapPin } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

import {
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from "@/components/ui/tooltip";

import { Slider } from "@/components/ui/slider";
import { POI_CATEGORIES, normalizePoiCategories } from "@/lib/poi-categories";

import type { SurveyData } from "@/api/conversation";

type LocationSuggestion = {
    place_id: number;
    display_name: string;
    lat: string;
    lon: string;
};

type SelectedLocation = {
    latitude: number;
    longitude: number;
    label: string;
    locationName: string;
};

export function SurveyForm({
    onSubmit,
    readOnly = false,
    initialData,
}: {
    onSubmit?: (data: SurveyData) => Promise<void> | void;
    readOnly?: boolean;
    initialData?: SurveyData;
}) {
    // Helper to get date string YYYY-MM-DD
    const getTodayString = () => new Date().toISOString().split('T')[0];

    const [range, setRange] = React.useState<number>(initialData?.rangeMeters ?? 5000);
    // Local string state for input to allow empty/typing states
    const [rangeInput, setRangeInput] = React.useState<string>((initialData?.rangeMeters ? initialData.rangeMeters / 1000 : 5).toString());

    const [startDate, setStartDate] = React.useState<string>(initialData?.startDate ?? getTodayString());
    const [endDate, setEndDate] = React.useState<string>(initialData?.endDate ?? getTodayString());
    const [selectedCategories, setSelectedCategories] = React.useState<string[]>(
        () => normalizePoiCategories(initialData?.poiCategories)
    );
    const [locationQuery, setLocationQuery] = React.useState<string>("");
    const [selectedLocation, setSelectedLocation] = React.useState<SelectedLocation | null>(null);
    const [locationSuggestions, setLocationSuggestions] = React.useState<LocationSuggestion[]>([]);
    const [isSearchingLocation, setIsSearchingLocation] = React.useState(false);
    const [isFetchingCurrentLocation, setIsFetchingCurrentLocation] = React.useState(false);
    const [locationError, setLocationError] = React.useState<string | null>(null);

    const [isSubmitting, setIsSubmitting] = React.useState(false);
    const [isSubmitted, setIsSubmitted] = React.useState(false);

    // Populate from initial data (read-only view)
    React.useEffect(() => {
        if (!initialData) return;
        const fallbackLabel = `Lat ${initialData.latitude.toFixed(4)}, Lon ${initialData.longitude.toFixed(4)}`;
        const label = initialData.locationName?.trim() ? initialData.locationName : fallbackLabel;
        setSelectedLocation({
            latitude: initialData.latitude,
            longitude: initialData.longitude,
            label,
            locationName: initialData.locationName ?? fallbackLabel,
        });
        setLocationQuery(label);
        if (initialData.rangeMeters) setRange(initialData.rangeMeters);
        setSelectedCategories(normalizePoiCategories(initialData.poiCategories));
    }, [initialData]);

    // Fetch location suggestions (debounced)
    React.useEffect(() => {
        if (readOnly || isSubmitted) return;
        const query = locationQuery.trim();
        if (selectedLocation && selectedLocation.label === query) {
            setLocationSuggestions([]);
            return;
        }
        if (query.length < 3) {
            setLocationSuggestions([]);
            return;
        }

        const controller = new AbortController();
        const timeoutId = window.setTimeout(async () => {
            try {
                setIsSearchingLocation(true);
                setLocationError(null);
                const params = new URLSearchParams({
                    q: query,
                    format: "json",
                    addressdetails: "1",
                    limit: "5",
                });
                const res = await fetch(
                    `https://nominatim.openstreetmap.org/search?${params.toString()}`,
                    {
                        signal: controller.signal,
                        headers: {
                            "Accept-Language": "en",
                        },
                    }
                );
                if (!res.ok) {
                    throw new Error(`HTTP ${res.status}`);
                }
                const data: LocationSuggestion[] = await res.json();
                setLocationSuggestions(data);
            } catch (error: unknown) {
                if (
                    error &&
                    typeof error === "object" &&
                    "name" in error &&
                    (error as { name?: string }).name === "AbortError"
                ) {
                    return;
                }
                setLocationError("Could not fetch locations. Please try again.");
                setLocationSuggestions([]);
            } finally {
                setIsSearchingLocation(false);
            }
        }, 350);

        return () => {
            clearTimeout(timeoutId);
            controller.abort();
        };
    }, [locationQuery, readOnly, isSubmitted, selectedLocation]);

    const toggleCategory = (category: string) => {
        if (readOnly || isSubmitted || isSubmitting) return;
        setSelectedCategories((prev) =>
            prev.includes(category)
                ? prev.filter((c) => c !== category)
                : [...prev, category]
        );
    };

    const handleLocationSelect = (suggestion: LocationSuggestion) => {
        const latitude = parseFloat(suggestion.lat);
        const longitude = parseFloat(suggestion.lon);
        setSelectedLocation({
            latitude,
            longitude,
            label: suggestion.display_name,
            locationName: suggestion.display_name,
        });
        setLocationQuery(suggestion.display_name);
        setLocationSuggestions([]);
        setLocationError(null);
    };

    const handleUseCurrentLocation = () => {
        if (readOnly || isSubmitted || isSubmitting) return;
        if (!navigator.geolocation) {
            setLocationError("Geolocation is not supported in this browser.");
            return;
        }

        setIsFetchingCurrentLocation(true);
        setLocationError(null);
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                const { latitude, longitude } = pos.coords;
                const label = "Current location";
                setSelectedLocation({
                    latitude,
                    longitude,
                    label,
                    locationName: label,
                });
                setLocationQuery(label);
                setLocationSuggestions([]);
                setIsFetchingCurrentLocation(false);
            },
            () => {
                setLocationError("Could not fetch your current location. Please allow location access or pick a place manually.");
                setIsFetchingCurrentLocation(false);
            },
            { enableHighAccuracy: true, timeout: 10000 }
        );
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (readOnly || !onSubmit || isSubmitting || isSubmitted) return;

        if (!selectedLocation) {
            setLocationError("Please choose a location to start from.");
            return;
        }

        if (!startDate || !endDate) {
            return;
        }

        setIsSubmitting(true);
        setLocationError(null);

        // Ensure dates are present
        const finalStartDate = startDate || getTodayString();
        const finalEndDate = endDate || getTodayString();

        try {
            await onSubmit({
                latitude: selectedLocation.latitude,
                longitude: selectedLocation.longitude,
                locationName: selectedLocation.locationName ?? selectedLocation.label,
                rangeMeters: range,
                startDate: finalStartDate,
                endDate: finalEndDate,
                poiCategories: selectedCategories,
            });
            setIsSubmitted(true);
        } catch (error) {
            console.error("Survey submission failed", error);
            // Optionally handle error state here, e.g. show error message
            // For now, we allow retrying if it failed (by setting isSubmitting back to false)
        } finally {
            setIsSubmitting(false);
        }
    };

    const hasLocation = Boolean(selectedLocation);
    const hasRange = Number.isFinite(range);
    const hasDates = Boolean(startDate) && Boolean(endDate);
    const isFormComplete = hasLocation && hasRange && hasDates;

    const isDisabled = readOnly || isSubmitting || isSubmitted;

    return (
        <Card className="w-full max-w-lg mx-auto border border-gray-200 shadow-sm bg-white rounded-xl overflow-hidden">
            <CardHeader className="bg-gray-50/50 border-b border-gray-100 pb-4">
                <CardTitle className="text-lg font-semibold text-gray-800">Trip Preferences</CardTitle>
                <CardDescription className="text-gray-500 text-sm">
                    {readOnly || isSubmitted ? "Your submitted preferences" : "Tell us where you're starting and what you're looking for."}
                </CardDescription>
            </CardHeader>
            <CardContent className="p-6 space-y-6">
                <form onSubmit={handleSubmit} className="space-y-6">
                    {/* Location Picker */}
                    <div className="space-y-2">
                        <Label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Location</Label>
                        {readOnly || isSubmitted ? (
                            <div className="flex items-center gap-3 p-3 border border-gray-200 rounded-lg bg-gray-50 text-gray-700 text-sm">
                                <MapPin className="w-4 h-4 text-gray-400" />
                                <div className="flex flex-col">
                                    <span className="font-medium">
                                        {selectedLocation?.label ?? "Location not provided"}
                                    </span>
                                    {selectedLocation && (
                                        <span className="text-xs text-gray-500">
                                            Lat {selectedLocation.latitude.toFixed(4)}, Lon {selectedLocation.longitude.toFixed(4)}
                                        </span>
                                    )}
                                </div>
                            </div>
                        ) : (
                            <div className="relative">
                                <div className="flex gap-2">
                                    <Input
                                        value={locationQuery}
                                        onChange={(e) => {
                                            setLocationQuery(e.target.value);
                                            setSelectedLocation(null);
                                            setLocationError(null);
                                        }}
                                        placeholder="Search for a city, address, or place"
                                        disabled={isDisabled || isFetchingCurrentLocation}
                                        className="pr-3"
                                    />
                                    <TooltipProvider>
                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <Button
                                                    type="button"
                                                    variant="secondary"
                                                    size="icon"
                                                    onClick={handleUseCurrentLocation}
                                                    disabled={isDisabled || isFetchingCurrentLocation}
                                                    aria-label={isFetchingCurrentLocation ? "Locating current position" : "Use my current location"}
                                                    className="h-10 w-10"
                                                >
                                                    {isFetchingCurrentLocation ? (
                                                        <Loader2 className="h-4 w-4 animate-spin" />
                                                    ) : (
                                                        <LocateFixed className="h-4 w-4" />
                                                    )}
                                                </Button>
                                            </TooltipTrigger>
                                            <TooltipContent>
                                                <p>{isFetchingCurrentLocation ? "Locating…" : "Use my location"}</p>
                                            </TooltipContent>
                                        </Tooltip>
                                    </TooltipProvider>
                                </div>
                                {isSearchingLocation && (
                                    <div className="mt-1 text-xs text-gray-500">Searching…</div>
                                )}
                                {locationSuggestions.length > 0 && (
                                    <ul className="absolute z-20 mt-1 w-full max-h-52 overflow-y-auto rounded-lg border border-gray-200 bg-white shadow-md">
                                        {locationSuggestions.map((suggestion) => (
                                            <li
                                                key={suggestion.place_id}
                                                className="cursor-pointer px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
                                                onClick={() => handleLocationSelect(suggestion)}
                                            >
                                                {suggestion.display_name}
                                            </li>
                                        ))}
                                    </ul>
                                )}
                                <div className="mt-1 text-xs text-gray-500">
                                    Start typing (min 3 characters) to search worldwide, or use your current location.
                                </div>
                            </div>
                        )}
                        {locationError && (
                            <div className="text-xs text-red-600">{locationError}</div>
                        )}
                    </div>

                    {/* Range */}
                    <div className="space-y-4">
                        <Label htmlFor="range-input" className="text-xs font-medium text-gray-500 uppercase tracking-wider">Search Range</Label>
                        <div className="flex items-center gap-4">
                            <Slider
                                className="flex-1"
                                value={[range / 1000]}
                                onValueChange={(vals) => {
                                    const km = vals[0];
                                    setRange(km * 1000);
                                    setRangeInput(km.toString());
                                }}
                                max={30}
                                min={1}
                                step={1}
                                disabled={isDisabled}
                            />
                            <Input
                                id="range-input"
                                type="number"
                                className="w-24 rounded-lg border-gray-200 focus-visible:ring-gray-900"
                                value={rangeInput}
                                onChange={(e) => {
                                    const valStr = e.target.value;
                                    setRangeInput(valStr);

                                    const val = parseFloat(valStr);
                                    if (!isNaN(val)) {
                                        if (val > 30) {
                                            // Clamp > 30 immediately to 30 for better UX
                                            setRange(30000);
                                            setRangeInput("30");
                                        } else if (val >= 1) {
                                            // Valid range update
                                            setRange(val * 1000);
                                        }
                                        // If val < 1 (e.g. 0), keep typing but don't update range yet
                                    }
                                }}
                                onBlur={() => {
                                    let val = parseFloat(rangeInput);
                                    if (isNaN(val)) {
                                        // Invalid/Empty -> revert to last valid state
                                        val = range / 1000;
                                    } else {
                                        // Clamp on blur
                                        if (val < 1) val = 1;
                                        if (val > 30) val = 30;
                                    }
                                    setRange(val * 1000);
                                    setRangeInput(val.toString());
                                }}
                                disabled={isDisabled}
                                min={1}
                                max={30}
                                step={1}
                            />
                            <span className="text-sm font-medium text-gray-700">km</span>
                        </div>
                    </div>

                    {/* Dates */}
                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <Label htmlFor="start-date" className="text-xs font-medium text-gray-500 uppercase tracking-wider">Start Date</Label>
                            <Input
                                id="start-date"
                                type="date"
                                className="rounded-lg border-gray-200 focus-visible:ring-gray-900"
                                value={startDate}
                                onChange={(e) => setStartDate(e.target.value)}
                                required={!isDisabled}
                                disabled={isDisabled}
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="end-date" className="text-xs font-medium text-gray-500 uppercase tracking-wider">End Date</Label>
                            <Input
                                id="end-date"
                                type="date"
                                className="rounded-lg border-gray-200 focus-visible:ring-gray-900"
                                value={endDate}
                                onChange={(e) => setEndDate(e.target.value)}
                                required={!isDisabled}
                                disabled={isDisabled}
                            />
                        </div>
                    </div>

                    {/* POI Categories */}
                    <div className="space-y-3">
                        <Label className="text-xs font-medium text-gray-500 uppercase tracking-wider">
                            Interests <span className="text-[10px] font-normal lowercase italic opacity-70">(Optional)</span>
                        </Label>
                        <div className="flex flex-wrap gap-2">
                            {POI_CATEGORIES.map((cat) => {
                                const isSelected = selectedCategories.includes(cat);
                                return (
                                    <button
                                        key={cat}
                                        type="button"
                                        onClick={() => toggleCategory(cat)}
                                        disabled={isDisabled}
                                        style={isSelected ? { backgroundColor: '#4b5563', color: 'white', borderColor: '#4b5563' } : {}}
                                        className={`
                      px-3 py-1.5 rounded-full text-sm font-medium transition-all duration-200 border
                      ${isSelected
                                                ? "shadow-sm"
                                                : "bg-white text-gray-600 border-gray-200 hover:border-gray-300 hover:bg-gray-50"}
                      ${isDisabled ? "cursor-default opacity-80" : "cursor-pointer"}
                    `}
                                    >
                                        {cat}
                                    </button>
                                );
                            })}
                        </div>
                    </div>
                </form>
            </CardContent>
            {!readOnly && !isSubmitted && (
                <CardFooter className="bg-gray-50/50 border-t border-gray-100 p-4">
                    <Button
                        style={{ backgroundColor: '#4b5563', color: 'white' }}
                        className="w-full font-medium py-2.5 rounded-lg transition-all shadow-sm hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
                        onClick={handleSubmit}
                        disabled={isSubmitting || !isFormComplete}
                    >
                        {isSubmitting ? "Submitting..." : "Find Recommendations"}
                    </Button>
                </CardFooter>
            )}
            {isSubmitted && (
                <CardFooter className="bg-gray-50/50 border-t border-gray-100 p-4">
                    <div className="w-full text-center text-sm text-gray-500 font-medium">
                        Submitted
                    </div>
                </CardFooter>
            )}
        </Card>
    );
}
