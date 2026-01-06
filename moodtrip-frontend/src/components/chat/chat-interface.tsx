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
import { SurveyForm } from "./survey-form";
import type { SurveyData } from "@/api/conversation";

interface ChatInterfaceProps {
  messages: UIMessage[];
  input: string;
  handleInputChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  handleSubmit: (e: React.FormEvent<HTMLFormElement>) => void;
  isLoading: boolean;
  routeGeoJson?: FeatureCollection | null;
  onSurveySubmit?: (data: SurveyData) => Promise<void> | void;
}

function RouteGrid({
  routes,
  onRouteClick,
}: {
  routes: any[];
  onRouteClick: (route: any) => void;
}) {
  const list = Array.isArray(routes) ? routes : [];

  return (
    <div className="w-full">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {list.map((r, i) => (
          <button
            key={r?.id ?? i}
            type="button"
            onClick={() => onRouteClick(r)}
            className="overflow-hidden rounded-2xl border bg-white text-left shadow-sm transition hover:shadow-md"
          >
            <div className="aspect-[16/9] w-full overflow-hidden bg-muted">
              <img
                src={r?.imageUrl ?? "/placeholder-route.jpg"}
                alt={r?.title ?? "Route"}
                className="h-full w-full object-cover"
                loading="lazy"
              />
            </div>

            <div className="p-4">
              <div className="text-base font-semibold line-clamp-1">
                {r?.title ?? `Recommended Route ${i + 1}`}
              </div>

              <div className="mt-1 text-sm text-muted-foreground line-clamp-2">
                {r?.description ??
                  "A personalized route based on your mood."}
              </div>

              {(typeof r?.distanceMeters === "number" ||
                typeof r?.durationSeconds === "number") && (
                <div className="mt-3 text-xs text-muted-foreground">
                  {typeof r?.distanceMeters === "number"
                    ? `${Math.round(
                        (r.distanceMeters / 1000) * 10
                      ) / 10} km`
                    : null}
                  {typeof r?.distanceMeters === "number" &&
                  typeof r?.durationSeconds === "number"
                    ? " · "
                    : null}
                  {typeof r?.durationSeconds === "number"
                    ? `${Math.round(r.durationSeconds / 60)} min`
                    : null}
                </div>
              )}
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}

export function ChatInterface({
  messages,
  input,
  handleInputChange,
  handleSubmit,
  isLoading,
  routeGeoJson,
  onSurveySubmit,
}: ChatInterfaceProps) {
  const bottomRef = React.useRef<HTMLDivElement | null>(null);
  const navigate = useNavigate();
  const { state, isMobile } = useSidebar();

  const [typingMessageId, setTypingMessageId] =
    React.useState<string | null>(null);
  const [typingIndex, setTypingIndex] = React.useState(0);

  const lastAssistantMessage = React.useMemo(() => {
    const reversed = [...messages].reverse();
    return reversed.find((m) => m.role !== "user") ?? null;
  }, [messages]);

  const hasRouteResponse = React.useMemo(() => {
    if (routeGeoJson) return true;
    return messages.some((m) =>
      m.parts.some(
        (p) =>
          p.type === "text" &&
          typeof p.text === "string" &&
          p.text.includes("[ROUTE_CARDS]")
      )
    );
  }, [messages, routeGeoJson]);

  const lastSurveyTriggerId = React.useMemo(() => {
    const reversed = [...messages].reverse();
    const triggerMessage = reversed.find((m) =>
      m.parts.some(
        (p) =>
          p.type === "text" &&
          typeof p.text === "string" &&
          p.text.startsWith("[SURVEY_FORM_TRIGGER")
      )
    );
    return triggerMessage?.id ?? null;
  }, [messages]);

  React.useEffect(() => {
    if (!lastAssistantMessage) {
      setTypingMessageId(null);
      setTypingIndex(0);
      return;
    }

    const animatableText = lastAssistantMessage.parts
      .filter((p) => p.type === "text")
      .map((p) => (p.type === "text" ? p.text : ""))
      .filter(
        (t) =>
          t &&
          !t.includes("[SURVEY_FORM_TRIGGER]") &&
          !t.startsWith("[SURVEY_DATA]") &&
          !t.includes("[ROUTE_CARDS]") &&
          !t.includes("[ROUTE_MAP]")
      )
      .join("");

    if (lastAssistantMessage.id !== typingMessageId) {
      if (!animatableText.length) {
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

  React.useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, typingIndex]);

  const renderMessageParts = (message: UIMessage) => {
    const isTyping = message.id === typingMessageId;
    let remaining = typingIndex;

    return message.parts.map((part, idx) => {
      if (part.type !== "text") return null;
      const text = part.text ?? "";

      if (text.includes("[SURVEY_FORM_TRIGGER]")) {
        if (message.id !== lastSurveyTriggerId || hasRouteResponse)
          return null;
        return (
          <SurveyForm
            key={idx}
            onSubmit={(data) => onSurveySubmit?.(data)}
          />
        );
      }

      if (text.startsWith("[SURVEY_DATA]")) {
        try {
          const data = JSON.parse(text.replace("[SURVEY_DATA]", "").trim());
          return (
            <SurveyForm
              key={idx}
              readOnly
              initialData={data}
            />
          );
        } catch {
          return null;
        }
      }

      if (text.includes("[ROUTE_MAP]")) return null;

      if (text.includes("[ROUTE_CARDS]")) {
        try {
          const routes = JSON.parse(
            text.replace("[ROUTE_CARDS]", "").trim()
          );
          return (
            <RouteGrid
              key={idx}
              routes={routes}
              onRouteClick={(r) =>
                navigate("/route-details", { state: r })
              }
            />
          );
        } catch {
          return null;
        }
      }

      if (!isTyping) {
        return (
          <div
            key={idx}
            className="prose prose-sm whitespace-pre-wrap"
          >
            <ReactMarkdown>{text}</ReactMarkdown>
          </div>
        );
      }

      if (remaining <= 0) return null;

      const slice =
        remaining >= text.length
          ? text
          : text.slice(0, remaining);
      remaining = Math.max(remaining - text.length, 0);

      return (
        <div
          key={idx}
          className="prose prose-sm whitespace-pre-wrap"
        >
          <ReactMarkdown>{slice}</ReactMarkdown>
        </div>
      );
    });
  };

  const fixedBarStyle =
    !isMobile
      ? {
          left: `var(${
            state === "expanded"
              ? "--sidebar-width"
              : "--sidebar-width-icon"
          })`,
        }
      : undefined;

  return (
    <div className="relative flex flex-1 flex-col bg-white">
      <ScrollArea className="flex-1 p-6 pb-32">
        <div className="w-full space-y-4">
          {messages.map((message) => {
            const isUser = message.role === "user";
            const isRouteCards = message.parts.some(
              (p) =>
                p.type === "text" &&
                p.text.includes("[ROUTE_CARDS]")
            );

            const rendered = renderMessageParts(message).filter(
              Boolean
            );
            if (!rendered.length) return null;

            if (isRouteCards) {
              return (
                <div key={message.id} className="w-full">
                  {rendered}
                </div>
              );
            }

            return (
              <div
                key={message.id}
                className={`mx-auto flex w-full max-w-3xl ${
                  isUser ? "justify-end" : "justify-start"
                }`}
              >
                <div
                  className={`rounded-lg px-4 py-3 text-base ${
                    isUser
                      ? "!bg-blue-100 !text-black"
                      : "border bg-muted text-foreground"
                  }`}
                >
                  <div className="space-y-1">{rendered}</div>
                </div>
              </div>
            );
          })}

          {isLoading && (
            <div className="mx-auto flex w-full max-w-3xl justify-start">
              <div className="rounded-lg border bg-muted px-4 py-3 text-sm">
                <div className="flex gap-2">
                  <span className="h-2 w-2 animate-bounce rounded-full bg-foreground/40" />
                  <span
                    className="h-2 w-2 animate-bounce rounded-full bg-foreground/40"
                    style={{ animationDelay: "0.1s" }}
                  />
                  <span
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
        className={`fixed bottom-10 z-50 flex justify-center ${
          isMobile ? "left-3 right-3" : "right-4"
        }`}
        style={fixedBarStyle}
      >
        <form onSubmit={handleSubmit} className="w-full max-w-3xl">
          <div className="flex items-center gap-3 rounded-full border bg-white px-4 py-2.5">
            <Input
              value={input}
              onChange={handleInputChange}
              placeholder="Ask anything"
              disabled={isLoading}
              className="flex-1 border-0 bg-transparent px-0 text-sm focus-visible:ring-0"
            />
            <Button
              type="submit"
              size="icon"
              disabled={!input.trim() || isLoading}
              className="h-9 w-9 rounded-full bg-black text-white"
            >
              <Send className="h-4 w-4" />
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
