import { useEffect, useState, useCallback, useRef } from "react";
import type { UIMessage } from "@ai-sdk/react";

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

import {
  Laugh,
  Smile,
  Meh,
  Annoyed,
  Angry,
  LifeBuoy,
  Send,
  Heart,
  Music,
  Users,
  SquareTerminal,
} from "lucide-react";

import { getUser } from "@/api/auth";
import {
  startConversation,
  getMyConversations,
  getConversationMessages,
  sendMessage as apiSendMessage,
  extractEmotion as apiExtractEmotion,
  submitSurvey,
} from "@/api/conversation";
import type { FeatureCollection } from "geojson";

type RawRoute = {
  type?: string;
  distanceMeters?: number;
  durationSeconds?: number;
  geometry?: Array<{
    lat?: number;
    latitude?: number;
    lon?: number;
    lng?: number;
    longitude?: number;
  }>;
};

const toGeoJsonRoute = (route: unknown): FeatureCollection | null => {
  if (!route) return null;

  // Attempt to parse string payloads (e.g., "Route[distanceMeters=..., geometry=[RouteCoordinate[lat=.., lon=..], ...]]")
  if (typeof route === "string") {
    const rawStr = route.trim();

    // Try JSON first
    try {
      const parsed = JSON.parse(rawStr);
      const geo = toGeoJsonRoute(parsed);
      if (geo) return geo;
    } catch {
      console.warn("not a json");
    }
  }

  if (typeof route !== "object") return null;
  const raw = route as RawRoute;

  if (raw.type === "FeatureCollection") {
    return raw as FeatureCollection;
  }

  if (!Array.isArray(raw.geometry)) return null;

  const coords = raw.geometry
    .map((c) => {
      const lat = c?.lat ?? c?.latitude;
      const lon = c?.lon ?? c?.lng ?? c?.longitude;
      if (typeof lat === "number" && typeof lon === "number") {
        return [lon, lat];
      }
      return null;
    })
    .filter(Boolean) as [number, number][];

  if (coords.length < 2) return null;

  return {
    type: "FeatureCollection",
    features: [
      {
        type: "Feature",
        properties: {
          distanceMeters: raw.distanceMeters,
          durationSeconds: raw.durationSeconds,
        },
        geometry: {
          type: "LineString",
          coordinates: coords,
        },
      },
    ],
  };
};

// ---- static nav data ----
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

    {
      title: "My favourites",
      url: "#",
      icon: Heart,
      items: [
        { title: "Trips", url: "#" },
        { title: "Saved Spots", url: "#" },
        { title: "Wishlists", url: "#" },
        { title: "Pinned Ideas", url: "#" },
      ],
    },

    {
      title: "My Spotify",
      url: "#",
      icon: Music,
      items: [
        { title: "Playlists", url: "#" },
        { title: "Settings", url: "#" },
      ],
    },

    {
      title: "Community",
      url: "#",
      icon: Users,
      items: [
        { title: "Travel Feed", url: "#" },
        { title: "My Stories", url: "#" },
        { title: "My Journeys", url: "#" },
        { title: "Insights Dashboard", url: "#" },
        { title: "Topics & Boards", url: "#" },
      ],
    },
  ],
  navSecondary: [
    { title: "Support", url: "#", icon: LifeBuoy },
    { title: "Feedback", url: "#", icon: Send },
  ],
};

