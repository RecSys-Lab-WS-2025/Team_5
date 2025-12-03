"use client";

import * as React from "react";
import ReactMarkdown from "react-markdown";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Send } from "lucide-react";
import type { UIMessage } from "@ai-sdk/react";
import { useSidebar } from "@/components/ui/sidebar";

import { SurveyForm } from "./survey-form";
import type { SurveyData } from "@/api/conversation";

// ... inside ChatInterfaceProps
interface ChatInterfaceProps {
  messages: UIMessage[];
  input: string;
  handleInputChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  handleSubmit: (e: React.FormEvent<HTMLFormElement>) => void;
  isLoading: boolean;
  onSurveySubmit?: (data: SurveyData) => void;
}

// ... inside ChatInterface component
export function ChatInterface({
  messages,
  input,
  handleInputChange,
  handleSubmit,
  isLoading,
  onSurveySubmit,
}: ChatInterfaceProps) {
  const bottomRef = React.useRef<HTMLDivElement | null>(null);

  // --- typing effect state ---
  const [typingMessageId, setTypingMessageId] = React.useState<string | null>(
    null,
  );
  const [typingIndex, setTypingIndex] = React.useState(0);

  const lastAssistantMessage = React.useMemo(() => {
    const reversed = [...messages].reverse();
    return reversed.find((m) => m.role !== "user") ?? null;
  }, [messages]);

  React.useEffect(() => {
    if (!lastAssistantMessage) {
      setTypingMessageId(null);
      setTypingIndex(0);
      return;
    }

    if (lastAssistantMessage.id !== typingMessageId) {
      const fullText = lastAssistantMessage.parts
        .filter((p) => p.type === "text")
        .map((p) => (p.type === "text" ? p.text : ""))
        .join("");

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

  // auto scroll to bottom
  React.useEffect(() => {
    if (bottomRef.current) {
      bottomRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages, typingIndex]);

  const renderMessageParts = (message: UIMessage) => {
    const isTypingMessage = message.id === typingMessageId;
    let typedCharsLeft = typingIndex;

    return message.parts.map((part, idx) => {
      if (part.type === "text") {
        const text = part.text;

        // Check for Survey Trigger
        if (text.includes("[SURVEY_FORM_TRIGGER]")) {
          console.log("Rendering Survey Trigger");
          return (
            <div key={idx} className="mt-4">
              <SurveyForm onSubmit={(data) => {
                console.log("Survey Data:", data);
                if (onSurveySubmit) onSurveySubmit(data);
              }} />
            </div>
          );
        }

        // Check for Persisted Survey Data
        if (text.startsWith("[SURVEY_DATA]")) {
          try {
            const jsonStr = text.replace("[SURVEY_DATA]", "").trim();
            const data = JSON.parse(jsonStr);
            return (
              <div key={idx} className="mt-4">
                <SurveyForm readOnly initialData={data} />
              </div>
            );
          } catch (e) {
            console.error("Failed to parse survey data", e);
            // Fallback to text if parsing fails
            return (
              <div
                key={idx}
                className="prose prose-sm dark:prose-invert whitespace-pre-wrap"
              >
                <ReactMarkdown>{text}</ReactMarkdown>
              </div>
            );
          }
        }

        // not the one currently playing typing animation
        if (!isTypingMessage || !typingMessageId) {
          return (
            <div
              key={idx}
              className="prose prose-sm dark:prose-invert whitespace-pre-wrap"
            >
              <ReactMarkdown>{text}</ReactMarkdown>
            </div>
          );
        }

        // ... (rest of typing logic)


        // typing animation: nothing left to show
        if (typedCharsLeft <= 0) {
          return <div key={idx} />;
        }

        const slice =
          typedCharsLeft >= text.length ? text : text.slice(0, typedCharsLeft);
        typedCharsLeft = Math.max(typedCharsLeft - text.length, 0);

        return (
          <div
            key={idx}
            className="prose prose-sm dark:prose-invert whitespace-pre-wrap"
          >
            <ReactMarkdown>{slice}</ReactMarkdown>
          </div>
        );
      }

      if (part.type === "reasoning") {
        return (
          <pre
            key={idx}
            className="whitespace-pre-wrap text-xs text-muted-foreground"
          >
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
      left: `var(${state === "expanded" ? "--sidebar-width" : "--sidebar-width-icon"
        })`,
    }
    : undefined;

  return (
    <div className="relative flex flex-1 flex-col">
      <ScrollArea className="flex-1 p-6 pb-32">
        <div className="mx-auto max-w-3xl space-y-4">
          {messages.map((message) => {
            const isUser = message.role === "user";

            return (
              <div
                key={message.id}
                className={`flex ${isUser ? "justify-end" : "justify-start"}`}
              >
                <div
                  className={`max-w-[80%] rounded-lg px-4 py-3 text-base ${isUser
                    ? "bg-emerald-500 text-white dark:bg-emerald-500 dark:text-white"
                    : "border bg-muted text-foreground"
                    }`}
                >
                  <div className="space-y-1">{renderMessageParts(message)}</div>
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

      {/* bottom input bar */}
      <div
        className={`
          fixed bottom-10 z-50 flex justify-center
          transition-all duration-320 ease-in-out
          ${isMobile ? "left-3 right-3" : "right-4"}
        `}
        style={fixedBarStyle}
      >
        <form onSubmit={handleSubmit} className="w-full max-w-3xl">
          <div
            className="
              flex items-center gap-3
              rounded-full border border-black/10 bg-background px-4 py-2.5
              dark:border-white/10 dark:bg-[#303030]
            "
          >
            <Input
              value={input}
              onChange={handleInputChange}
              placeholder="Ask anything"
              className="
                h-auto flex-1 border-0 bg-transparent px-0
                text-sm
                shadow-none
                focus-visible:ring-0 focus-visible:ring-offset-0
              "
              disabled={isLoading}
            />

            <Button
              type="submit"
              size="icon"
              disabled={!input.trim() || isLoading}
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
  );
}
