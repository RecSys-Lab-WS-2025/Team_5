"use client";

import React from "react";
import ReactMarkdown from "react-markdown";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Send } from "lucide-react";
import type { UIMessage } from "@ai-sdk/react";
import { useSidebar } from "@/components/ui/sidebar";
import type { FeatureCollection } from "geojson";
import { useNavigate } from "react-router-dom";
import { RouteCarousel } from "@/components/chat/route-carousel";
import { SurveyForm } from "./survey-form";
import type { SurveyData } from "@/api/conversation";
import { RecommendedRouteMap } from "@/components/map/recommended-route";

interface ChatInterfaceProps {
  messages: UIMessage[];
  input: string;
  handleInputChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  handleSubmit: (e: React.FormEvent<HTMLFormElement>) => void;
  isLoading: boolean;
  isInputLocked?: boolean;
  routeGeoJson?: FeatureCollection | null;
  onSurveySubmit?: (data: SurveyData) => Promise<void> | void;
  currentEmotion?: string | null;
  chatId?: string | null;
  spotifyPlaylistUrl?: string | null;
}

function SpotifyVinylMiniCard({
  url,
  onClose,
  title = "Moodtrip playlist",
  subtitle = "Open in Spotify",
}: {
  url: string;
  onClose: () => void;
  title?: string;
  subtitle?: string;
}) {
  return (
    <>
      <div
        className="
          fixed top-[88px] right-6 z-[100]
          w-[260px] sm:w-[280px]
          rounded-2xl border bg-white/90 backdrop-blur
          shadow-[0_10px_30px_rgba(0,0,0,0.12)]
          overflow-hidden
        "
        style={{
          animation: "playlist-slide-in 520ms cubic-bezier(0.22, 1, 0.36, 1)",
        }}
        role="region"
        aria-label="Spotify playlist recommendation"
      >
        <div className="relative p-3">
          <button
            type="button"
            onClick={onClose}
            className="
              absolute right-2 top-2
              h-7 w-7 rounded-full
              grid place-items-center
              hover:bg-black/5
            "
            aria-label="Close"
          >
            <span className="text-[16px] leading-none">×</span>
          </button>

          <button
            type="button"
            onClick={() => window.open(url, "_blank", "noopener,noreferrer")}
            className="w-full text-left"
          >
            <div className="flex items-center gap-3">
              <div className="relative h-12 w-12 shrink-0">
                <div className="absolute inset-0 rounded-full bg-black/5 blur-[6px]" />

                <div
                  className="absolute inset-0 rounded-full bg-gradient-to-br from-neutral-900 to-neutral-700 shadow-sm"
                  style={{ animation: "vinyl-spin 3.6s linear infinite" }}
                >
                  <div className="absolute inset-[6px] rounded-full border border-white/10" />
                  <div className="absolute inset-[10px] rounded-full border border-white/8" />
                  <div className="absolute inset-[14px] rounded-full border border-white/6" />

                  <div className="absolute left-1/2 top-1/2 h-4 w-4 -translate-x-1/2 -translate-y-1/2 rounded-full bg-white/90" />
                  <div className="absolute left-1/2 top-1/2 h-[3px] w-[3px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-neutral-700" />
                </div>

                <div className="absolute -right-1 top-1/2 h-[2px] w-5 -translate-y-1/2 rotate-[12deg] rounded-full bg-neutral-300" />
              </div>

              <div className="min-w-0 flex-1">
                <div className="text-sm font-semibold tracking-tight truncate">
                  {title}
                </div>
                <div className="mt-0.5 text-xs text-muted-foreground truncate">
                  {subtitle}
                </div>

                <div className="mt-2 inline-flex items-center gap-1 text-xs font-medium">
                  <span className="inline-block h-2 w-2 rounded-full bg-[#1DB954]" />
                  <span>Spotify</span>
                </div>
              </div>
            </div>
          </button>
        </div>
      </div>

      <style>{`
        @keyframes vinyl-spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }

        @keyframes playlist-slide-in {
          from { transform: translateX(40px); opacity: 0; }
          to { transform: translateX(0); opacity: 1; }
        }

        @media (prefers-reduced-motion: reduce) {
          [style*="vinyl-spin"] { animation: none !important; }
          [style*="playlist-slide-in"] { animation: none !important; }
        }
      `}</style>
    </>
  );
}

