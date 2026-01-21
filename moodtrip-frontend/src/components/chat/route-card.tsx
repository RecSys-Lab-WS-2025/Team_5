import { Star, ExternalLink, StarHalf } from "lucide-react";
import { Card } from "@/components/ui/card";
import type { AppRouteType } from "@/api/conversation";

export interface RouteCardProps {
    title: string;
    description: string;
    imageUrl: string;
    distanceMeters: number;
    durationSeconds: number;
    tripDays?: number;
    index?: number;
    routeType?: AppRouteType;
    routeTypeTitle?: string;
    onClick?: () => void;
}

// Map route types to colors
const routeTypeColors: Record<AppRouteType, { bg: string; text: string }> = {
    BALANCED: { bg: "bg-emerald-500", text: "text-white" },
    YOUR_PICKS: { bg: "bg-blue-500", text: "text-white" },
    DISCOVERY: { bg: "bg-purple-500", text: "text-white" },
};

export function RouteCard({
    title,
    description,
    imageUrl,
    distanceMeters,
    durationSeconds,
    tripDays = 1,
    index,
    routeType,
    routeTypeTitle,
    onClick,
}: RouteCardProps) {
    // Calculate difficulty based on DAILY AVERAGE distance and duration
    const getDifficulty = (meters: number, seconds: number, days: number) => {
        if (!meters && !seconds) return 1.0;

        const dailyKm = (meters / 1000) / Math.max(1, days);
        const dailyHours = (seconds / 3600) / Math.max(1, days);

        const dailyEffortScore = dailyKm + (dailyHours * 0.3);

        if (dailyEffortScore < 3) return 1.0;
        if (dailyEffortScore < 5) return 1.5;
        if (dailyEffortScore < 7) return 2.0;
        if (dailyEffortScore < 9) return 2.5;
        if (dailyEffortScore < 11) return 3.0;
        if (dailyEffortScore < 13) return 3.5;
        if (dailyEffortScore < 15) return 4.0;
        if (dailyEffortScore < 17) return 4.5;
        return 5.0;
    };

    const difficulty = getDifficulty(distanceMeters || 0, durationSeconds || 0, tripDays);

    const formatDuration = (seconds?: number) => {
        if (!seconds) return "N/A";
        const hours = Math.floor(seconds / 3600);
        const mins = Math.round((seconds % 3600) / 60);
        if (hours === 0) return `${mins} min`;
        return `${hours} h ${mins} min`;
    };

    const durationText = formatDuration(durationSeconds);

    const renderStars = (rating: number) => {
        return Array.from({ length: 5 }).map((_, i) => {
            const starValue = i + 1;
            const isFull = rating >= starValue;
            const isHalf = rating >= starValue - 0.5 && rating < starValue;

            if (isHalf) {
                return (
                    <StarHalf
                        key={i}
                        className="w-3.5 h-3.5 fill-yellow-400 text-yellow-400"
                    />
                );
            }

            return (
                <Star
                    key={i}
                    className={`w-3.5 h-3.5 ${isFull ? "fill-yellow-400 text-yellow-400" : "text-gray-200"}`}
                />
            );
        });
    };

    const hasError = !title && !description;

    return (
        <Card
            className={`
                w-full h-full min-h-[320px] flex flex-col 
                overflow-hidden bg-white border-gray-200/60 shadow-sm
                hover:shadow-xl hover:border-emerald-500/30 hover:-translate-y-1
                transition-all duration-300 cursor-pointer group rounded-xl
                ${hasError ? "opacity-70 grayscale" : ""}
            `}
            onClick={hasError ? undefined : onClick}
        >
            {/* Image Section */}
            <div className="w-full h-44 relative flex-shrink-0 overflow-hidden bg-gray-100">
                {index !== undefined && (
                    <div className="absolute top-3 left-3 z-10 bg-white/90 text-gray-900 px-2.5 py-1 rounded-full text-xs font-bold shadow-sm backdrop-blur-sm border border-white/50">
                        #{index}
                    </div>
                )}
                {routeType && routeTypeTitle && (
                    <div className={`absolute top-3 right-3 z-10 ${routeTypeColors[routeType]?.bg ?? "bg-gray-600"} ${routeTypeColors[routeType]?.text ?? "text-white"} px-2.5 py-1 rounded-full text-xs font-semibold shadow-sm backdrop-blur-sm border border-white/20`}>
                        {routeTypeTitle}
                    </div>
                )}
                {imageUrl ? (
                    <img
                        src={imageUrl}
                        alt={title || "Route Image"}
                        className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
                        onError={(e) => {
                            (e.target as HTMLImageElement).src = "/placeholder-route.jpg";
                        }}
                    />
                ) : (
                    <div className="w-full h-full flex items-center justify-center text-gray-300 bg-gray-50">
                        <span className="text-sm">No Image</span>
                    </div>
                )}
            </div>

            {/* Content Section */}
            <div className="flex-1 p-5 flex flex-col relative">
                <div className="flex-1">
                    <div className="flex justify-between items-start gap-3 mb-2">
                        <h3 className="text-lg font-bold text-gray-800 group-hover:text-emerald-600 transition-colors line-clamp-1 leading-tight tracking-tight">
                            {title || "Untitled Route"}
                        </h3>
                        <ExternalLink className="w-4 h-4 text-emerald-500/50 flex-shrink-0 mt-1 opacity-0 group-hover:opacity-100 transition-all duration-300 translate-x-2 group-hover:translate-x-0" />
                    </div>

                    <p className="text-xs text-gray-500 line-clamp-3 leading-relaxed mb-4 min-h-[3.6em]">
                        {description || "No description available for this route."}
                    </p>
                </div>

                <div className="pt-4 mt-auto border-t border-gray-50">
                    <div className="grid grid-cols-2 gap-4">
                        {/* Difficulty */}
                        <div className="flex flex-col space-y-1">
                            <span className="text-[10px] uppercase tracking-wider font-semibold text-gray-400">Difficulty</span>
                            <div className="flex gap-0.5" title={`${difficulty} Stars`}>
                                {renderStars(difficulty)}
                            </div>
                        </div>

                        {/* Duration */}
                        <div className="flex flex-col space-y-1 text-right">
                            <span className="text-[10px] uppercase tracking-wider font-semibold text-gray-400">Duration</span>
                            <span className="text-sm font-bold text-gray-700">{durationText}</span>
                        </div>
                    </div>
                </div>
            </div>
        </Card>
    );
}
