import { Button } from "@/components/ui/button";
import { Navbar } from "@/components/layout/navbar";
import { ArrowRight } from "lucide-react";
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
    <path d="M12 2a8 8 0 0 0 8 8c0 1.66-1.34 3-3 3s-3-1.34-3-3M12 2v20M2 12h20"/>
  </svg>
);

export function HomePage() {
  const navigate = useNavigate();
  return (
    <div className="relative min-h-screen bg-blue-50 text-black">
      {/* Background Layer */}
      <div 
        className="absolute top-0 left-0 w-full h-full overflow-hidden pointer-events-none"
        aria-hidden="true" 
      >
        <img
          src="/earth.png" 
          alt="Abstract background illustration of the Earth"
          className="absolute top-0 left-0 w-[600px] h-[600px] object-cover opacity-20" 
        />
      </div>

      {/* Fixed Navbar */}
      <header className="fixed top-0 left-0 w-full z-50">
        <Navbar />
      </header>

      {/* Hero Section (Content Layer) */}
      <main className="min-h-screen pt-24 pb-12 flex items-center justify-center px-6 md:px-12 lg:px-24 relative">
        <div className="w-full max-w-6xl grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-12 items-center">
          
          {/* Left Side: Text Content - Centered for all screen sizes */}
          <div className="flex flex-col text-center lg:text-center">
            <h1 className="!text-[3.3rem] md:text-5xl font-extrabold mb-14 tracking-tight text-gray-900">
              Your Mood Trip Maker
            </h1>
            
            <p className="text-lg md:text-xl max-w-lg text-gray-700 mb-10 mx-auto lg:mx-0 whitespace-nowrap !font-semibold">
              Chat with me—I'll design travel plans that best fit your mood.
            </p>

            <div className="btn-container mx-auto w-fit">
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
          </div>

          {/* Right Side: Illustration (Hero Image) */}
          <div className="flex justify-center lg:justify-end lg:-mr-30">
            <img
              src="/hero.png" 
              alt="Illustration representing personalized travel planning"
              className="w-full max-w-md md:max-w-lg lg:max-w-4xl h-auto object-contain" 
            />
          </div>
        </div>
      </main>
    </div>
  );
}