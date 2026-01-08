import { Star, Users, ExternalLink } from "lucide-react";
import { Card } from "@/components/ui/card";

export interface RouteCardProps {
    title: string;
    description: string;
    imageUrl: string;
    distanceMeters: number;
    durationSeconds: number;
    index?: number; // TODO: Remove this mock index when we have real ranking or remove it if not needed
    onClick?: () => void;
}

export function RouteCard({
    title,
    description,
    imageUrl,
    distanceMeters,
    durationSeconds,
    index,
    onClick,
}: RouteCardProps) {
    // TODO: Replace this mock difficulty logic with real data from backend
    // Mock Difficulty based on distance
    const getDifficulty = (meters: number) => {
        if (meters < 3000) return 1;
        if (meters < 6000) return 2;
        if (meters < 10000) return 3;
        if (meters < 15000) return 4;
        return 5;
    };

    const difficulty = getDifficulty(distanceMeters);

    // TODO: Use a proper localization/formatting library or backend string
    // Format Duration
    const formatDuration = (seconds: number) => {
        const hours = Math.floor(seconds / 3600);
        const mins = Math.round((seconds % 3600) / 60);
        if (hours === 0) return `${mins} min`;
        return `${hours} h ${mins} min`;
    };

    const durationText = formatDuration(durationSeconds);

    // TODO: Replace with real crowd level data from backend API
    // Mock Crowd Level
    const crowdLevel = 2.5;

    const renderStars = (rating: number) => {
        return Array.from({ length: 5 }).map((_, i) => (
            <Star
                key={i}
                className={`w-4 h-4 ${i < rating ? "fill-yellow-400 text-yellow-400" : "text-gray-300"}`}
            />
        ));
    };

    const renderCrowd = (level: number) => {
        return Array.from({ length: 5 }).map((_, i) => (
            <Users
                key={i}
                className={`w-4 h-4 ${i < Math.ceil(level) ? "fill-blue-500 text-blue-500" : "text-gray-300"}`}
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

                    {/* Crowd Level */}
                    <div className="flex items-center gap-2 text-xs">
                        <span className="font-medium text-gray-500 w-16">Crowd Level:</span>
                        <div className="flex gap-0.5">{renderCrowd(crowdLevel)}</div>
                    </div>
                </div>
            </div>
        </Card>
    );
}