function extractLatestSpotifyUrlFromMessages(messages: UIMessage[]): string | null {
  for (let i = messages.length - 1; i >= 0; i--) {
    const m = messages[i];
    if (m.role === "user") continue;

    for (const p of m.parts) {
      if (p.type !== "text") continue;
      const t = p.text || "";

      // Markdown: [text](https://open.spotify.com/...)
      const md = t.match(/\[([^\]]*)\]\((https?:\/\/open\.spotify\.com\/[^)\s]+)\)/i);
      if (md?.[2]) return md[2];

      // looser Markdown
      const mdLoose = t.match(/\[([^\]]*)\]\((https?:\/\/open\.spotify\.com\/[^)]+)\)/i);
      if (mdLoose?.[2]) return mdLoose[2].trim();

      // (https://open.spotify.com/...)
      const paren = t.match(/\((https?:\/\/open\.spotify\.com\/[^)\s]+)\)/i);
      if (paren?.[1]) return paren[1];

      // raw https://open.spotify.com/...
      // ✅ remove useless escape: \) -> )
      const raw = t.match(/https?:\/\/open\.spotify\.com\/[^\s)\n]+/i);
      if (raw?.[0]) return raw[0];

      // any spotify.com url
      // ✅ remove useless escape: \) -> )
      const anySpotify = t.match(/https?:\/\/[^\s)\n]*spotify\.com\/[^\s)\n]+/i);
      if (anySpotify?.[0]) return anySpotify[0];
    }
  }
  return null;
}

function getTextParts(message: UIMessage): string[] {
  return message.parts
    .filter((p) => p.type === "text")
    .map((p) => (p.type === "text" ? p.text : ""))
    .filter(Boolean);
}

function messageContains(message: UIMessage, needle: string): boolean {
  return getTextParts(message).some((t) => t.includes(needle));
}

function messageStartsWith(message: UIMessage, prefix: string): boolean {
  return getTextParts(message).some((t) => t.startsWith(prefix));
}

