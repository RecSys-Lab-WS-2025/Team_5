import React, { useState, useRef, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Send } from "lucide-react";
import { useSidebar } from "@/components/ui/sidebar";

interface WelcomeScreenProps {
  onSuggestionClick: (suggestion: string) => void;
  userName?: string | null;
}

type Mood =
  | "joyful"
  | "energized"
  | "calm"
  | "content"
  | "curious"
  | "tired"
  | "stressed"
  | "anxious"
  | "overwhelmed";

const suggestions: { title: string; mood: Mood }[] = [
  {
    title:
      "I'm feeling really joyful today and want a fun place to celebrate this great mood.",
    mood: "joyful",
  },
  {
    title:
      "I'm energized and ready for something active where I can move and explore.",
    mood: "energized",
  },
  {
    title:
      "I'm feeling calm and would love a peaceful spot to unwind and breathe.",
    mood: "calm",
  },
  {
    title:
      "I'm feeling pretty content and just want a cozy, easy-going kind of trip.",
    mood: "content",
  },
  {
    title:
      "I'm curious and want to discover somewhere new that sparks my sense of wonder.",
    mood: "curious",
  },
  {
    title:
      "I'm tired and would love a slow, gentle place where I can rest and recharge.",
    mood: "tired",
  },
  {
    title:
      "I'm stressed and need a quiet escape to clear my head and decompress.",
    mood: "stressed",
  },
  {
    title:
      "I'm a bit anxious and want somewhere soothing that can help me feel safer and more at ease.",
    mood: "anxious",
  },
  {
    title:
      "I'm overwhelmed and need a simple, low-pressure getaway where everything feels taken care of.",
    mood: "overwhelmed",
  },
];

// ---------------------------
// EXPRESSIVE MOOD ICONS
// ---------------------------
function MoodIcon({ mood }: { mood: Mood }) {
  const styles: Record<Mood, { bg: string; color: string }> = {
    joyful: { bg: "bg-yellow-100", color: "text-yellow-600" },
    energized: { bg: "bg-orange-100", color: "text-orange-600" },
    calm: { bg: "bg-sky-100", color: "text-sky-600" },
    content: { bg: "bg-emerald-100", color: "text-emerald-600" },
    curious: { bg: "bg-violet-100", color: "text-violet-600" },
    tired: { bg: "bg-slate-100", color: "text-slate-600" },
    stressed: { bg: "bg-red-100", color: "text-red-600" },
    anxious: { bg: "bg-cyan-100", color: "text-cyan-600" },
    overwhelmed: { bg: "bg-rose-100", color: "text-rose-600" },
  };

  const { bg, color } = styles[mood];

  const mouthPath: Record<Mood, string> = {
    joyful: "M6 15 Q12 20 18 15",
    energized: "M5 15 Q12 22 19 15",
    calm: "M7 16 H17",
    content: "M7 15 Q12 18 17 15",
    curious: "M8 15 Q12 17 16 15",
    tired: "M6 17 Q12 13 18 17",
    stressed: "M6 18 Q9 14 12 17 Q15 20 18 14",
    anxious: "M6 18 Q10 16 12 18 Q14 20 18 16",
    overwhelmed: "M6 19 Q12 9 18 19",
  };

  const eyes: Record<Mood, React.ReactNode> = {
    joyful: (
      <>
        <circle cx="9" cy="9" r="2.3" fill="currentColor" />
        <circle cx="15" cy="9" r="2.3" fill="currentColor" />
      </>
    ),
    energized: (
      <>
        <circle cx="9" cy="9" r="2.5" fill="currentColor" />
        <circle cx="15" cy="9" r="2.5" fill="currentColor" />
      </>
    ),
    calm: (
      <>
        <circle cx="9" cy="9" r="2.1" fill="currentColor" />
        <circle cx="15" cy="9" r="2.1" fill="currentColor" />
      </>
    ),
    content: (
      <>
        <circle cx="9" cy="9" r="2" fill="currentColor" />
        <circle cx="15" cy="9" r="2" fill="currentColor" />
      </>
    ),
    curious: (
      <>
        <circle cx="9" cy="9" r="2.2" fill="currentColor" />
        <circle cx="15" cy="9" r="2.2" fill="currentColor" />
      </>
    ),
    tired: (
      <>
        <path
          d="M7 9 Q8 8 9 9"
          stroke="currentColor"
          strokeWidth={2}
          strokeLinecap="round"
        />
        <path
          d="M15 9 Q16 8 17 9"
          stroke="currentColor"
          strokeWidth={2}
          strokeLinecap="round"
        />
      </>
    ),
    stressed: (
      <>
        <path
          d="M7 9 Q9 7 11 9"
          stroke="currentColor"
          strokeWidth={2}
          strokeLinecap="round"
        />
        <path
          d="M13 9 Q15 7 17 9"
          stroke="currentColor"
          strokeWidth={2}
          strokeLinecap="round"
        />
      </>
    ),
    anxious: (
      <>
        <circle cx="9" cy="9" r="1.5" fill="currentColor" />
        <circle cx="15" cy="9" r="1.5" fill="currentColor" />
        <path
          d="M6 8 Q12 5 18 8"
          stroke="currentColor"
          strokeWidth={2}
          strokeLinecap="round"
        />
      </>
    ),
    overwhelmed: (
      <>
        <path
          d="M7 8 L11 10"
          stroke="currentColor"
          strokeWidth={2.2}
          strokeLinecap="round"
        />
        <path
          d="M17 8 L13 10"
          stroke="currentColor"
          strokeWidth={2.2}
          strokeLinecap="round"
        />
      </>
    ),
  };

  return (
    <div
      className={`flex h-10 w-10 items-center justify-center rounded-full ${bg}`}
    >
      <svg
        viewBox="0 0 24 24"
        className={`h-7 w-7 ${color}`}
        aria-hidden="true"
      >
        {eyes[mood]}
        <path
          d={mouthPath[mood]}
          fill="none"
          stroke="currentColor"
          strokeWidth={2.8}
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </div>
  );
}

