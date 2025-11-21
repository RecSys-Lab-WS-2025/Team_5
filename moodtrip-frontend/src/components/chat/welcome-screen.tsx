"use client";

import { useState, useRef } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ArrowUpRight, Send } from "lucide-react";

interface WelcomeScreenProps {
  onSuggestionClick: (suggestion: string) => void;
  userName?: string | null;
}

const suggestions = [
  { title: "I've been really busy lately and not in a great mood—I want to unwind." },
  { title: "Something great happened today—I'm so happy!" },
  { title: "Nothing special has happened recently, and I feel pretty neutral." },
  { title: "Study/work stress is piling up; I'd love a quiet place to decompress." },
  { title: "Feeling a bit anxious—I'd like fresh air and somewhere calming to clear my head." },
  { title: "I'm full of energy and want a lively, vibrant place to explore!" },
  { title: "I'm a bit tired and just want a slow, relaxing pace." },
  { title: "Mood is steady—I’d enjoy a casual wander without a packed schedule." },
];

export function WelcomeScreen({ onSuggestionClick, userName }: WelcomeScreenProps) {
  const [value, setValue] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);

  const handlePick = (text: string) => {
    setValue(text);
    requestAnimationFrame(() => inputRef.current?.focus());
  };

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const text = value.trim();
    if (!text) return;
    onSuggestionClick(text);
    setValue("");
  };

  const displayName = userName && userName.trim().length > 0 ? userName : "John";

  return (
    <div className="flex h-full flex-1 flex-col">
      <div className="flex-1 px-6">
        <div className="mx-auto max-w-3xl py-12 text-center">
          <h1 className="mb-4 text-6xl font-bold">
            <span className="bg-gradient-to-r from-pink-500 via-red-500 to-red-600 bg-clip-text text-transparent">
              Welcome, {displayName}
            </span>
          </h1>
        </div>

        <div className="mx-auto grid w-full max-w-3xl grid-cols-1 gap-4 md:grid-cols-2">
          {suggestions.map((s, i) => (
            <Button
              key={i}
              variant="outline"
              onClick={() => handlePick(s.title)}
              className="group h-auto border-gray-200 p-6 text-left hover:bg-gray-50"
            >
              <div className="flex w-full items-start justify-between">
                <div className="flex-1 pr-4">
                  <p className="text-wrap text-sm leading-relaxed text-muted-foreground">
                    {s.title}
                  </p>
                </div>
                <ArrowUpRight className="h-4 w-4 flex-shrink-0 text-muted-foreground group-hover:text-primary" />
              </div>
            </Button>
          ))}
        </div>
      </div>

      <div className="border-t p-6">
        <div className="mx-auto max-w-3xl">
          <form onSubmit={handleSubmit} className="relative">
            <div
              className="
                flex items-center gap-3
                rounded-full border border-black/10 bg-background px-4 py-2.5
                shadow-md
                dark:border-white/10 dark:bg-[#303030]
              "
            >
              <Input
                ref={inputRef}
                value={value}
                onChange={(e) => setValue(e.target.value)}
                placeholder='Share a quick check-in (e.g., "Feeling stressed from exams, want somewhere quiet to relax.")'
                className="
                  h-auto flex-1 border-0 bg-transparent px-0
                  text-sm
                  shadow-none
                  focus-visible:ring-0 focus-visible:ring-offset-0
                "
              />

              <Button
                type="submit"
                size="icon"
                disabled={!value.trim()}
                aria-label="Send"
                className="
                  ml-1 h-9 w-9 rounded-full
                  !bg-black text-white
                  hover:bg-black/90
                  disabled:bg-black/40 disabled:text-white/70
                  dark:bg-white dark:text-black dark:hover:bg-white/90
                "
              >
                <Send className="h-4 w-4" />
              </Button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
