"use client";

import { Navbar } from "@/components/layout/navbar";

export default function ContactPage() {
  return (
    <div className="!min-h-screen !flex !flex-col !bg-blue-50 !text-black">
      <header className="!fixed !top-0 !left-0 !w-full !z-50">
        <Navbar />
      </header>

      <main className="!flex-1 !pt-44 md:!pt-48 !pb-28 !px-6 md:!px-12 lg:!px-24">
        <div className="!mx-auto !max-w-4xl">
          <p className="!text-xs !font-semibold !tracking-[0.22em] !uppercase !text-blue-700/80">
            Get in touch
          </p>

          <h1 className="!mt-4 !text-3xl md:!text-4xl !font-extrabold">
            Contact
          </h1>

          <p className="!mt-6 !text-base md:!text-lg !text-gray-700">
            For collaboration, questions, or press inquiries, feel free to reach out.
          </p>

          <div className="!mt-12 !space-y-8">
            <div>
              <div className="!text-xs !uppercase !tracking-[0.22em] !text-gray-500">
                Email
              </div>
              <a
                href="mailto:hello@moodtrip.com"
                className="!block !mt-2 !text-lg !font-semibold !underline !underline-offset-4"
              >
                hello@moodtrip.com
              </a>
            </div>

            <div>
              <div className="!text-xs !uppercase !tracking-[0.22em] !text-gray-500">
                Social
              </div>
              <div className="!mt-2 !text-base !text-gray-700">
                X / Instagram / LinkedIn: <span className="!font-semibold">@moodtrip</span>
              </div>
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
