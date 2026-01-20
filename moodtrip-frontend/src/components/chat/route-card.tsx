import { Star, ExternalLink } from "lucide-react";
import { Card } from "@/components/ui/card";

export interface RouteCardProps {
    title: string;
    description: string;
    imageUrl: string;
    distanceMeters: number;
    durationSeconds: number;
    tripDays?: number;
    index?: number;
    onClick?: () => void;
}

export function RouteCard({
    title,
    description,
    imageUrl,
    distanceMeters,
    durationSeconds,
    tripDays = 1,
    index,
    onClick,
}: RouteCardProps) {
    // Calculate difficulty based on DAILY AVERAGE distance and duration
    // This makes multi-day trips easier than single-day trips with the same total distance
    const getDifficulty = (meters: number, seconds: number, days: number) => {
        const dailyKm = (meters / 1000) / days;
        const dailyHours = (seconds / 3600) / days;

        // Calculate daily effort score combining distance and time
        // Distance weight: 1 point per km
        // Time weight: 0.3 points per hour (accounts for pace/terrain)
        const dailyEffortScore = dailyKm + (dailyHours * 0.3);

        // Difficulty levels based on DAILY effort score:
        // 1 star: Very Easy (< 2 daily effort) - leisurely walks under 2km/day
        // 2 stars: Easy (2-4 daily effort) - casual walks 2-4km/day
        // 3 stars: Moderate (4-7 daily effort) - decent walks 4-7km/day
        // 4 stars: Challenging (7-12 daily effort) - active days 7-12km/day
        // 5 stars: Demanding (> 12 daily effort) - intense hiking 12km+/day
        if (dailyEffortScore < 2) return 1;
        if (dailyEffortScore < 4) return 2;
        if (dailyEffortScore < 7) return 3;
        if (dailyEffortScore < 12) return 4;
        return 5;
    };

    const difficulty = getDifficulty(distanceMeters, durationSeconds, tripDays);

    const formatDuration = (seconds: number) => {
        const hours = Math.floor(seconds / 3600);
        const mins = Math.round((seconds % 3600) / 60);
        if (hours === 0) return `${mins} min`;
        return `${hours} h ${mins} min`;
    };

    const durationText = formatDuration(durationSeconds);

    const renderStars = (rating: number) => {
        return Array.from({ length: 5 }).map((_, i) => (
            <Star
                key={i}
                className={`w-4 h-4 ${i < rating ? "fill-yellow-400 text-yellow-400" : "text-gray-300"}`}
            />
        ));
    };

    return (
        <Card
            className="w-full h-full overflow-hidden hover:shadow-lg transition-all duration-300 cursor-pointer group bg-white border-gray-100 flex flex-col"
            onClick={onClick}
        >
            {/* Image Section */}
            <div className="w-full h-40 relative flex-shrink-0 overflow-hidden">
                {index !== undefined && (
                    <div className="absolute top-2 left-2 z-10 bg-black/50 text-white px-2 py-1 rounded-md text-xs font-bold backdrop-blur-sm">
                        #{index}
                    </div>
                )}
                <img
                    src={imageUrl}
                    alt={title}
                    className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
                    onError={(e) => {
                        (e.target as HTMLImageElement).src = "/placeholder.png";
                    }}
                />
            </div>

            {/* Content Section */}
            <div className="flex-1 p-4 flex flex-col justify-between">
                <div>
                    <div className="flex justify-between items-start gap-2">
                        <h3 className="text-base font-bold text-gray-900 group-hover:text-emerald-600 transition-colors line-clamp-1 leading-tight">
                            {title}
                        </h3>
                        <ExternalLink className="w-4 h-4 text-gray-400 flex-shrink-0 mt-0.5 opacity-0 group-hover:opacity-100 transition-opacity" />
                    </div>
                    <p className="mt-1.5 text-xs text-gray-600 line-clamp-2 leading-relaxed h-8">
                        {description}
                    </p>
                </div>

                <div className="mt-4 space-y-2">
                    {/* Difficulty */}
                    <div className="flex items-center gap-2 text-xs">
                        <span className="font-medium text-gray-500 w-16">Difficulty:</span>
                        <div className="flex gap-0.5">{renderStars(difficulty)}</div>
                    </div>

                    {/* Duration */}
                    <div className="flex items-center gap-2 text-xs">
                        <span className="font-medium text-gray-500 w-16">Duration:</span>
                        <span className="text-gray-700 font-medium">{durationText}</span>
                    </div>
                </div>
            </div>
        </Card>
    );
}
