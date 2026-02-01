import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
    DialogClose,
} from "@/components/ui/dialog";
import { MessageSquareHeart, SlidersHorizontal, Map, PlayCircle, X } from "lucide-react";
import React from "react";

export function HowItWorksDialog({ children }: { children: React.ReactNode }) {
    return (
        <Dialog>
            <DialogTrigger asChild>{children}</DialogTrigger>
            <DialogContent className="sm:max-w-[425px]">
                <DialogHeader>
                    <DialogTitle className="text-2xl font-bold text-center mb-2">
                        How MoodTrip Works
                    </DialogTitle>
                    <DialogDescription className="text-center">
                        Turn your feelings into a perfect travel itinerary in 3 simple steps.
                    </DialogDescription>
                </DialogHeader>
                <div className="grid gap-6 py-4">
                    <div className="flex items-start gap-4">
                        <div className="p-3 rounded-full bg-pink-100 text-pink-600">
                            <MessageSquareHeart className="w-6 h-6" />
                        </div>
                        <div>
                            <h3 className="font-semibold text-gray-900">1. Share Your Mood</h3>
                            <p className="text-sm text-gray-500">
                                Chat with our AI companion. Tell us how you're feeling and what kind of vibe you're looking for.
                            </p>
                        </div>
                    </div>

                    <div className="flex items-start gap-4">
                        <div className="p-3 rounded-full bg-blue-100 text-blue-600">
                            <SlidersHorizontal className="w-6 h-6" />
                        </div>
                        <div>
                            <h3 className="font-semibold text-gray-900">2. Personalize</h3>
                            <p className="text-sm text-gray-500">
                                Customize your preferences. Foodie? History buff? Nature lover? We adjust the route to you.
                            </p>
                        </div>
                    </div>

                    <div className="flex items-start gap-4">
                        <div className="p-3 rounded-full bg-green-100 text-green-600">
                            <Map className="w-6 h-6" />
                        </div>
                        <div>
                            <h3 className="font-semibold text-gray-900">3. Get Your Trip</h3>
                            <p className="text-sm text-gray-500">
                                Receive a tailored itinerary complete with routes, mood-matched spots, and hidden gems.
                            </p>
                        </div>
                    </div>
                </div>

                <div className="border-t border-gray-100 mt-2 pt-4">
                    <button
                        type="button"
                        className="w-full flex items-center justify-center gap-2 text-sm text-blue-600 hover:text-blue-700 hover:underline transition-colors cursor-pointer"
                        onClick={() => window.open("https://youtu.be/l-z4m303NjQ", "_blank")} // Placeholder link
                    >
                        <PlayCircle className="w-4 h-4" />
                        Still confused? Watch a quick start video
                    </button>
                </div>
                <DialogClose className="absolute right-4 top-4 rounded-sm opacity-70 ring-offset-background transition-opacity hover:opacity-100 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:pointer-events-none cursor-pointer">
                    <X className="h-4 w-4" />
                    <span className="sr-only">Close</span>
                </DialogClose>
            </DialogContent>
        </Dialog>
    );
}