export default function Chatbot() {
  const [displayUser, setDisplayUser] = useState(() => {
    const storedUser = getUser();
    return {
      name: storedUser?.username ?? navData.user.name,
      email: storedUser?.email ?? navData.user.email,
      avatar: undefined,
    };
  });

  useEffect(() => {
    const handleStorageChange = () => {
      const storedUser = getUser();
      if (storedUser) {
        setDisplayUser({
          name: storedUser.username,
          email: storedUser.email,
          avatar: undefined,
        });
      }
    };

    window.addEventListener("storage", handleStorageChange);
    window.addEventListener("userLogin", handleStorageChange);

    return () => {
      window.removeEventListener("storage", handleStorageChange);
      window.removeEventListener("userLogin", handleStorageChange);
    };
  }, []);

  const [selectedChatId, setSelectedChatId] = useState<string | null>(null);
  const [chats, setChats] = useState<ChatSummary[]>([]);
  const [messages, setMessages] = useState<UIMessage[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [input, setInput] = useState("");
  const [emotionExtracted, setEmotionExtracted] = useState(true);
  const [pendingChatId, setPendingChatId] = useState<string | null>(null);
  const [routeGeoJson, setRouteGeoJson] = useState<FeatureCollection | null>(
    null
  );
  const skipLoadRef = useRef(false);

  const parseMessageContent = (content: string) => {
    if (content.startsWith("EmotionResult[")) {
      const match: RegExpMatchArray | null = content.match(
        /content=([\s\S]*?),\s*success=(true|false)/
      );
      if (match && match[1]) {
        return match[1];
      }
    }
    return content;
  };

  const getEmotionIcon = (emotion?: string) => {
    if (!emotion) return undefined;
    const e = emotion.toUpperCase();
    switch (e) {
      case "JOYFUL":
      case "EXCITED":
      case "ENERGIZED":
        return Laugh;
      case "CONTENT":
      case "CALM":
      case "RELAXED":
      case "GRATEFUL":
      case "HOPEFUL":
      case "TRUSTING":
        return Smile;
      case "NEUTRAL":
      case "BORED":
      case "TIRED":
      case "CURIOUS":
      case "ANTICIPATING":
      case "SAD":
      case "LONELY":
      case "NOSTALGIC":
      case "CONFUSED":
        return Meh;
      case "ANGRY":
      case "FRUSTRATED":
      case "DISGUSTED":
        return Angry;
      case "STRESSED":
      case "ANXIOUS":
      case "OVERWHELMED":
      case "FEARFUL":
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
    setIsLoading(true);
    try {
      const msgs = await getConversationMessages(Number(chatId));
      const uiMsgs: UIMessage[] = msgs.map((m) => ({
        id: m.id.toString(),
        role: m.sender === "USER" ? "user" : "assistant",
        parts: [{ type: "text", text: parseMessageContent(m.content) }],
      }));
      setMessages(uiMsgs);

      setEmotionExtracted(uiMsgs.length > 0);
    } catch (e) {
      console.error("Failed to load messages", e);
      setMessages([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!selectedChatId) {
      setMessages([]);
      return;
    }

    if (skipLoadRef.current) {
      skipLoadRef.current = false;
      return;
    }

    loadMessages(selectedChatId);
  }, [selectedChatId, loadMessages]);

  const handleNewChat = async () => {
    // Only reset state to WelcomeScreen, do not call API.
    setSelectedChatId(null);
    setMessages([]);
    setEmotionExtracted(false);
    setPendingChatId(null);
    setRouteGeoJson(null);
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
    if (!text || !selectedChatId) return;

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
        console.log("Extract Emotion Result:", res);

        if (res.success) {
          setEmotionExtracted(true);
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
      console.log("Suggestion Click - Extract Emotion Result:", res);
      if (res.success) {
        setEmotionExtracted(true);
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
    const userText =
      "Could you briefly introduce what this Moodtrip website does?";

    const assistantText = `Of course! 😊  

**In one sentence:** Moodtrip is a tiny travel buddy that suggests same-day or short-notice trips that match your current mood.

**What Moodtrip helps with:** - You’re not sure *where* to go, you just know *how* you feel  
- You want a small reset rather than a big, complicated holiday  
- You’d like ideas that feel emotionally right, not just “top rated nearby”

**What you share with me:** 1. How you feel right now (tired, excited, overwhelmed, calm, “meh”…).  
2. Who’s coming with you (solo, couple, friends, family).  
3. When you’d roughly like to go and where you’re starting from (if you already know).

**What I do with that:** - I turn your mood + context into a few trip ideas to explore.  
- I try to match the vibe you want: soothing, energising, playful, reflective, etc.

**A small heads-up:** I’m not doing deep emotion analysis on the backend *yet*, so if you share something very complex I may not catch every nuance — but I’ll always respond kindly and try to stay close to your tone.

**What you can do next:** Choose a mood prompt from the sidebar, or just type how you’re feeling and hit **Send**.  
I’ll take it from there and start shaping a Moodtrip for you 💫`;

    handleScriptedClick(userText, assistantText);
  };

  const handleQuickStartClick = () => {
    const userText =
      "How do I quickly get started using Moodtrip? Please give me a short guide.";

    const assistantText = `Let’s keep it super simple 🌈  

**Quick start in 4 tiny steps:**

1. **Tell me how you feel.** For example: “I’m stressed from work and need a soft reset”,  
   or “I’m in a great mood and want something fun and spontaneous”.

2. **Say what you want this trip to do.** Do you want to:
   - Relax and slow down?  
   - Clear your head?  
   - Celebrate something?  
   - Feel inspired or creative?

3. **Add a few basics when you’re ready.** - How many people are travelling  
   - Rough timing (tonight, tomorrow, this weekend, sometime soon)  
   - Your starting city or area  

4. **Press Send and just chat.** I’ll ask for anything that’s missing and then suggest a few trip ideas that match your mood, energy and situation — not just your location.

Later on, when Spotify is connected, I’ll also suggest playlists and artists that fit both your mood and the style of your trip, so your Moodtrip comes with its own soundtrack 🎧✨`;

    handleScriptedClick(userText, assistantText);
  };

  return (
    <SidebarProvider>
      <AppSidebar
        user={displayUser}
        navMain={navData.navMain}
        navSecondary={navData.navSecondary}
        chats={chats}
        selectedChatId={selectedChatId}
        onNewChat={handleNewChat}
        onSelectChat={handleSelectChat}
        onIntroductionClick={handleIntroClick}
        onQuickStartClick={handleQuickStartClick}
        onRefreshChats={loadChats}
      />
      <SidebarInset>
        <header className="sticky top-0 z-50 flex h-16 shrink-0 items-center gap-2 bg-background">
          <div className="flex items-center gap-2 px-4">
            <SidebarTrigger className="-ml-1" />
          </div>
        </header>

        <div className="flex min-h-[calc(100vh-4rem)] flex-1 flex-col">
          {showWelcome ? (
            <WelcomeScreen
              onSuggestionClick={handleSuggestionClick}
              userName={displayUser.name}
            />
          ) : (
            <ChatInterface
              messages={messages}
              input={input}
              handleInputChange={handleInputChange}
              handleSubmit={handleSubmit}
              isLoading={isLoading}
              routeGeoJson={routeGeoJson}
              onSurveySubmit={async (data: SurveyData) => {
                if (!selectedChatId) return;
                try {
                  const res = await submitSurvey(Number(selectedChatId), data);
                  console.log("Received Route:", res.route);
                  setRouteGeoJson(res.route as FeatureCollection);

                  const surveyContent = `[SURVEY_DATA] ${JSON.stringify(data)}`;
                  await apiSendMessage(
                    Number(selectedChatId),
                    surveyContent,
                    true
                  );

                  const persistedSurveyMsg: UIMessage = {
                    id: Date.now().toString(),
                    role: "user",
                    parts: [{ type: "text", text: surveyContent }],
                  };
                  setMessages((prev) => [...prev, persistedSurveyMsg]);

                  let botText =
                    "Thank you! I've received your preferences. I'll now generate a personalized trip for you.";
                  if (res.spotifyPlaylistLink) {
                    botText += `\n\nI also created a Spotify playlist for you based on the conversation mood: ${res.spotifyPlaylistLink}`;
                  }

                  await apiSendMessage(Number(selectedChatId), botText, false);

                  const successMsg: UIMessage = {
                    id: (Date.now() + 1).toString(),
                    role: "assistant",
                    parts: [{ type: "text", text: botText }],
                  };
                  setMessages((prev) => [...prev, successMsg]);

                  // Send and display the route map as a bot message
                  if (res.route) {
                    const geo = toGeoJsonRoute(res.route);
                    if (geo) {
                      const mapPayload = `[ROUTE_MAP] ${JSON.stringify(geo)}`;
                      setRouteGeoJson(geo as FeatureCollection);
                      await apiSendMessage(
                        Number(selectedChatId),
                        mapPayload,
                        false
                      );
                      const mapMsg: UIMessage = {
                        id: (Date.now() + 2).toString(),
                        role: "assistant",
                        parts: [{ type: "text", text: mapPayload }],
                      };
                      setMessages((prev) => [...prev, mapMsg]);
                    } else {
                      console.warn(
                        "Received route but could not convert to GeoJSON"
                      );
                    }
                  }
                } catch (e) {
                  console.error("Failed to submit survey", e);
                }
              }}
            />
          )}
        </div>
      </SidebarInset>
    </SidebarProvider>
  );
}