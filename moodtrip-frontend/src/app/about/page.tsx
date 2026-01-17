"use client";

import { Navbar } from "@/components/layout/navbar";

export default function AboutPage() {
  return (
    <div className="!min-h-screen !flex !flex-col !bg-blue-50 !text-black">
      <header className="!fixed !top-0 !left-0 !w-full !z-50">
        <Navbar />
      </header>

      <main className="!flex-1 !pt-44 md:!pt-48 !pb-28 !px-6 md:!px-12 lg:!px-24">
        <div className="!mx-auto !w-full !max-w-4xl">
          <p className="!text-xs !font-semibold !tracking-[0.22em] !uppercase !text-blue-700/80">
            Mood-based travel companion
          </p>

          <h1 className="!mt-4 !text-3xl md:!text-4xl !font-extrabold !tracking-tight !text-gray-900">
            About Moodtrip
          </h1>

          <p className="!mt-6 !text-base md:!text-lg !leading-relaxed !text-gray-700">
            Moodtrip is a mood-driven travel assistant that designs short, fast-paced routes
            based on how you feel. Instead of asking where you want to go, it starts by
            understanding your emotional state and energy level.
          </p>

          <div className="!mt-12 !grid !grid-cols-1 md:!grid-cols-2 !gap-10">
            <div>
              <h2 className="!text-sm !font-semibold !text-gray-900">Mood-first routing</h2>
              <p className="!mt-2 !text-sm md:!text-base !text-gray-700">
                Routes are generated from mood, pace, and context rather than popularity alone.
              </p>
            </div>

            <div>
              <h2 className="!text-sm !font-semibold !text-gray-900">Spotify integration</h2>
              <p className="!mt-2 !text-sm md:!text-base !text-gray-700">
                When connected with Spotify, playlists are recommended to match your trip vibe.
              </p>
            </div>

            <div>
              <h2 className="!text-sm !font-semibold !text-gray-900">Learning system</h2>
              <p className="!mt-2 !text-sm md:!text-base !text-gray-700">
                POI ratings after trips help improve future route recommendations.
              </p>
            </div>

            <div>
              <h2 className="!text-sm !font-semibold !text-gray-900">Built for spontaneity</h2>
              <p className="!mt-2 !text-sm md:!text-base !text-gray-700">
                Designed for quick decisions, short breaks, and same-day adventures.
              </p>
            </div>
          </div>
        </div>
      </main>

      <footer className="!mt-auto !bg-blue-900 !py-4">
        <div className="!text-center !text-xs !text-white/90">
          © {new Date().getFullYear()} MoodTrip Maker. All rights reserved.
        </div>
      </footer>
    </div>
  );
}
