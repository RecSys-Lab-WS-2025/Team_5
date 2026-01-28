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
  historyRenderToken?: number;
  processingMessage?: string | null;
}

function SpotifyVinylMiniCard({
  url,
  onClose,
  title = "Moodtrip playlist",
}: {
  url: string;
  onClose: () => void;
  title?: string;
}) {
  return (
    <>
      <div
        className="
          fixed top-[30%] -translate-y-1/2 right-6 z-[100]
          w-[220px] 
          rounded-[22px] border border-blue-100/50
          bg-blue-50/60 backdrop-blur-2xl
          shadow-[0_15px_35px_rgba(0,40,80,0.1)]
          overflow-hidden group transition-all duration-500
        "
        style={{
          animation: "playlist-slide-in 600ms cubic-bezier(0.16, 1, 0.3, 1)",
        }}
      >
        <div className="absolute -right-4 -top-4 w-20 h-20 bg-blue-200/20 blur-[30px] rounded-full" />

        <div className="relative p-3">
          <button
            type="button"
            onClick={onClose}
            className="
              absolute right-2 top-2 z-10
              h-5 w-5 rounded-full
              grid place-items-center
              bg-blue-900/5 hover:bg-blue-900/10 transition-colors
            "
            aria-label="Close"
          >
            <span className="text-[16px] leading-none text-blue-900/40">×</span>
          </button>

          <button
            type="button"
            onClick={() => window.open(url, "_blank", "noopener,noreferrer")}
            className="w-full text-left outline-none"
          >
            <div className="flex items-center gap-3">
              <div className="relative h-14 w-14 shrink-0 group-hover:scale-105 transition-transform duration-500">
                <div className="absolute inset-0 rounded-full bg-blue-900/10 blur-sm" />

                <div
                  className="absolute inset-0 rounded-full animate-spin"
                  style={{
                    animationDuration: "3s",
                    animationTimingFunction: "linear",
                    animationIterationCount: "infinite",
                    boxShadow: "0 4px 12px rgba(0,20,50,0.2)",
                  }}
                  aria-hidden="true"
                >
                  <div className="absolute inset-0 rounded-full bg-gradient-to-br from-neutral-800 via-slate-900 to-neutral-950" />

                  <div className="absolute inset-[3px] rounded-full border-[0.5px] border-white/5" />
                  <div className="absolute inset-[6px] rounded-full border-[0.5px] border-white/5" />
                  <div className="absolute inset-[9px] rounded-full border-[0.5px] border-white/5" />
                  <div
                    className="absolute inset-0 rounded-full"
                    style={{
                      background:
                        "conic-gradient(from 0deg, transparent 0%, rgba(255,255,255,0.15) 15%, transparent 30%, transparent 50%, rgba(255,255,255,0.15) 65%, transparent 80%, transparent 100%)",
                    }}
                  />

                  <div
                    className="absolute inset-0 rounded-full opacity-60"
                    style={{
                      background:
                        "conic-gradient(from 90deg, transparent 0%, rgba(255,255,255,0.2) 5%, transparent 10%, transparent 100%)",
                    }}
                  />

                  <div className="absolute left-1/2 top-1/2 h-4 w-4 -translate-x-1/2 -translate-y-1/2 rounded-full bg-gradient-to-tr from-blue-100 to-slate-300 shadow-inner border border-white/30" />
                </div>

                <div className="absolute -right-0.5 top-1 w-5 h-[2px] bg-slate-400 rounded-full rotate-[25deg] shadow-sm origin-right group-hover:rotate-[15deg] transition-transform duration-500" />
              </div>

              <div className="min-w-0 flex-1 pr-4">
                <div className="text-[13px] font-bold tracking-tight text-slate-800 truncate leading-tight">
                  {title}
                </div>

                <div className="mt-2 flex items-center">
                  <div className="flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-blue-900/5 border border-blue-200/50">
                    <div className="h-1 w-1 rounded-full bg-blue-400 animate-pulse" />
                    <span className="text-[9px] font-bold uppercase tracking-wider text-slate-500">
                      Spotify
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </button>
        </div>
      </div>

      <style>{`
        @keyframes playlist-slide-in {
          from { transform: translate(30px, -50%) scale(0.95); opacity: 0; }
          to { transform: translate(0, -50%) scale(1); opacity: 1; }
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

      const md = t.match(/\[([^\]]*)\]\((https?:\/\/open\.spotify\.com\/[^)\s]+)\)/i);
      if (md?.[2]) return md[2];

      const mdLoose = t.match(/\[([^\]]*)\]\((https?:\/\/open\.spotify\.com\/[^)]+)\)/i);
      if (mdLoose?.[2]) return mdLoose[2].trim();

      const paren = t.match(/\((https?:\/\/open\.spotify\.com\/[^)\s]+)\)/i);
      if (paren?.[1]) return paren[1];

      const raw = t.match(/https?:\/\/open\.spotify\.com\/[^\s)\n]+/i);
      if (raw?.[0]) return raw[0];

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
  historyRenderToken = 0,
  processingMessage,
}: ChatInterfaceProps) {
  const navigate = useNavigate();

  const scrollAreaRootRef = React.useRef<HTMLDivElement | null>(null);
  const bottomSentinelRef = React.useRef<HTMLDivElement | null>(null);
  const inputBarRef = React.useRef<HTMLDivElement | null>(null);
  const [inputBarH, setInputBarH] = React.useState(120);

  const scrollToBottom = React.useCallback((smooth: boolean) => {
    const el = bottomSentinelRef.current;
    if (!el) return;
    el.scrollIntoView({ behavior: smooth ? "smooth" : "auto", block: "end" });
  }, []);

  React.useEffect(() => {
    const el = inputBarRef.current;
    if (!el) return;

    const ro = new ResizeObserver(() => {
      const h = el.getBoundingClientRect().height;
      if (Number.isFinite(h) && h > 0) setInputBarH(Math.ceil(h));
    });
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const prevMsgLenRef = React.useRef(messages.length);
  React.useEffect(() => {
    const prevLen = prevMsgLenRef.current;
    const curLen = messages.length;
    prevMsgLenRef.current = curLen;

    if (curLen > prevLen) {
      requestAnimationFrame(() => scrollToBottom(true));
    }
  }, [messages.length, scrollToBottom]);

  const [typingMessageId, setTypingMessageId] = React.useState<string | null>(null);
  const [typingIndex, setTypingIndex] = React.useState(0);

  const [visibleCount, setVisibleCount] = React.useState(messages.length);
  const prevLengthRef = React.useRef(messages.length);
  const timerRef = React.useRef<ReturnType<typeof setTimeout> | null>(null);

  const prevHistoryTokenRef = React.useRef(historyRenderToken);

  React.useEffect(() => {
    if (prevHistoryTokenRef.current !== historyRenderToken) {
      prevHistoryTokenRef.current = historyRenderToken;

      if (timerRef.current) {
        clearTimeout(timerRef.current);
        timerRef.current = null;
      }

      prevLengthRef.current = messages.length;
      setVisibleCount(messages.length);

      requestAnimationFrame(() => scrollToBottom(false));
      return;
    }

    const prevLength = prevLengthRef.current;
    const currentLength = messages.length;
    prevLengthRef.current = currentLength;

    if (currentLength <= prevLength) {
      setVisibleCount(currentLength);
      return;
    }

    const newCount = currentLength - prevLength;

    if (newCount === 1) {
      setVisibleCount(currentLength);
      return;
    }

    const revealNext = (targetCount: number) => {
      if (targetCount > currentLength) return;
      setVisibleCount(targetCount);
      if (targetCount < currentLength) {
        timerRef.current = setTimeout(() => revealNext(targetCount + 1), 1000);
      }
    };

    timerRef.current = setTimeout(() => revealNext(prevLength + 1), 100);

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, [messages.length, historyRenderToken, scrollToBottom]);

  const visibleMessages = React.useMemo(() => {
    return messages.slice(0, visibleCount);
  }, [messages, visibleCount]);

  React.useEffect(() => {
    requestAnimationFrame(() => scrollToBottom(true));
  }, [visibleCount, isLoading, inputBarH, scrollToBottom]);

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
              <ReactMarkdown
                components={{
                  a: ({ children, href, ...props }) => (
                    <a
                      href={href}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="font-semibold underline decoration-2"
                      style={{
                        color: href?.includes('spotify.com') ? '#1DB954' : '#2563eb',
                      }}
                      {...props}
                    >
                      {children}
                    </a>
                  ),
                }}
              >
                {text}
              </ReactMarkdown>
            </div>
          );
        }

        if (typedCharsLeft <= 0) return <div key={idx} />;

        const slice = typedCharsLeft >= text.length ? text : text.slice(0, typedCharsLeft);
        typedCharsLeft = Math.max(typedCharsLeft - text.length, 0);

        return (
          <div key={idx} className="prose prose-sm whitespace-pre-wrap">
            <ReactMarkdown
              components={{
                a: ({ children, href, ...props }) => (
                  <a
                    href={href}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="font-semibold underline decoration-2"
                    style={{
                      color: href?.includes('spotify.com') ? '#1DB954' : '#2563eb',
                    }}
                    {...props}
                  >
                    {children}
                  </a>
                ),
              }}
            >
              {slice}
            </ReactMarkdown>
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

  const bottomGap = inputBarH + 28;

  const onSubmitWrapped = (e: React.FormEvent<HTMLFormElement>) => {
    handleSubmit(e);
    requestAnimationFrame(() => scrollToBottom(true));
  };

  return (
    <div className="bg-white relative flex flex-1 flex-col">
      <ScrollArea
        className="flex-1"
      >
        <div className="p-6" style={{ paddingBottom: bottomGap }}>
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

            <div ref={bottomSentinelRef} />
          </div>
        </div>
      </ScrollArea>


      <div
        className={`fixed bottom-10 z-50 flex justify-center transition-all duration-300 ease-in-out ${isMobile ? "left-3 right-3" : "right-4"
          }`}
        style={fixedBarStyle}
      >
        <div className="w-full max-w-3xl flex flex-col items-center gap-2">
          {processingMessage && (
            <div
              className="
                px-4 py-1.5 
                bg-muted/80 backdrop-blur-sm border border-border/50
                text-xs font-medium text-muted-foreground
                rounded-full shadow-sm
                animate-pulse
              "
            >
              {processingMessage}
            </div>
          )}

          <form onSubmit={onSubmitWrapped} className="w-full">
            <div
              className="
                flex items-center gap-3
                rounded-full border border-black/10 bg-white px-4 py-2.5
                dark:border-white/10 dark:bg-[#303030]
              "
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
      </div>

      {spotifyUrlForThisChat ? (
        <SpotifyVinylMiniCard
          url={spotifyUrlForThisChat}
          title="Moodtrip playlist"
          onClose={() => {
            setDismissedUrl((prev) => ({
              ...prev,
              [chatKey]: spotifyUrlForThisChat,
            }));
          }}
        />
      ) : null}
    </div>
  );
}
