import { Button } from "@/components/ui/button";
import { Navbar } from "@/components/layout/navbar";
import { ArrowRight } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { getUser } from "@/api/auth";

export function HomePage() {
  const navigate = useNavigate();
  return (
    <div className="relative min-h-screen bg-blue-50 text-black">
      {/* Fixed Navbar */}
      <header className="fixed top-0 left-0 w-full z-50">
        <Navbar />
      </header>

      {/* Hero Section */}
      <main className="flex flex-col items-center justify-center h-screen px-6 text-center">
        <h1 className="text-5xl font-bold mb-4 tracking-tight">
          Trip Maker
        </h1>
        <p className="text-lg md:text-xl max-w-xl text-gray-700 mb-10">
          Chat with me — I’ll design travel plans that fit your mood.
        </p>

        <Button
          onClick={() => {
            const user = getUser();
            if (!user) {
              navigate("/login");
            } else {
              navigate("/chat");
            }
          }}
          className="flex items-center gap-2 !bg-black !text-white hover:!bg-gray-700"
        >
          Get My Trip
          <ArrowRight className="w-4 h-4 animate-pulse" />
        </Button>
      </main>
    </div>
  );
}