export function ChatInterface({
  messages,
  input,
  handleInputChange,
  handleSubmit,
  isLoading,
  isInputLocked = false,
  routeGeoJson,
  onSurveySubmit,
  currentEmotion,
  chatId,
  spotifyPlaylistUrl,
}: ChatInterfaceProps) {
  const bottomRef = React.useRef<HTMLDivElement | null>(null);
  const navigate = useNavigate();

  const [typingMessageId, setTypingMessageId] = React.useState<string | null>(null);
  const [typingIndex, setTypingIndex] = React.useState(0);

  // Staggered message reveal: track which messages are visible
  const [visibleCount, setVisibleCount] = React.useState(messages.length);
  const prevLengthRef = React.useRef(messages.length);
  const timerRef = React.useRef<ReturnType<typeof setTimeout> | null>(null);

  React.useEffect(() => {
    const prevLength = prevLengthRef.current;
    const currentLength = messages.length;
    prevLengthRef.current = currentLength;

    // Messages were removed or reset - show all immediately
    if (currentLength <= prevLength) {
      setVisibleCount(currentLength);
      return;
    }

    // New messages arrived
    const newCount = currentLength - prevLength;

    // If only 1 new message, show immediately
    if (newCount === 1) {
      setVisibleCount(currentLength);
      return;
    }

    // Multiple new messages - reveal one by one
    // First, keep visibleCount at prevLength (don't show new ones yet)
    // Then reveal them with delay

    const revealNext = (targetCount: number) => {
      if (targetCount > currentLength) return;

      setVisibleCount(targetCount);

      if (targetCount < currentLength) {
        timerRef.current = setTimeout(() => revealNext(targetCount + 1), 1000);
      }
    };

    // Start revealing from the first new message
    timerRef.current = setTimeout(() => revealNext(prevLength + 1), 100);

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, [messages.length]);

  // Get only visible messages
  const visibleMessages = React.useMemo(() => {
    return messages.slice(0, visibleCount);
  }, [messages, visibleCount]);

  const chatKey = chatId ?? "__global__";
  const [dismissedUrl, setDismissedUrl] = React.useState<Record<string, string | null>>({});

  const latestSpotifyUrl = React.useMemo(
    () => spotifyPlaylistUrl || extractLatestSpotifyUrlFromMessages(messages),
    [messages, spotifyPlaylistUrl]
  );

  const spotifyUrlForThisChat = React.useMemo(() => {
    const url = latestSpotifyUrl ?? null;
    if (!url) return null;

    const dismissed = dismissedUrl[chatKey] ?? null;
    if (dismissed && dismissed === url) return null;

    return url;
  }, [chatKey, dismissedUrl, latestSpotifyUrl]);

  const lastAssistantMessage = React.useMemo(() => {
    for (let i = messages.length - 1; i >= 0; i--) {
      if (messages[i].role !== "user") return messages[i];
    }
    return null;
  }, [messages]);

  const surveyState = React.useMemo(() => {
    let lastTriggerIndex = -1;

    for (let i = messages.length - 1; i >= 0; i--) {
      if (messageContains(messages[i], "[SURVEY_FORM_TRIGGER]")) {
        lastTriggerIndex = i;
        break;
      }
    }

    if (lastTriggerIndex === -1) {
      return {
        lastTriggerIndex: -1,
        lastTriggerId: null as string | null,
        shouldShowForm: false,
      };
    }

    const lastTriggerId = messages[lastTriggerIndex]?.id ?? null;

    let completedAfterTrigger = false;
    for (let i = lastTriggerIndex + 1; i < messages.length; i++) {
      const m = messages[i];
      if (messageStartsWith(m, "[SURVEY_DATA]")) {
        completedAfterTrigger = true;
        break;
      }
      if (messageContains(m, "[ROUTE_MAP]") || messageContains(m, "[ROUTE_CARDS]")) {
        completedAfterTrigger = true;
        break;
      }
    }

    return {
      lastTriggerIndex,
      lastTriggerId,
      shouldShowForm: !completedAfterTrigger,
    };
  }, [messages]);

  React.useEffect(() => {
    if (!lastAssistantMessage) {
      setTypingMessageId(null);
      setTypingIndex(0);
      return;
    }

    const getAnimatableText = (message: UIMessage): string => {
      return message.parts
        .filter((p) => p.type === "text")
        .map((p) => (p.type === "text" ? p.text : ""))
        .filter((text) => {
          if (!text) return false;
          if (text.includes("[SURVEY_FORM_TRIGGER]")) return false;
          if (text.startsWith("[SURVEY_DATA]")) return false;
          if (text.includes("[ROUTE_CARDS]")) return false;
          return !text.includes("[ROUTE_MAP]");
        })
        .join("");
    };

    if (lastAssistantMessage.id !== typingMessageId) {
      const fullText = getAnimatableText(lastAssistantMessage);
      if (!fullText.length) {
        setTypingMessageId(null);
        setTypingIndex(0);
        return;
      }
      setTypingMessageId(lastAssistantMessage.id);
      setTypingIndex(0);
    }
  }, [lastAssistantMessage, typingMessageId]);

  React.useEffect(() => {
    if (!typingMessageId || !lastAssistantMessage) return;

    const fullText = lastAssistantMessage.parts
      .filter((p) => p.type === "text")
      .map((p) => (p.type === "text" ? p.text : ""))
      .join("");

    if (!fullText.length) return;

    const interval = setInterval(() => {
      setTypingIndex((prev) => {
        const next = prev + 2;
        if (next >= fullText.length) {
          clearInterval(interval);
          return fullText.length;
        }
        return next;
      });
    }, 20);

    return () => clearInterval(interval);
  }, [typingMessageId, lastAssistantMessage]);

  // Smoothly scroll the bottom marker into view within the scrollable container
  const smoothScrollToBottom = React.useCallback(() => {
    if (!bottomRef.current) return;

    bottomRef.current.scrollIntoView({
      behavior: "smooth",
      block: "end",
    });
  }, []);

  React.useEffect(() => {
    smoothScrollToBottom();
  }, [visibleMessages.length, typingIndex, smoothScrollToBottom]);

  const renderMessageParts = (message: UIMessage) => {
    const isTypingMessage = message.id === typingMessageId;
    let typedCharsLeft = typingIndex;

    return message.parts.map((part, idx) => {
      if (part.type === "text") {
        const text = part.text || "";

        if (text.includes("[SURVEY_FORM_TRIGGER]")) {
          const isLastTrigger = message.id === surveyState.lastTriggerId;
          if (!isLastTrigger || !surveyState.shouldShowForm) return null;

          return (
            <div key={idx} className="mt-4">
              <SurveyForm
                onSubmit={async (data) => {
                  if (onSurveySubmit) await onSurveySubmit(data);
                }}
              />
            </div>
          );
        }

        if (text.startsWith("[SURVEY_DATA]")) {
          try {
            const jsonStr = text.replace("[SURVEY_DATA]", "").trim();
            const data = JSON.parse(jsonStr);
            return (
              <div key={idx} className="mt-4">
                <SurveyForm readOnly initialData={data} />
              </div>
            );
          } catch {
            return (
              <div key={idx} className="prose prose-sm dark:prose-invert whitespace-pre-wrap">
                <ReactMarkdown>{text}</ReactMarkdown>
              </div>
            );
          }
        }

        if (text.includes("[ROUTE_MAP]")) {
          const jsonStr = text.replace("[ROUTE_MAP]", "").trim();
          let dataFromMessage: FeatureCollection | null = null;

          try {
            dataFromMessage = JSON.parse(jsonStr) as FeatureCollection;
          } catch {
            // ignore
          }

          const mapData = dataFromMessage || routeGeoJson || null;

          return mapData ? (
            <RecommendedRouteMap data={mapData} emotion={currentEmotion ?? undefined} />
          ) : (
            <div className="rounded-lg border bg-muted/60 px-4 py-3 text-sm text-muted-foreground">
              We couldn't display the route map. Please try submitting the survey again.
            </div>
          );
        }

        if (text.includes("[ROUTE_CARDS]")) {
          try {
            const jsonStr = text.replace("[ROUTE_CARDS]", "").trim();
            const routes = JSON.parse(jsonStr);
            return (
              <div key={idx} className="mt-4 w-full flex justify-center">
                <div className="w-full flex justify-center">
                  <div className="w-fit max-w-full">
                    <RouteCarousel
                      routes={routes}
                      onRouteClick={(route) => navigate("/route-details", { state: route })}
                    />
                  </div>
                </div>
              </div>
            );
          } catch {
            return null;
          }
        }

        if (!isTypingMessage || !typingMessageId) {
          return (
            <div key={idx} className="prose prose-sm whitespace-pre-wrap">
              <ReactMarkdown>{text}</ReactMarkdown>
            </div>
          );
        }

        if (typedCharsLeft <= 0) return <div key={idx} />;

        const slice = typedCharsLeft >= text.length ? text : text.slice(0, typedCharsLeft);
        typedCharsLeft = Math.max(typedCharsLeft - text.length, 0);

        return (
          <div key={idx} className="prose prose-sm whitespace-pre-wrap">
            <ReactMarkdown>{slice}</ReactMarkdown>
          </div>
        );
      }

      if (part.type === "reasoning") {
        return (
          <pre key={idx} className="whitespace-pre-wrap text-xs text-muted-foreground">
            {part.text}
          </pre>
        );
      }

      return null;
    });
  };

  const { state, isMobile } = useSidebar();
  const fixedBarStyle = !isMobile
    ? {
      left: `var(${state === "expanded" ? "--sidebar-width" : "--sidebar-width-icon"})`,
    }
    : undefined;

  return (
    <div className="bg-white relative flex flex-1 flex-col">
      <ScrollArea className="flex-1 p-6 pb-32">
        <div className="mx-auto max-w-6xl space-y-4">
          {visibleMessages.map((message) => {
            const isUser = message.role === "user";
            const renderedParts = renderMessageParts(message).filter(
              (part) => part !== null && part !== undefined
            );
            if (renderedParts.length === 0) return null;

            const isMapBubble = message.parts.some(
              (p) => p.type === "text" && (p.text || "").includes("[ROUTE_MAP]")
            );

            const isRouteCardsBubble = message.parts.some(
              (p) => p.type === "text" && (p.text || "").includes("[ROUTE_CARDS]")
            );

            if (isRouteCardsBubble) {
              return (
                <div key={message.id} className="w-full flex justify-center">
                  <div className="w-full flex justify-center">{renderedParts}</div>
                </div>
              );
            }

            return (
              <div
                key={message.id}
                className={`flex ${isUser ? "justify-end" : "justify-start"}`}
              >
                <div
                  className={`${isMapBubble ? "w-full max-w-[900px]" : "max-w-[80%]"
                    } rounded-lg px-4 py-3 text-base ${isUser ? "!bg-blue-100 !text-black" : "border bg-muted text-foreground"
                    } `}
                >
                  <div className="space-y-1">{renderedParts}</div>
                </div>
              </div>
            );
          })}

          {isLoading && (
            <div className="flex justify-start">
              <div className="max-w-[80%] rounded-lg border bg-muted px-4 py-3 text-sm">
                <div className="flex items-center gap-2">
                  <div className="h-2 w-2 animate-bounce rounded-full bg-foreground/40" />
                  <div
                    className="h-2 w-2 animate-bounce rounded-full bg-foreground/40"
                    style={{ animationDelay: "0.1s" }}
                  />
                  <div
                    className="h-2 w-2 animate-bounce rounded-full bg-foreground/40"
                    style={{ animationDelay: "0.2s" }}
                  />
                </div>
              </div>
            </div>
          )}

          <div ref={bottomRef} />
        </div>
      </ScrollArea>

      <div
        className={`fixed bottom-10 z-50 flex justify-center transition-all duration-320 ease-in-out ${isMobile ? "left-3 right-3" : "right-4"
          }`}
        style={fixedBarStyle}
      >
        <form onSubmit={handleSubmit} className="w-full max-w-3xl">
          <div
            className={`
              flex items-center gap-3
              rounded-full border border-black/10 px-4 py-2.5
              dark:border-white/10
              ${isInputLocked
                ? "opacity-60 cursor-not-allowed bg-gray-50 dark:bg-zinc-800"
                : "bg-white dark:bg-[#303030]"
              }
            `}
          >
            <Input
              value={input}
              onChange={handleInputChange}
              placeholder={isInputLocked ? "Trip generated" : "Ask anything"}
              className="
                h-auto flex-1 border-0 bg-transparent px-0
                text-sm
                shadow-none
                focus-visible:ring-0 focus-visible:ring-offset-0
              "
              disabled={isLoading || isInputLocked}
            />

            <Button
              type="submit"
              size="icon"
              disabled={!input.trim() || isLoading || isInputLocked}
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

      {
        spotifyUrlForThisChat ? (
          <SpotifyVinylMiniCard
            url={spotifyUrlForThisChat}
            title="Moodtrip playlist"
            subtitle="Open in Spotify"
            onClose={() => {
              setDismissedUrl((prev) => ({
                ...prev,
                [chatKey]: spotifyUrlForThisChat,
              }));
            }}
          />
        ) : null}
    </div >
  );
}
