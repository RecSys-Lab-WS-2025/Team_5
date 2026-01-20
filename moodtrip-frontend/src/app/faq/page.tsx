"use client";

import { Navbar } from "@/components/layout/navbar";
import { Link } from "react-router-dom";

const FAQS = [
  {
    q: "What is Moodtrip?",
    a: "Moodtrip is a chat-based travel assistant that recommends routes based on your mood and energy level.",
  },
  {
    q: "What makes it different?",
    a: "It prioritizes emotional fit and pacing instead of generic popularity or ratings.",
  },
  {
    q: "How do I start?",
    a: "Open the chat, describe how you feel, complete the survey, and receive a route.",
  },
  {
    q: "Is Spotify required?",
    a: "No. Spotify is optional and only used to recommend mood-matching playlists.",
  },
  {
    q: "How does feedback help?",
    a: "POI ratings are used to refine and improve future recommendations.",
  },
];

export default function FAQPage() {
  return (
    <div className="!min-h-screen !flex !flex-col !bg-blue-50 !text-black">
      <header className="!fixed !top-0 !left-0 !w-full !z-50">
        <Navbar />
      </header>

      <main className="!flex-1 !pt-44 md:!pt-48 !pb-28 !px-6 md:!px-12 lg:!px-24">
        <div className="!mx-auto !max-w-4xl">
          <p className="!text-xs !font-semibold !tracking-[0.22em] !uppercase !text-blue-700/80">
            Help center
          </p>

          <h1 className="!mt-4 !text-3xl md:!text-4xl !font-extrabold !tracking-tight">
            FAQ
          </h1>

          <div className="!mt-10 !divide-y !divide-gray-200/60">
            {FAQS.map((f) => (
              <details key={f.q} className="!py-5">
                <summary className="!cursor-pointer !text-base md:!text-lg !font-semibold">
                  {f.q}
                </summary>
                <p className="!mt-3 !text-sm md:!text-base !text-gray-700">
                  {f.a}
                </p>
              </details>
            ))}
          </div>

          <div className="!mt-12 !pt-8 !border-t !border-gray-200/60">
            <p className="!text-sm !text-gray-700">
              Still have questions?{" "}
              <Link
                to="/contact"
                className="!font-semibold !text-blue-700 !underline !underline-offset-4"
              >
                Contact us
              </Link>
            </p>
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
