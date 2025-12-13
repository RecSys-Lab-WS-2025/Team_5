import { Button } from "@/components/ui/button";
import { Navbar } from "@/components/layout/navbar";
import { useNavigate } from "react-router-dom";
import { getUser } from "@/api/auth";

const TravelIcon = () => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    className="w-6 h-6 text-white"
  >
    <circle cx="12" cy="12" r="10" />
    <path d="M12 2a8 8 0 0 0 8 8c0 1.66-1.34 3-3 3s-3-1.34-3-3M12 2v20M2 12h20" />
  </svg>
);

const PlayIcon = () => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 24 24"
    fill="currentColor"
    className="w-5 h-5 text-blue-500"
  >
    <polygon points="5,3 19,12 5,21" />
  </svg>
);

export function HomePage() {
  const navigate = useNavigate();
  return (
    <div className="relative min-h-screen bg-blue-50 text-black">
      <div
        className="absolute top-0 left-0 w-full h-full overflow-hidden pointer-events-none hidden lg:block"
        aria-hidden="true"
      >
        <img
          src="/earth.png"
          alt="Abstract background illustration of the Earth"
          className="absolute top-0 left-0 w-[600px] h-[600px] object-cover opacity-20"
        />
      </div>

      {/* Navbar */}
      <header className="fixed top-0 left-0 w-full z-50">
        <Navbar />
      </header>

      {/* Hero section */}
      <main className="min-h-screen pt-24 pb-12 flex items-center justify-center px-6 md:px-12 lg:px-24 relative">
        <div className="w-full max-w-6xl grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-12 items-center">
          
          {/* Left side: text */}
          <div className="flex flex-col text-center lg:text-left lg:order-1">
            <p className="text-xl font-bold uppercase tracking-wider text-orange-600 mb-7 mx-auto lg:mx-0">
              No More Mediocre Trips.
            </p>

            <h1
              className="!text-[3rem] md:text-6xl font-black mb-9 tracking-tight text-gray-900"
              style={{ fontFamily: "'Montserrat', sans-serif" }}
            >
              Your Mood Trip Maker
            </h1>

            <p className="text-lg md:text-xl max-w-lg text-gray-700 mb-15 mx-auto lg:mx-0 font-bold">
              Your mood is the map! Chat with me—I'll design travel plans that best fit your mood.
            </p>

            <div className="flex items-center gap-6 mt-6 mx-auto lg:mx-0">
              <div className="btn-container w-fit flex items-center mr-8 gap-2">
                <TravelIcon />
                <Button
                  onClick={() => {
                    const user = getUser();
                    if (!user) {
                      navigate("/login");
                    } else {
                      navigate("/chat");
                    }
                  }}
                  className="custom-swipe-btn"
                >
                  Get My Trip
                </Button>
              </div>

              {/* Play / guide button - FORCED PAGE BACKGROUND COLOR */}
              <button
                type="button"
                onClick={() => navigate("/demo")}
                className="flex items-center gap-3 group !bg-blue-50 !border-0 !p-0 focus:outline-none" 
              >
                {/* Play Icon Wrapper - FORCED PAGE BACKGROUND COLOR */}
                <div className="w-10 h-10 rounded-full border-2 border-blue-400 flex items-center justify-center group-hover:scale-105 transition-all duration-200 !bg-blue-50">
                  <PlayIcon />
                </div>
                <span className="text-gray-600 font-bold group-hover:text-blue-500 transition">
                  How it works
                </span>
              </button>
            </div>
          </div>

          {/* Right side: illustration */}
          <div className="flex justify-center lg:justify-end lg:-mr-30 hidden lg:flex lg:order-5">
            <img
              src="/hero.png"
              alt="Illustration representing personalized travel planning"
              className="w-full max-w-md md:max-w-lg lg:max-w-4xl h-auto object-contain"
            />
          </div>
        </div>
      </main>
      
      {/* Deep Blue Footer */}
      <footer className="bg-blue-900 w-full py-4 mt-auto">
        <div className="max-w-6xl mx-auto px-6 text-white text-center text-sm">
          &copy; {new Date().getFullYear()} MoodTrip Maker. All rights reserved.
        </div>
      </footer>
    </div>
  );
}