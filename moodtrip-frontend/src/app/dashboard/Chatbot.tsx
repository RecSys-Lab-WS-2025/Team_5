"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import type { UIMessage } from "@ai-sdk/react";
import { DeleteConfirmDialog } from "@/components/dialog/delete-confirm-dialog";
import { RenameChatDialog } from "@/components/dialog/rename-chat-dialog";
import { AppSidebar } from "@/components/sidebar/app-sidebar";
import type { ChatSummary } from "@/components/sidebar/app-sidebar";
import { WelcomeScreen } from "@/components/chat/welcome-screen";
import { ChatInterface } from "@/components/chat/chat-interface";
import type { SurveyData } from "@/api/conversation";
import {
  SidebarInset,
  SidebarProvider,
  SidebarTrigger,
} from "@/components/ui/sidebar";

import { Button } from "@/components/ui/button";
import {
  Laugh,
  Smile,
  Meh,
  Annoyed,
  Send,
  SquareTerminal,
  House,
} from "lucide-react";

import { getUser } from "@/api/auth";
import { onUserUpdated } from "@/lib/user-events";
import { normalizePoiCategories } from "@/lib/poi-categories";
import {
  startConversation,
  getMyConversations,
  getConversationMessages,
  sendMessage as apiSendMessage,
  extractEmotion as apiExtractEmotion,
  submitSurvey,
  renameConversation,
  deleteConversation,
} from "@/api/conversation";
import type { RouteRecommendation, AppRouteType } from "@/api/conversation";
import type {
  Feature,
  FeatureCollection,
  Geometry,
  GeoJsonProperties,
} from "geojson";
import { useLocation, useNavigate } from "react-router-dom";

type AnyFeature = Feature<Geometry, GeoJsonProperties>;

type RouteProperties = {
  type?: "route";
  name?: string;
  dayDescriptions?: Record<string, string>;
  image?: string;
  distanceMeters?: number;
  durationSeconds?: number;
  routeType?: AppRouteType;
  routeTypeTitle?: string;
  routeTypeDescription?: string;
};

type PoiProperties = {
  type?: "poi";
  imageUrl?: string;
};

function isRouteFeature(f: AnyFeature): f is Feature<Geometry, RouteProperties> {
  const p = f.properties as RouteProperties | null | undefined;
  return !!p && p.type === "route";
}

function isPoiFeature(f: AnyFeature): f is Feature<Geometry, PoiProperties> {
  const p = f.properties as PoiProperties | null | undefined;
  return !!p && p.type === "poi";
}

const navData = {
  user: {
    name: "John Doe",
    email: "johndoe@example.com",
    avatar: "/avatars/john.jpg",
  },
  navMain: [
    {
      title: "Get Started",
      url: "#",
      icon: SquareTerminal,
      isActive: true,
      items: [
        { title: "Introduction", url: "#" },
        { title: "Quick-Start", url: "#" },
      ],
    },
  ],
  navSecondary: [
    { title: "Feedback", url: "/contact", icon: Send },
  ],
};

const CHAT_SNAPSHOT_KEY = "moodtrip:chatSnapshot";

type ChatSnapshot = {
  selectedChatId: string | null;
  messages: UIMessage[];
  emotionExtracted: boolean;
  currentEmotion: string | null;
  routeGeoJson: FeatureCollection | null;
};

type ChatLocationState = { resetChat?: boolean } | null;

type SubmitSurveyResponse = Awaited<ReturnType<typeof submitSurvey>>;
type SubmitSurveyResponseWithSpotify = SubmitSurveyResponse & {
  spotifyPlaylistLink?: string | null;
};

