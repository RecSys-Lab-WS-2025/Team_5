import * as React from "react";
import { MapPin } from "lucide-react";

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

// Mock Munich Coordinates
const MUNICH_COORDS = {
    latitude: 48.1351,
    longitude: 11.5820,
};

const POI_CATEGORIES = [
    "Nature",
    "History & Culture",
    "Adventure",
    "Relaxation",
    "Food & Culinary",
    "Nightlife",
    "Art & Museums",
    "Shopping",
    "Beach & Coast",
    "City Exploration",
];

const RANGE_OPTIONS = [
    { label: "500 m", value: 500 },
    { label: "1 km", value: 1000 },
    { label: "2 km", value: 2000 },
    { label: "5 km", value: 5000 },
    { label: "10 km", value: 10000 },
];

export interface SurveyData {
    latitude: number;
    longitude: number;
    rangeMeters: number;
    startDate: string;
    endDate: string;
    poiCategories: string[];
}

export function SurveyForm({
    onSubmit,
    readOnly = false,
    initialData,
}: {
    onSubmit?: (data: SurveyData) => void;
    readOnly?: boolean;
    initialData?: SurveyData;
}) {
    // Helper to get date string YYYY-MM-DD
    const getTodayString = () => new Date().toISOString().split('T')[0];
    const getTomorrowString = () => {
        const d = new Date();
        d.setDate(d.getDate() + 1);
        return d.toISOString().split('T')[0];
    };

    const [range, setRange] = React.useState<number>(initialData?.rangeMeters ?? 1000);
    const [startDate, setStartDate] = React.useState<string>(initialData?.startDate ?? getTodayString());
    const [endDate, setEndDate] = React.useState<string>(initialData?.endDate ?? getTomorrowString());
    const [selectedCategories, setSelectedCategories] = React.useState<string[]>(initialData?.poiCategories ?? []);

    const toggleCategory = (category: string) => {
        if (readOnly) return;
        setSelectedCategories((prev) =>
            prev.includes(category)
                ? prev.filter((c) => c !== category)
                : [...prev, category]
        );
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (readOnly || !onSubmit) return;

        // Ensure dates are present
        const finalStartDate = startDate || getTodayString();
        const finalEndDate = endDate || getTomorrowString();

        onSubmit({
            latitude: MUNICH_COORDS.latitude,
            longitude: MUNICH_COORDS.longitude,
            rangeMeters: range,
            startDate: finalStartDate,
            endDate: finalEndDate,
            poiCategories: selectedCategories,
        });
    };

    return (
        <Card className="w-full max-w-lg mx-auto border border-gray-200 shadow-sm bg-white rounded-xl overflow-hidden">
            <CardHeader className="bg-gray-50/50 border-b border-gray-100 pb-4">
                <CardTitle className="text-lg font-semibold text-gray-800">Trip Preferences</CardTitle>
                <CardDescription className="text-gray-500 text-sm">
                    {readOnly ? "Your submitted preferences" : "Tell us what you're looking for in Munich."}
                </CardDescription>
            </CardHeader>
            <CardContent className="p-6 space-y-6">
                <form onSubmit={handleSubmit} className="space-y-6">
                    {/* Location (Fixed) */}
                    <div className="space-y-2">
                        <Label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Location</Label>
                        <div className="flex items-center gap-3 p-3 border border-gray-200 rounded-lg bg-gray-50 text-gray-700 text-sm">
                            <MapPin className="w-4 h-4 text-gray-400" />
                            <span className="font-medium">Munich (Current Location)</span>
                        </div>
                    </div>

                    {/* Range */}
                    <div className="space-y-2">
                        <Label htmlFor="range" className="text-xs font-medium text-gray-500 uppercase tracking-wider">Search Range</Label>
                        <select
                            id="range"
                            className="flex h-10 w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gray-900 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 transition-all hover:border-gray-300"
                            value={range}
                            onChange={(e) => setRange(Number(e.target.value))}
                            disabled={readOnly}
                        >
                            {RANGE_OPTIONS.map((opt) => (
                                <option key={opt.value} value={opt.value}>
                                    {opt.label}
                                </option>
                            ))}
                        </select>
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
                                required={!readOnly}
                                disabled={readOnly}
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
                                required={!readOnly}
                                disabled={readOnly}
                            />
                        </div>
                    </div>

                    {/* POI Categories */}
                    <div className="space-y-3">
                        <Label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Interests</Label>
                        <div className="flex flex-wrap gap-2">
                            {POI_CATEGORIES.map((cat) => {
                                const isSelected = selectedCategories.includes(cat);
                                return (
                                    <button
                                        key={cat}
                                        type="button"
                                        onClick={() => toggleCategory(cat)}
                                        disabled={readOnly}
                                        style={isSelected ? { backgroundColor: '#4b5563', color: 'white', borderColor: '#4b5563' } : {}}
                                        className={`
                      px-3 py-1.5 rounded-full text-sm font-medium transition-all duration-200 border
                      ${isSelected
                                                ? "shadow-sm"
                                                : "bg-white text-gray-600 border-gray-200 hover:border-gray-300 hover:bg-gray-50"}
                      ${readOnly ? "cursor-default opacity-80" : "cursor-pointer"}
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
            {!readOnly && (
                <CardFooter className="bg-gray-50/50 border-t border-gray-100 p-4">
                    <Button
                        style={{ backgroundColor: '#4b5563', color: 'white' }}
                        className="w-full font-medium py-2.5 rounded-lg transition-all shadow-sm hover:opacity-90"
                        onClick={handleSubmit}
                    >
                        Find Recommendations
                    </Button>
                </CardFooter>
            )}
        </Card>
    );
}