// ---------------------------
// MAIN COMPONENT
// ---------------------------
export function WelcomeScreen({
  onSuggestionClick,
  userName,
}: WelcomeScreenProps) {
  const [value, setValue] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);
  const sidebar = useSidebar();
  const { setOpen, state, isMobile } = sidebar;
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const handlePick = (text: string) => {
    setOpen(false);
    onSuggestionClick(text);
  };

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const text = value.trim();
    if (!text) return;
    setOpen(false);
    onSuggestionClick(text);
    setValue("");
  };

  const displayName = userName?.trim() || "John";

  const fixedBarStyle = !isMobile
    ? {
        left: `var(${
          state === "expanded" ? "--sidebar-width" : "--sidebar-width-icon"
        })`,
      }
    : undefined;

  const renderCard = (item: (typeof suggestions)[number], index: number) => (
    <Button
      key={index}
      variant="ghost"
      onClick={() => handlePick(item.title)}
      className={`
        group
        !h-auto !w-full
        !rounded-2xl
        !border-0
        !bg-white
        shadow-[0_8px_24px_rgba(15,23,42,0.05)]
        hover:shadow-[0_12px_32px_rgba(15,23,42,0.08)]
        !p-5 !text-left
        transition-all duration-500 ease-out
        ${mounted ? "opacity-100 translate-y-0" : "opacity-0 translate-y-2"}
      `}
      style={{ transitionDelay: `${index * 50}ms` }}
    >
      <div className="relative w-full">
        <div className="absolute right-0 top-0">
          <MoodIcon mood={item.mood} />
        </div>
        <p className="pr-14 text-sm leading-relaxed text-gray-900 !whitespace-normal !break-words">
          {item.title}
        </p>
      </div>
    </Button>
  );

  return (
    <div className="relative flex h-full bg-white flex-1 flex-col">
      <div className="flex-1 px-6 pb-32">
        <div className="mx-auto max-w-3xl py-12 text-center">
          <div className="mb-6 flex justify-center">
            <img
              src="/moodtrip-bot.png"
              alt="MoodTrip chatbot"
              className="h-30 w-30 rounded-full"
            />
          </div>

          <p className="text-base text-gray-600">
            Hi, {displayName}! I'm <span className="font-medium">MoMo</span>,
            your mood-based travel buddy.
          </p>

          <h2 className="mt-4 text-2xl font-semibold text-gray-900">
            How are you feeling today?
          </h2>
        </div>

        <div className="mx-auto w-full max-w-5xl">
          {/* Small screens: 2×2 (first 4) */}
          <div className="hidden sm:grid grid-cols-2 gap-4 md:hidden">
            {suggestions.slice(0, 4).map((item, index) =>
              renderCard(item, index)
            )}
          </div>

          {/* Medium and up: full 3×3 */}
          <div className="hidden md:grid md:grid-cols-3 gap-4">
            {suggestions.map((item, index) => renderCard(item, index))}
          </div>
        </div>
      </div>

      {/* bottom input */}
      <div
        className={`fixed bottom-10 z-50 flex justify-center transition-all duration-300 ${
          isMobile ? "left-3 right-3" : "right-4"
        }`}
        style={fixedBarStyle}
      >
        <form onSubmit={handleSubmit} className="w-full max-w-3xl">
          <div className="flex items-center gap-3 rounded-full border border-black/10 bg-white px-4 py-2.5">
            <Input
              ref={inputRef}
              value={value}
              onChange={(e) => setValue(e.target.value)}
              placeholder="Tell me how you're feeling..."
              className="h-auto flex-1 border-0 !bg-white px-0 text-sm shadow-none focus-visible:ring-0"
            />

            <Button
              type="submit"
              size="icon"
              disabled={!value.trim()}
              className="ml-1 h-9 w-9 rounded-full !bg-black text-white hover:bg-black/80 disabled:bg-black/40"
            >
              <Send className="h-4 w-4" />
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