function readSnapshot(): ChatSnapshot | null {
  try {
    const raw = sessionStorage.getItem(CHAT_SNAPSHOT_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as ChatSnapshot;
    if (!parsed || typeof parsed !== "object") return null;
    return parsed;
  } catch {
    return null;
  }
}

function writeSnapshot(snapshot: ChatSnapshot) {
  try {
    sessionStorage.setItem(CHAT_SNAPSHOT_KEY, JSON.stringify(snapshot));
  } catch {
    // ignore
  }
}

function clearSnapshot() {
  try {
    sessionStorage.removeItem(CHAT_SNAPSHOT_KEY);
  } catch {
    // ignore
  }
}

function isFiniteNumber(v: unknown): v is number {
  return typeof v === "number" && Number.isFinite(v);
}

function isValidLatLng(lat: unknown, lng: unknown) {
  if (!isFiniteNumber(lat) || !isFiniteNumber(lng)) return false;
  if (lat === 0 && lng === 0) return false;
  if (Math.abs(lat) > 90 || Math.abs(lng) > 180) return false;
  return true;
}

async function getBrowserLocation(
  timeoutMs = 8000
): Promise<{ latitude: number; longitude: number } | null> {
  if (typeof window === "undefined") return null;
  if (!("geolocation" in navigator)) return null;

  return await new Promise((resolve) => {
    const timer = window.setTimeout(() => resolve(null), timeoutMs);

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        window.clearTimeout(timer);
        const latitude = pos.coords.latitude;
        const longitude = pos.coords.longitude;
        if (!isValidLatLng(latitude, longitude)) return resolve(null);
        resolve({ latitude, longitude });
      },
      () => {
        window.clearTimeout(timer);
        resolve(null);
      },
      { enableHighAccuracy: false, maximumAge: 60_000, timeout: timeoutMs }
    );
  });
}

function normalizeRangeMeters(rangeMeters: unknown) {
  if (isFiniteNumber(rangeMeters)) {
    return Math.max(500, Math.min(50_000, Math.round(rangeMeters)));
  }
  return 3000;
}

function normalizeCategories(cats: unknown): string[] {
  return normalizePoiCategories(cats);
}

