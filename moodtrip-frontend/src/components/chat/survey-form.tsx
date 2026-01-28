import * as React from "react";
import { Calendar, CheckCircle2, Compass, Loader2, LocateFixed, MapPin, Sparkles } from "lucide-react";

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
    const getTodayString = () => new Date().toISOString().split('T')[0];

    const calculateTripDays = (start: string, end: string): number => {
        const startDate = new Date(start);
        const endDate = new Date(end);
        const diffTime = endDate.getTime() - startDate.getTime();
        const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24)) + 1;
        return Math.max(1, Math.min(5, diffDays));
    };

    const [range, setRange] = React.useState<number>(initialData?.rangeMeters ?? 5000);
    const [rangeInput, setRangeInput] = React.useState<string>((initialData?.rangeMeters ? initialData.rangeMeters / 1000 : 5).toString());

    const [tripDays, setTripDays] = React.useState<number>(
        initialData?.startDate && initialData?.endDate
            ? calculateTripDays(initialData.startDate, initialData.endDate)
            : 1
    );
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

        setIsSubmitting(true);
        setLocationError(null);

        const finalStartDate = getTodayString();
        const endDateObj = new Date();
        endDateObj.setDate(endDateObj.getDate() + tripDays - 1);
        const finalEndDate = [
            endDateObj.getFullYear(),
            String(endDateObj.getMonth() + 1).padStart(2, "0"),
            String(endDateObj.getDate()).padStart(2, "0"),
        ].join("-");

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
        } finally {
            setIsSubmitting(false);
        }
    };

    const hasLocation = Boolean(selectedLocation);
    const hasRange = Number.isFinite(range);
    const hasTripDays = tripDays >= 1 && tripDays <= 5;
    const isFormComplete = hasLocation && hasRange && hasTripDays;

    const isDisabled = readOnly || isSubmitting || isSubmitted;

    return (
        <Card className="w-full max-w-lg mx-auto border border-gray-200 shadow-xl bg-white rounded-2xl overflow-hidden">
            <CardHeader className="bg-gray-100 border-b border-gray-200 pb-6 pt-4">
                <div className="flex items-center gap-3">
                    <div className="p-2 bg-gray-200 rounded-xl">
                        <Compass className="w-6 h-6 text-gray-700" />
                    </div>
                    <div>
                        <CardTitle className="text-xl font-bold text-gray-800">Trip Preferences</CardTitle>
                        <CardDescription className="text-gray-500 text-sm mt-1">
                            {readOnly || isSubmitted ? "Your submitted preferences" : "Tell us about your ideal trip"}
                        </CardDescription>
                    </div>
                </div>
            </CardHeader>

            <CardContent className="p-6 space-y-6">
                <form onSubmit={handleSubmit} className="space-y-6">
                    <div className="space-y-3">
                        <div className="flex items-center gap-2">
                            <MapPin className="w-4 h-4 text-gray-700" />
                            <Label className="text-sm font-semibold text-gray-700">Starting Location</Label>
                        </div>
                        {readOnly || isSubmitted ? (
                            <div className="flex items-center gap-3 p-4 border border-gray-200 rounded-xl bg-gray-50 text-gray-700">
                                <div className="p-2 bg-gray-200 rounded-lg">
                                    <MapPin className="w-4 h-4 text-gray-700" />
                                </div>
                                <div className="flex flex-col min-w-0 flex-1">
                                    <span className="font-medium text-gray-800 truncate">
                                        {selectedLocation?.label ?? "Location not provided"}
                                    </span>
                                    {selectedLocation && (
                                        <span className="text-xs text-gray-500">
                                            {selectedLocation.latitude.toFixed(4)}, {selectedLocation.longitude.toFixed(4)}
                                        </span>
                                    )}
                                </div>
                            </div>
                        ) : (
                            <div className="relative">
                                <div className="flex gap-2">
                                    <div className="relative flex-1">
                                        <Input
                                            value={locationQuery}
                                            onChange={(e) => {
                                                setLocationQuery(e.target.value);
                                                setSelectedLocation(null);
                                                setLocationError(null);
                                            }}
                                            placeholder="Search for a city, address, or place..."
                                            disabled={isDisabled || isFetchingCurrentLocation}
                                            className="h-11 pl-4 pr-3 rounded-xl border-gray-200 bg-gray-50/50 focus:bg-white focus:border-gray-400 focus-visible:ring-gray-900/10 transition-all"
                                        />
                                        {selectedLocation && (
                                            <div className="absolute right-3 top-1/2 -translate-y-1/2">
                                                <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                                            </div>
                                        )}
                                    </div>
                                    <TooltipProvider>
                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <Button
                                                    type="button"
                                                    variant="outline"
                                                    size="icon"
                                                    onClick={handleUseCurrentLocation}
                                                    disabled={isDisabled || isFetchingCurrentLocation}
                                                    aria-label={isFetchingCurrentLocation ? "Locating current position" : "Use my current location"}
                                                    className="h-11 w-11 rounded-xl border-gray-200 hover:bg-gray-100 hover:border-gray-400 hover:text-gray-900 transition-all"
                                                >
                                                    {isFetchingCurrentLocation ? (
                                                        <Loader2 className="h-4 w-4 animate-spin" />
                                                    ) : (
                                                        <LocateFixed className="h-4 w-4" />
                                                    )}
                                                </Button>
                                            </TooltipTrigger>
                                            <TooltipContent>
                                                <p>{isFetchingCurrentLocation ? "Locating..." : "Use my location"}</p>
                                            </TooltipContent>
                                        </Tooltip>
                                    </TooltipProvider>
                                </div>
                                {isSearchingLocation && (
                                    <div className="mt-2 flex items-center gap-2 text-xs text-gray-500">
                                        <Loader2 className="w-3 h-3 animate-spin" />
                                        Searching...
                                    </div>
                                )}
                                {locationSuggestions.length > 0 && (
                                    <ul className="absolute z-20 mt-2 w-full max-h-52 overflow-y-auto rounded-xl border border-gray-200 bg-white shadow-lg">
                                        {locationSuggestions.map((suggestion) => (
                                            <li
                                                key={suggestion.place_id}
                                                className="cursor-pointer px-4 py-3 text-sm text-gray-700 hover:bg-gray-100 transition-colors first:rounded-t-xl last:rounded-b-xl border-b border-gray-100 last:border-0"
                                                onClick={() => handleLocationSelect(suggestion)}
                                            >
                                                <div className="flex items-start gap-3">
                                                    <MapPin className="w-4 h-4 text-gray-400 mt-0.5 flex-shrink-0" />
                                                    <span className="line-clamp-2">{suggestion.display_name}</span>
                                                </div>
                                            </li>
                                        ))}
                                    </ul>
                                )}
                                {!selectedLocation && !isSearchingLocation && locationSuggestions.length === 0 && (
                                    <p className="mt-2 text-xs text-gray-400">
                                        Type at least 3 characters to search, or use your current location
                                    </p>
                                )}
                            </div>
                        )}
                        {locationError && (
                            <div className="flex items-center gap-2 text-xs text-red-600 bg-red-50 px-3 py-2 rounded-lg">
                                <span>{locationError}</span>
                            </div>
                        )}
                    </div>

                    <div className="border-t border-gray-100" />

                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-3">
                            <div className="flex items-center gap-2">
                                <Calendar className="w-4 h-4 text-gray-700" />
                                <Label htmlFor="trip-days" className="text-sm font-semibold text-gray-700">Duration</Label>
                            </div>
                            <select
                                id="trip-days"
                                className="flex h-11 w-full rounded-xl border border-gray-200 bg-gray-50/50 px-4 py-2 text-sm font-medium text-gray-700 focus:outline-none focus:ring-2 focus:ring-gray-900/10 focus:border-gray-400 focus:bg-white disabled:cursor-not-allowed disabled:opacity-50 transition-all appearance-none cursor-pointer"
                                style={{
                                    backgroundImage: `url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%236b7280' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3e%3c/svg%3e")`,
                                    backgroundPosition: 'right 0.75rem center',
                                    backgroundRepeat: 'no-repeat',
                                    backgroundSize: '1.25rem 1.25rem',
                                }}
                                value={tripDays}
                                onChange={(e) => setTripDays(Number(e.target.value))}
                                disabled={isDisabled}
                            >
                                <option value={1}>1 day</option>
                                <option value={2}>2 days</option>
                                <option value={3}>3 days</option>
                                <option value={4}>4 days</option>
                                <option value={5}>5 days</option>
                            </select>
                        </div>

                        <div className="space-y-3">
                            <div className="flex items-center gap-2">
                                <Compass className="w-4 h-4 text-gray-700" />
                                <Label htmlFor="range-input" className="text-sm font-semibold text-gray-700">Range</Label>
                            </div>
                            <div className="flex items-center gap-2">
                                <Input
                                    id="range-input"
                                    type="number"
                                    className="h-11 rounded-xl border-gray-200 bg-gray-50/50 focus:bg-white focus:border-gray-400 focus-visible:ring-gray-900/10 text-center font-medium transition-all"
                                    value={rangeInput}
                                    onChange={(e) => {
                                        const valStr = e.target.value;
                                        setRangeInput(valStr);
                                        const val = parseFloat(valStr);
                                        if (!isNaN(val)) {
                                            if (val > 30) {
                                                setRange(30000);
                                                setRangeInput("30");
                                            } else if (val >= 1) {
                                                setRange(val * 1000);
                                            }
                                        }
                                    }}
                                    onBlur={() => {
                                        let val = parseFloat(rangeInput);
                                        if (isNaN(val)) {
                                            val = range / 1000;
                                        } else {
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
                                <span className="text-sm font-medium text-gray-500 whitespace-nowrap">km</span>
                            </div>
                        </div>
                    </div>

                    <div className="px-1">
                        <Slider
                            className="w-full"
                            value={[range / 1000]}
                            onValueChange={(vals) => {
                                const km = vals[0];
                                setRange(km * 1000);
                                setRangeInput(km.toString());
                            }}
                            max={30}
                            min={1}
                            step={0.1}
                            disabled={isDisabled}
                        />
                        <div className="flex justify-between mt-1 text-xs text-gray-400">
                            <span>1 km</span>
                            <span>30 km</span>
                        </div>
                    </div>

                    <div className="border-t border-gray-100" />

                    <div className="space-y-3">
                        <div className="flex items-center gap-2">
                            <Sparkles className="w-4 h-4 text-gray-700" />
                            <Label className="text-sm font-semibold text-gray-700">
                                Interests
                                <span className="ml-2 text-xs font-normal text-gray-400">(Optional)</span>
                            </Label>
                        </div>
                        <div className="grid grid-cols-3 gap-2">
                            {POI_CATEGORIES.map((cat) => {
                                const isSelected = selectedCategories.includes(cat);
                                return (
                                    <button
                                        key={cat}
                                        type="button"
                                        onClick={() => toggleCategory(cat)}
                                        disabled={isDisabled}
                                        style={isSelected ? { backgroundColor: '#4b5563', color: '#fff', borderColor: '#4b5563' } : {}}
                                        className={`
                                            px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 border outline-none
                                            ${isSelected
                                                ? "shadow-md hover:!bg-gray-600 focus:ring-2 focus:ring-gray-400 focus:ring-offset-2"
                                                : "bg-white text-gray-600 border-gray-200 hover:border-gray-400 hover:bg-gray-100 hover:text-gray-900 focus:ring-2 focus:ring-black focus:ring-offset-2"}
                                            ${isDisabled ? "cursor-default opacity-70" : "cursor-pointer"}
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
                <CardFooter className="bg-gray-50 border-t border-gray-100 p-4">
                    <Button
                        style={
                            isSubmitting || !isFormComplete
                                ? { backgroundColor: '#d1d5db', color: '#6b7280' }
                                : { backgroundColor: '#4b5563', color: '#fff' }
                        }
                        className={`w-full h-12 font-semibold text-base rounded-xl transition-all duration-200 disabled:cursor-not-allowed outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-2 ${
                            isSubmitting || !isFormComplete
                                ? "shadow-none"
                                : "hover:!bg-gray-600 active:!bg-gray-700 shadow-lg shadow-black/30"
                        }`}
                        onClick={handleSubmit}
                        disabled={isSubmitting || !isFormComplete}
                    >
                        {isSubmitting ? (
                            <span className="flex items-center gap-2">
                                <Loader2 className="w-4 h-4 animate-spin" />
                                Finding recommendations...
                            </span>
                        ) : (
                            <span className="flex items-center gap-2">
                                <Sparkles className="w-4 h-4" />
                                Find Recommendations
                            </span>
                        )}
                    </Button>
                </CardFooter>
            )}

            {isSubmitted && (
                <CardFooter className="bg-gray-50 border-t border-gray-200 p-4">
                    <div className="w-full flex items-center justify-center gap-2 text-gray-700 font-medium">
                        <CheckCircle2 className="w-5 h-5" />
                        Preferences submitted
                    </div>
                </CardFooter>
            )}
        </Card>
    );
}