export default function Chatbot() {
  const location = useLocation();
  const navigate = useNavigate();

  const initialSnapshot = (() => {
    if (typeof window === "undefined") return null;
    return readSnapshot();
  })();

  const [displayUserName, setDisplayUserName] = useState(() => {
    const storedUser = getUser();
    return storedUser?.username ?? navData.user.name;
  });

  useEffect(() => {
    const off = onUserUpdated(() => {
      const storedUser = getUser();
      setDisplayUserName(storedUser?.username ?? navData.user.name);
    });
    return () => off();
  }, []);

  const [selectedChatId, setSelectedChatId] = useState<string | null>(
    initialSnapshot?.selectedChatId ?? null
  );
  const [chats, setChats] = useState<ChatSummary[]>([]);
  const [messages, setMessages] = useState<UIMessage[]>(
    initialSnapshot?.messages ?? []
  );
  const [isLoading, setIsLoading] = useState(false);
  const [input, setInput] = useState("");
  const [emotionExtracted, setEmotionExtracted] = useState(
    initialSnapshot?.emotionExtracted ?? true
  );
  const [pendingChatId, setPendingChatId] = useState<string | null>(null);
  const [routeGeoJson, setRouteGeoJson] = useState<FeatureCollection | null>(
    initialSnapshot?.routeGeoJson ?? null
  );
  const [currentEmotion, setCurrentEmotion] = useState<string | null>(
    initialSnapshot?.currentEmotion ?? null
  );
  const [isChatLocked, setIsChatLocked] = useState(() => {
    const snapshotMessages = initialSnapshot?.messages ?? [];
    return snapshotMessages.some(
      (message) =>
        message.role !== "user" &&
        message.parts.some(
          (part) =>
            part.type === "text" && (part.text || "").includes("[ROUTE_CARDS]")
        )
    );
  });

  const [spotifyUrlByChat, setSpotifyUrlByChat] = useState<
    Record<string, string | null>
  >({});

  const skipLoadRef = useRef(false);

  useEffect(() => {
    const state = (location.state as ChatLocationState) ?? null;
    if (state?.resetChat) {
      clearSnapshot();

      setSelectedChatId(null);
      setMessages([]);
      setEmotionExtracted(false);
      setPendingChatId(null);
      setRouteGeoJson(null);
      setCurrentEmotion(null);
      setIsChatLocked(false);

      navigate(".", { replace: true, state: null });
    }
  }, [location.state, navigate]);

  useEffect(() => {
    writeSnapshot({
      selectedChatId,
      messages,
      emotionExtracted,
      currentEmotion,
      routeGeoJson,
    });
  }, [selectedChatId, messages, emotionExtracted, currentEmotion, routeGeoJson]);

  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [chatToDelete, setChatToDelete] = useState<ChatSummary | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const [renameDialogOpen, setRenameDialogOpen] = useState(false);
  const [chatToRename, setChatToRename] = useState<ChatSummary | null>(null);
  const [isRenaming, setIsRenaming] = useState(false);

  const parseMessageContent = (content: string) => {
    if (content.startsWith("EmotionResult[")) {
      const match: RegExpMatchArray | null = content.match(
        /content=([\s\S]*?),\s*success=(true|false)/
      );
      if (match && match[1]) return match[1];
    }
    return content;
  };

  const parseEmotionResultSuccess = useCallback((content: string) => {
    if (!content.startsWith("EmotionResult[")) return null;
    const match = content.match(/success=(true|false)/i);
    if (!match) return null;
    return match[1].toLowerCase() === "true";
  }, []);

  const didEmotionExtractionSucceed = useCallback(
    (messages: Array<{ sender: string; content: string }>) => {
      let latestSuccess: boolean | null = null;
      for (const message of messages) {
        if (message.sender !== "BOT") continue;
        const success = parseEmotionResultSuccess(message.content);
        if (success !== null) latestSuccess = success;
      }
      return latestSuccess === true;
    },
    [parseEmotionResultSuccess]
  );

  const didGenerateRoute = useCallback(
    (messages: Array<{ sender: string; content: string }>) =>
      messages.some(
        (message) =>
          message.sender === "BOT" && message.content.includes("[ROUTE_CARDS]")
      ),
    []
  );

  const getEmotionIcon = (emotion?: string) => {
    if (!emotion) return undefined;
    const e = emotion.toUpperCase();
    switch (e) {
      case "JOYFUL":
      case "ENERGIZED":
        return Laugh;
      case "CALM":
        return Smile;
      case "NEUTRAL":
      case "SAD":
      case "TIRED":
        return Meh;
      case "STRESSED":
        return Annoyed;
      default:
        return undefined;
    }
  };

  const loadChats = useCallback(async () => {
    try {
      const data = await getMyConversations();
      const sorted = data.sort((a, b) => b.id - a.id);
      const mapped: ChatSummary[] = sorted.map((c) => ({
        id: c.id.toString(),
        title: c.title,
        icon: getEmotionIcon(c.emotion),
        emotion: c.emotion,
      }));
      setChats(mapped);
    } catch (e) {
      console.error("Failed to load chats", e);
    }
  }, []);

  useEffect(() => {
    loadChats();
  }, [loadChats]);

  const loadMessages = useCallback(async (chatId: string) => {
    try {
      const msgs = await getConversationMessages(Number(chatId));
      const uiMsgs: UIMessage[] = msgs.map((m) => ({
        id: m.id.toString(),
        role: m.sender === "USER" ? "user" : "assistant",
        parts: [{ type: "text", text: parseMessageContent(m.content) }],
      }));
      setMessages(uiMsgs);
      setEmotionExtracted(didEmotionExtractionSucceed(msgs));
      setIsChatLocked(didGenerateRoute(msgs));
    } catch (e) {
      console.error("Failed to load messages", e);
      setMessages([]);
      setIsChatLocked(false);
    }
  }, [didEmotionExtractionSucceed, didGenerateRoute]);

  useEffect(() => {
    if (!selectedChatId) return;

    if (skipLoadRef.current) {
      skipLoadRef.current = false;
      return;
    }

    loadMessages(selectedChatId);
    const chat = chats.find((c) => c.id === selectedChatId);
    if (chat) setCurrentEmotion(chat.emotion || null);
  }, [selectedChatId, loadMessages, chats]);

  const handleNewChat = async () => {
    setSelectedChatId(null);
    setMessages([]);
    setEmotionExtracted(false);
    setPendingChatId(null);
    setRouteGeoJson(null);
    setCurrentEmotion(null);
    setIsChatLocked(false);
    clearSnapshot();
  };

  const handleSelectChat = (chatId: string) => {
    setSelectedChatId(chatId);
    setPendingChatId(null);
    setRouteGeoJson(null);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) =>
    setInput(e.target.value);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const text = input.trim();
    if (!text || !selectedChatId || isChatLocked) return;

    setInput("");

    const tempId = Date.now().toString();
    const userMsg: UIMessage = {
      id: tempId,
      role: "user",
      parts: [{ type: "text", text }],
    };
    setMessages((prev) => [...prev, userMsg]);
    setIsLoading(true);

    try {
      if (!emotionExtracted) {
        const res = await apiExtractEmotion(Number(selectedChatId), text);
        if (res.success) {
          setEmotionExtracted(true);
          if (res.topLabel) setCurrentEmotion(res.topLabel);
        } else {
          setEmotionExtracted(false);
        }

        if (res.content) {
          const botMsg: UIMessage = {
            id: (Date.now() + 1).toString(),
            role: "assistant",
            parts: [{ type: "text", text: parseMessageContent(res.content) }],
          };
          setMessages((prev) => [...prev, botMsg]);
        }

        if (res.success) {
          const surveyMsg: UIMessage = {
            id: (Date.now() + 2).toString(),
            role: "assistant",
            parts: [{ type: "text", text: "[SURVEY_FORM_TRIGGER]" }],
          };
          setMessages((prev) => [...prev, surveyMsg]);
          try {
            await apiSendMessage(
              Number(selectedChatId),
              "[SURVEY_FORM_TRIGGER]",
              false
            );
          } catch (err) {
            console.error("Failed to persist survey form trigger", err);
          }
        }
      } else {
        await apiSendMessage(Number(selectedChatId), text, true);
      }
    } catch (e) {
      console.error("Failed to send message", e);
    } finally {
      setIsLoading(false);
    }
  };

  const showWelcome = !selectedChatId && messages.length === 0;

  const handleSuggestionClick = async (text: string) => {
    let chatId = selectedChatId || pendingChatId;

    if (!chatId) {
      try {
        const newChat = await startConversation();
        const newChatSummary: ChatSummary = {
          id: newChat.id.toString(),
          title: newChat.title,
          icon: getEmotionIcon(newChat.emotion),
        };
        setChats((prev) => [newChatSummary, ...prev]);
        chatId = newChat.id.toString();
        setEmotionExtracted(false);
        setRouteGeoJson(null);
      } catch (e) {
        console.error(e);
        return;
      }
    }

    skipLoadRef.current = true;
    setSelectedChatId(chatId);
    setPendingChatId(null);

    const tempId = Date.now().toString();
    const userMsg: UIMessage = {
      id: tempId,
      role: "user",
      parts: [{ type: "text", text }],
    };
    setMessages((prev) => [...prev, userMsg]);
    setIsLoading(true);

    try {
      const res = await apiExtractEmotion(Number(chatId), text);
      if (res.success) {
        setEmotionExtracted(true);
        if (res.topLabel) setCurrentEmotion(res.topLabel);
      } else {
        setEmotionExtracted(false);
      }

      if (res.content) {
        const botMsg: UIMessage = {
          id: (Date.now() + 1).toString(),
          role: "assistant",
          parts: [{ type: "text", text: parseMessageContent(res.content) }],
        };
        setMessages((prev) => [...prev, botMsg]);
      }

      if (res.success) {
        const surveyMsg: UIMessage = {
          id: (Date.now() + 2).toString(),
          role: "assistant",
          parts: [{ type: "text", text: "[SURVEY_FORM_TRIGGER]" }],
        };
        setMessages((prev) => [...prev, surveyMsg]);
        try {
          await apiSendMessage(Number(chatId), "[SURVEY_FORM_TRIGGER]", false);
        } catch (err) {
          console.error("Failed to persist survey form trigger", err);
        }
      }
    } catch (e) {
      console.error(e);
    } finally {
      setIsLoading(false);
    }
  };

  const handleScriptedClick = async (userText: string, assistantText: string) => {
    setIsLoading(true);

    let currentChatId = selectedChatId;

    if (!currentChatId) {
      try {
        const newChat = await startConversation();
        const newChatSummary: ChatSummary = {
          id: newChat.id.toString(),
          title: newChat.title,
          icon: getEmotionIcon(newChat.emotion),
        };
        setChats((prev) => [newChatSummary, ...prev]);
        currentChatId = newChat.id.toString();
        setEmotionExtracted(false);

        skipLoadRef.current = true;
        setSelectedChatId(currentChatId);
        setMessages([]);
      } catch (e) {
        console.error("Failed to start new conversation for scripted content", e);
        setIsLoading(false);
        return;
      }
    }

    if (!currentChatId) {
      setIsLoading(false);
      return;
    }

    const userMsg: UIMessage = {
      id: Date.now().toString(),
      role: "user",
      parts: [{ type: "text", text: userText }],
    };

    const assistantMsg: UIMessage = {
      id: (Date.now() + 1).toString(),
      role: "assistant",
      parts: [{ type: "text", text: assistantText }],
    };

    setMessages((prev) => [...prev, userMsg, assistantMsg]);
    setEmotionExtracted(true);

    try {
      await apiSendMessage(Number(currentChatId), userText, true);
      await apiSendMessage(Number(currentChatId), assistantText, false);
    } catch (e) {
      console.error("Failed to persist scripted exchange", e);
    }

    setIsLoading(false);
  };

  const handleIntroClick = () => {
    const userText = "Could you briefly introduce what this Moodtrip website does?";
    const assistantText = `Of course! 😊

**In one sentence:** Moodtrip is a tiny travel buddy that suggests same-day or short-notice trips that match your current mood.

**What Moodtrip helps with:**
- You’re not sure *where* to go, you just know *how* you feel
- You want a small reset rather than a big, complicated holiday
- You’d like ideas that feel emotionally right, not just “top rated nearby”

**What you can do next:** Choose a mood prompt from the sidebar, or just type how you’re feeling and hit **Send**.
I’ll take it from there and start shaping a Moodtrip for you 💫`;

    handleScriptedClick(userText, assistantText);
  };

  const handleQuickStartClick = () => {
    const userText =
      "How do I quickly get started using Moodtrip? Please give me a short guide.";
    const assistantText = `Let’s keep it super simple 🌈

**Quick start in 4 steps:**
1. Tell me how you feel.
2. Say what you want this trip to do.
3. Add a few basics (people, timing, starting area).
4. Press Send and just chat.

I’ll ask for anything missing and suggest a few mood-matching trip ideas 🎧✨`;

    handleScriptedClick(userText, assistantText);
  };

  const handleRenameChat = (id: string, currentTitle: string) => {
    const found = chats.find((c) => c.id === id);
    const chat: ChatSummary = found ?? {
      id,
      title: currentTitle,
      icon: undefined,
      emotion: undefined,
    };
    setChatToRename(chat);
    setRenameDialogOpen(true);
  };

  const handleConfirmRenameChat = async (newTitle: string) => {
    if (!chatToRename) return;
    setIsRenaming(true);
    try {
      const updated = await renameConversation(Number(chatToRename.id), newTitle);
      setChats((prev) =>
        prev.map((c) =>
          c.id === chatToRename.id ? { ...c, title: updated.title } : c
        )
      );
    } catch (e) {
      console.error("Failed to rename chat", e);
    } finally {
      setIsRenaming(false);
      setRenameDialogOpen(false);
      setChatToRename(null);
    }
  };

  const handleDeleteChat = (id: string) => {
    const chat = chats.find((c) => c.id === id) || null;
    setChatToDelete(chat || ({ id, title: "This chat" } as ChatSummary));
    setDeleteDialogOpen(true);
  };

  const handleConfirmDeleteChat = async () => {
    if (!chatToDelete) return;
    setIsDeleting(true);
    try {
      await deleteConversation(Number(chatToDelete.id));
      setChats((prev) => prev.filter((c) => c.id !== chatToDelete.id));

      setSpotifyUrlByChat((prev) => {
        const next = { ...prev };
        delete next[String(chatToDelete.id)];
        return next;
      });

      if (selectedChatId === chatToDelete.id) {
        setSelectedChatId(null);
        setMessages([]);
        setEmotionExtracted(false);
        setPendingChatId(null);
        setRouteGeoJson(null);
        setCurrentEmotion(null);
        setIsChatLocked(false);
        clearSnapshot();
      }
    } catch (e) {
      console.error("Failed to delete chat", e);
    } finally {
      setIsDeleting(false);
      setDeleteDialogOpen(false);
      setChatToDelete(null);
    }
  };

  const handleShareChat = async (id: string) => {
    const baseUrl = window.location.origin;
    const url = `${baseUrl}/chat/${id}`;

    try {
      if (navigator.share) {
        await navigator.share({
          title: "Moodtrip Chat",
          text: "Check out this Moodtrip conversation.",
          url,
        });
      } else if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(url);
        window.alert("Chat link copied to clipboard.");
      } else {
        window.prompt("Copy this link:", url);
      }
    } catch (e) {
      console.error("Failed to share chat", e);
    }
  };

  return (
    <SidebarProvider>
      <AppSidebar
        navMain={navData.navMain}
        navSecondary={navData.navSecondary}
        chats={chats}
        selectedChatId={selectedChatId}
        onNewChat={handleNewChat}
        onSelectChat={handleSelectChat}
        onRenameChat={handleRenameChat}
        onDeleteChat={handleDeleteChat}
        onShareChat={handleShareChat}
        onIntroductionClick={handleIntroClick}
        onQuickStartClick={handleQuickStartClick}
        onRefreshChats={loadChats}
      />

      <SidebarInset>
        <header className="sticky top-0 z-50 flex h-16 shrink-0 items-center bg-white">
          <div className="flex items-center gap-2 px-4">
            <SidebarTrigger className="-ml-1" />
            <Button
              variant="ghost"
              size="icon"
              onClick={() => {
                clearSnapshot();
                navigate("/", { replace: true });
              }}
              aria-label="Go to home"
              title="Home"
              className="
                !bg-transparent
                hover:bg-transparent
                active:bg-transparent
                focus-visible:bg-transparent
                shadow-none
              "
            >
              <House className="h-5 w-5 text-black" />
            </Button>
          </div>
        </header>

        <div className="flex min-h-[calc(100vh-4rem)] flex-1 flex-col">
          {showWelcome ? (
            <WelcomeScreen
              onSuggestionClick={handleSuggestionClick}
              userName={displayUserName}
            />
          ) : (
            <ChatInterface
              chatId={selectedChatId}
              messages={messages}
              input={input}
              handleInputChange={handleInputChange}
              handleSubmit={handleSubmit}
              isLoading={isLoading}
              isInputLocked={isChatLocked}
              routeGeoJson={routeGeoJson}
              currentEmotion={currentEmotion}
              spotifyPlaylistUrl={
                selectedChatId ? (spotifyUrlByChat[String(selectedChatId)] ?? null) : null
              }
              onSurveySubmit={async (data: SurveyData) => {
                if (!selectedChatId) return;
                const conversationId = Number(selectedChatId);

                const appendRecovery = async (messageText: string) => {
                  const triggerPayload = "[SURVEY_FORM_TRIGGER]";

                  const errorMsg: UIMessage = {
                    id: Date.now().toString(),
                    role: "assistant",
                    parts: [{ type: "text", text: messageText }],
                  };
                  const triggerMsg: UIMessage = {
                    id: (Date.now() + 1).toString(),
                    role: "assistant",
                    parts: [{ type: "text", text: triggerPayload }],
                  };
                  setMessages((prev) => [...prev, errorMsg, triggerMsg]);

                  try {
                    await apiSendMessage(conversationId, messageText, false);
                    await apiSendMessage(conversationId, triggerPayload, false);
                  } catch (err) {
                    console.error("Failed to persist recovery messages", err);
                  }
                };

                setIsLoading(true);

                let waitMsgId: string | null = null;

                try {
                  const rangeMetersInput = (data as unknown as { rangeMeters?: unknown }).rangeMeters;
                  const poiCategoriesInput = (data as unknown as { poiCategories?: unknown }).poiCategories;

                  let payload: SurveyData = {
                    ...data,
                    rangeMeters: normalizeRangeMeters(rangeMetersInput),
                    poiCategories: normalizeCategories(poiCategoriesInput),
                  };

                  if (!isValidLatLng(payload.latitude, payload.longitude)) {
                    const geo = await getBrowserLocation();
                    if (geo) {
                      payload = {
                        ...payload,
                        latitude: geo.latitude,
                        longitude: geo.longitude,
                      };
                    }
                  }

                  if (!isValidLatLng(payload.latitude, payload.longitude)) {
                    setRouteGeoJson(null);
                    await appendRecovery(
                      "Location is missing or invalid. Please allow location access or choose a different area."
                    );
                    return;
                  }

                  // 1. Optimistically append messages to UI:
                  //    (A) User's survey data
                  //    (B) "Please wait" message
                  const now = Date.now();
                  const surveyContent = `[SURVEY_DATA] ${JSON.stringify(payload)}`;
                  const persistedSurveyMsg: UIMessage = {
                    id: `survey-${now}`,
                    role: "user",
                    parts: [{ type: "text", text: surveyContent }],
                  };

                  const thankYouText = "Got it! 🌟 I've received your preferences and I'm excited to plan this for you!";
                  const thankYouMsg: UIMessage = {
                    id: `thank-${now}`,
                    role: "assistant",
                    parts: [{ type: "text", text: thankYouText }],
                  };

                  const waitText = "I'm crafting your perfect trip now. This might take a moment, so please bear with me... ⏳";
                  waitMsgId = `wait-${now}`;
                  const waitMsg: UIMessage = {
                    id: waitMsgId,
                    role: "assistant",
                    parts: [{ type: "text", text: waitText }],
                  };

                  setMessages((prev) => [...prev, persistedSurveyMsg, thankYouMsg, waitMsg]);

                  // 2. Persist the survey message and the thank you message to backend history immediately
                  await apiSendMessage(conversationId, surveyContent, true);
                  await apiSendMessage(conversationId, thankYouText, false);

                  // 3. Submit survey for processing
                  const trySubmit = async (p: SurveyData) => {
                    const res =
                      (await submitSurvey(conversationId, p)) as SubmitSurveyResponseWithSpotify;

                    if (res.routeStatus === "FAILED") return { ok: false as const, res };

                    const routeData = res.route as FeatureCollection | undefined;
                    if (!routeData) {
                      return {
                        ok: false as const,
                        res: {
                          routeStatus: "FAILED" as const,
                          userMessage:
                            "The route data was missing or malformed. Please try again.",
                        },
                      };
                    }
                    return { ok: true as const, res, routeData };
                  };

                  const attempt = await trySubmit(payload);

                  if (!attempt.ok) {
                    const msg =
                      attempt.res.userMessage ??
                      "I couldn't find enough interesting places nearby to build a route. Please try a larger radius or a different area.";
                    setRouteGeoJson(null);
                    await appendRecovery(msg);
                    return;
                  }

                  const { res, routeData } = attempt;

                  setRouteGeoJson(routeData);

                  const spotifyLink = res.spotifyPlaylistLink ?? null;
                  if (spotifyLink) {
                    setSpotifyUrlByChat((prev) => ({
                      ...prev,
                      [String(conversationId)]: spotifyLink,
                    }));
                  }

                  // Check if routes array exists
                  const routesArray = Array.isArray(res.routes) ? res.routes : [routeData];
                  const routeCount = routesArray.length;

                  let botText = `All done! 🎉 I've prepared ${routeCount} lovely route${routeCount === 1 ? "" : "s"} just for you.`;
                  if (spotifyLink) {
                    botText += `\n\nI also created a Spotify playlist for you based on the conversation mood: [Open playlist](${spotifyLink})`;
                  }

                  await apiSendMessage(conversationId, botText, false);

                  const successMsg: UIMessage = {
                    id: (Date.now() + 1).toString(),
                    role: "assistant",
                    parts: [{ type: "text", text: botText }],
                  };
                  setMessages((prev) => [...prev, successMsg]);

                  // Track used images to prioritize diversity
                  const usedImages = new Set<string>();

                  const cardDataList: RouteRecommendation[] = routesArray.map((fc, idx) => {
                    const features = (fc.features ?? []) as AnyFeature[];
                    const routeFeature = features.find(isRouteFeature);
                    const props = routeFeature?.properties as RouteProperties | undefined;

                    const poiFeats = features.filter(isPoiFeature);

                    // Find the best image for this card
                    // Priority 1: Image explicitly set on route props
                    // Priority 2: First POI image that hasn't been used yet
                    // Priority 3: First POI image (fallback if all used)

                    let thumbnail = props?.image;

                    if (!thumbnail) {
                      const availablePoiImages = poiFeats
                        .map(f => f.properties?.imageUrl)
                        .filter((url): url is string => !!url);

                      const uniqueImage = availablePoiImages.find(url => !usedImages.has(url));

                      if (uniqueImage) {
                        thumbnail = uniqueImage;
                      } else if (availablePoiImages.length > 0) {
                        thumbnail = availablePoiImages[0];
                      }
                    }

                    if (thumbnail) {
                      usedImages.add(thumbnail);
                    } else {
                      thumbnail = "/placeholder.png";
                    }

                    const dayDescs = props?.dayDescriptions || { "1": "A personalized route based on your mood." };

                    return {
                      id: String(idx + 1),
                      title: props?.name || "Your Personalized Trip",
                      dayDescriptions: dayDescs,
                      imageUrl: thumbnail,
                      distanceMeters: props?.distanceMeters || 0,
                      durationSeconds: props?.durationSeconds || 0,
                      geoJson: fc as FeatureCollection,
                      routeType: props?.routeType,
                      routeTypeTitle: props?.routeTypeTitle,
                    };
                  });

                  const cardsPayload = `[ROUTE_CARDS] ${JSON.stringify(cardDataList)}`;
                  await apiSendMessage(conversationId, cardsPayload, false);

                  const cardsMsg: UIMessage = {
                    id: (Date.now() + 3).toString(),
                    role: "assistant",
                    parts: [{ type: "text", text: cardsPayload }],
                  };
                  setMessages((prev) => [...prev, cardsMsg]);
                  setIsChatLocked(true);
                } catch (e) {
                  console.error("Failed to submit survey", e);
                  setRouteGeoJson(null);
                  const fallbackMessage =
                    e instanceof Error && e.message
                      ? e.message
                      : "I couldn't generate a route due to an unexpected error. Please try again.";
                  await appendRecovery(fallbackMessage);
                } finally {
                  // Remove the "wait" message from the UI
                  if (waitMsgId) {
                    const idToRemove = waitMsgId;
                    setMessages((prev) => prev.filter((m) => m.id !== idToRemove));
                  }
                  setIsLoading(false);
                }
              }}
            />
          )}
        </div>
      </SidebarInset>

      <DeleteConfirmDialog
        open={deleteDialogOpen}
        onOpenChange={(open) => {
          setDeleteDialogOpen(open);
          if (!open) setChatToDelete(null);
        }}
        title="Delete this chat?"
        description={
          chatToDelete
            ? `“${chatToDelete.title}” and all its messages will be permanently deleted.`
            : "This chat and all its messages will be permanently deleted."
        }
        confirmLabel="Delete"
        cancelLabel="Cancel"
        loading={isDeleting}
        onConfirm={handleConfirmDeleteChat}
      />

      <RenameChatDialog
        open={renameDialogOpen}
        onOpenChange={(open) => {
          setRenameDialogOpen(open);
          if (!open) setChatToRename(null);
        }}
        initialTitle={chatToRename?.title ?? ""}
        loading={isRenaming}
        onConfirm={handleConfirmRenameChat}
      />
    </SidebarProvider>
  );
}
