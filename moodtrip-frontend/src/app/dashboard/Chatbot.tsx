"use client";

import { useEffect, useState } from "react";
import { useChat } from "@ai-sdk/react";
import type { UIMessage } from "@ai-sdk/react";
import { DefaultChatTransport } from "ai";

import { AppSidebar } from "@/components/sidebar/app-sidebar";
import type { ChatSummary } from "@/components/sidebar/app-sidebar";
import { WelcomeScreen } from "@/components/chat/welcome-screen";
import { ChatInterface } from "@/components/chat/chat-interface";

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

// ---- backend DTOs ----
type StoredTurn = {
  id: string;
  role: "user" | "assistant";
  content: string;
};

type ChatDetailDTO = {
  id: string;
  title: string;
  icon?: "laugh" | "smile" | "meh" | "annoyed" | "angry";
  turns: StoredTurn[];
};

const iconMap = {
  laugh: Laugh,
  smile: Smile,
  meh: Meh,
  annoyed: Annoyed,
  angry: Angry,
} as const;

// mock-ready fetchers
async function fetchChatList(): Promise<ChatSummary[]> {
  try {
    const res = await fetch("/api/chats", { cache: "no-store" });
    if (!res.ok) throw new Error("bad status");
    const list: { id: string; title: string; icon?: keyof typeof iconMap }[] =
      await res.json();
    return list.map((x) => ({
      id: x.id,
      title: x.title,
      icon: x.icon ? iconMap[x.icon] : undefined,
    }));
  } catch {
    return [
      { id: "1", title: "Chat1", icon: Laugh },
      { id: "2", title: "Chat2", icon: Meh },
      { id: "3", title: "Chat3", icon: Angry },
    ];
  }
}

async function fetchChatDetail(id: string): Promise<ChatDetailDTO> {
  try {
    const res = await fetch(`/api/chats/${encodeURIComponent(id)}`, {
      cache: "no-store",
    });
    if (!res.ok) throw new Error("bad status");
    return res.json();
  } catch {
    if (id === "2") {
      return {
        id: "2",
        title: "Chat2",
        icon: "smile",
        turns: [
          { id: "1", role: "user", content: "Do you have emotions?" },
          {
            id: "2",
            role: "assistant",
            content:
              "I can't feel emotions myself, but I can understand and respond to yours! Tell me how you're feeling today.",
          },
        ],
      };
    }
    return {
      id,
      title: `Chat${id}`,
      icon: id === "1" ? "laugh" : "annoyed",
      turns: [
        { id: "u-1", role: "user", content: "Hello?" },
        {
          id: "a-1",
          role: "assistant",
          content: "Hi! How can I help you today?",
        },
      ],
    };
  }
}

function mapStoredToUIMessages(turns: StoredTurn[]): UIMessage[] {
  return turns.map((t) => ({
    id: t.id,
    role: t.role,
    parts: [{ type: "text", text: t.content }],
  }));
}

export default function Chatbot() {
  const [selectedChatId, setSelectedChatId] = useState<string | null>(null);

  const [chats, setChats] = useState<ChatSummary[]>([]);
  useEffect(() => {
    fetchChatList()
      .then(setChats)
      .catch(() => setChats([]));
  }, []);

  const [initialMessages, setInitialMessages] = useState<UIMessage[]>([]);
  const [loadingDetail, setLoadingDetail] = useState(false);

  useEffect(() => {
    if (!selectedChatId || selectedChatId === "new") {
      setInitialMessages([]);
      return;
    }
    setLoadingDetail(true);
    fetchChatDetail(selectedChatId)
      .then((detail) => setInitialMessages(mapStoredToUIMessages(detail.turns)))
      .catch(() => setInitialMessages([]))
      .finally(() => setLoadingDetail(false));
  }, [selectedChatId]);

  const {
    messages,
    sendMessage,
    status,
    setMessages,
  } = useChat({
    transport: new DefaultChatTransport({ api: "/api/chat" }),
    id: selectedChatId ?? undefined,
    messages: initialMessages,
    onError() {
      const idBase = Date.now().toString();
      setMessages((prev) => [
        ...prev,
        {
          id: `error-assistant-${idBase}`,
          role: "assistant",
          parts: [
            {
              type: "text",
              text:
                "Sorry, something went wrong with the connection. I couldn’t reply this time. Please try again.",
            },
          ],
        },
      ]);
    },
  });

  const [input, setInput] = useState("");
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) =>
    setInput(e.target.value);

  const isLoading =
    status === "submitted" || status === "streaming" || loadingDetail;

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const text = input.trim();
    if (!text) return;

    // normal chat input: always goes to backend
    sendMessage({ text });
    setInput("");
  };

  const handleNewChat = () => {
    setSelectedChatId("new");
    setInitialMessages([]);
    setMessages([]);
  };

  const handleSelectChat = (chatId: string) => setSelectedChatId(chatId);

  function createSidebarChatEntry(firstUserText: string) {
    const newId =
      typeof crypto !== "undefined" && "randomUUID" in crypto
        ? crypto.randomUUID()
        : `local-${Date.now()}`;

    const createdAt = new Date();

    const titleFromTime = createdAt.toLocaleString(undefined, {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });

    const title =
      firstUserText && firstUserText.trim().length > 0
        ? firstUserText.slice(0, 40)
        : titleFromTime;

    const preview =
      firstUserText && firstUserText.trim().length > 0
        ? firstUserText.slice(0, 80)
        : undefined;

    setChats((prev) => [
      ...prev,
      {
        id: newId,
        title,
        icon: undefined,
        preview,
      },
    ]);

    setSelectedChatId(newId);
    setInitialMessages([]);
    setMessages([]);
  }

  const handleSuggestionClick = (text: string) => {
    const cleanText = text.trim();
    if (!cleanText) return;

    const isStartingFresh =
      (!selectedChatId || selectedChatId === "new") && messages.length === 0;

    if (isStartingFresh) {
      createSidebarChatEntry(cleanText);
    }

    // send to backend; onError will automatically add a "network error" reply
    sendMessage({ text: cleanText });
  };

  const startNewChatWithMessages = (
    initialMsgs: UIMessage[],
    firstUserText?: string,
  ) => {
    const newId =
      typeof crypto !== "undefined" && "randomUUID" in crypto
        ? crypto.randomUUID()
        : `local-${Date.now()}`;

    const createdAt = new Date();

    const titleFromTime = createdAt.toLocaleString(undefined, {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });

    const title =
      firstUserText && firstUserText.trim().length > 0
        ? firstUserText.slice(0, 40)
        : titleFromTime;

    const preview =
      firstUserText && firstUserText.trim().length > 0
        ? firstUserText.slice(0, 80)
        : undefined;

    setChats((prev) => [
      ...prev,
      {
        id: newId,
        title,
        icon: undefined,
        preview,
      },
    ]);

    setSelectedChatId(newId);
    setInitialMessages(initialMsgs);
    setMessages(initialMsgs);
  };

  const appendScriptedExchange = (userText: string, assistantText: string) => {
    const idBase = Date.now().toString();

    const userMessage: UIMessage = {
      id: `script-user-${idBase}`,
      role: "user",
      parts: [{ type: "text", text: userText }],
    };

    const assistantMessage: UIMessage = {
      id: `script-assistant-${idBase}`,
      role: "assistant",
      parts: [{ type: "text", text: assistantText }],
    };

    const isStartingFresh =
      (!selectedChatId || selectedChatId === "new") && messages.length === 0;

    if (isStartingFresh) {
      // pre-saved conversation: no backend, just local messages + new chat
      startNewChatWithMessages(
        [userMessage, assistantMessage],
        userText,
      );
    } else {
      // already in a chat: just append locally, still不走后端
      setMessages((prev) => [...prev, userMessage, assistantMessage]);
    }
  };

  const handleIntroClick = () => {
    const userText =
      "Could you briefly introduce what this Moodtrip website does?";

    const assistantText = `Of course! 😊  

**In one sentence:**  
Moodtrip is a tiny travel buddy that suggests same-day or short-notice trips that match your current mood.

**What Moodtrip helps with:**  
- You’re not sure *where* to go, you just know *how* you feel  
- You want a small reset rather than a big, complicated holiday  
- You’d like ideas that feel emotionally right, not just “top rated nearby”

**What you share with me:**  
1. How you feel right now (tired, excited, overwhelmed, calm, “meh”…).  
2. Who’s coming with you (solo, couple, friends, family).  
3. When you’d roughly like to go and where you’re starting from (if you already know).

**What I do with that:**  
- I turn your mood + context into a few trip ideas to explore.  
- I try to match the vibe you want: soothing, energising, playful, reflective, etc.

**A small heads-up:**  
I’m not doing deep emotion analysis on the backend *yet*, so if you share something very complex I may not catch every nuance — but I’ll always respond kindly and try to stay close to your tone.

**What you can do next:**  
Choose a mood prompt from the sidebar, or just type how you’re feeling and hit **Send**.  
I’ll take it from there and start shaping a Moodtrip for you 💫`;

    appendScriptedExchange(userText, assistantText);
  };

  const handleQuickStartClick = () => {
    const userText =
      "How do I quickly get started using Moodtrip? Please give me a short guide.";

    const assistantText = `Let’s keep it super simple 🌈  

**Quick start in 4 tiny steps:**

1. **Tell me how you feel.**  
   For example: “I’m stressed from work and need a soft reset”,  
   or “I’m in a great mood and want something fun and spontaneous”.

2. **Say what you want this trip to do.**  
   Do you want to:
   - Relax and slow down?  
   - Clear your head?  
   - Celebrate something?  
   - Feel inspired or creative?

3. **Add a few basics when you’re ready.**  
   - How many people are travelling  
   - Rough timing (tonight, tomorrow, this weekend, sometime soon)  
   - Your starting city or area  

4. **Press Send and just chat.**  
   I’ll ask for anything that’s missing and then suggest a few trip ideas that match your mood, energy and situation — not just your location.

Later on, when Spotify is connected, I’ll also suggest playlists and artists that fit both your mood and the style of your trip, so your Moodtrip comes with its own soundtrack 🎧✨`;

    appendScriptedExchange(userText, assistantText);
  };

  const showWelcome = messages.length === 0;

  return (
    <SidebarProvider>
      <AppSidebar
        user={navData.user}
        navMain={navData.navMain}
        navSecondary={navData.navSecondary}
        chats={chats}
        selectedChatId={selectedChatId}
        onNewChat={handleNewChat}
        onSelectChat={handleSelectChat}
        onIntroductionClick={handleIntroClick}
        onQuickStartClick={handleQuickStartClick}
      />
      <SidebarInset>
        <header className="sticky top-0 z-50 flex h-16 shrink-0 items-center gap-2 bg-white">
          <div className="flex items-center gap-2 px-4">
            <SidebarTrigger className="-ml-1" />
          </div>
        </header>

        <div className="flex min-h-[calc(100vh-4rem)] flex-1 flex-col">
          {showWelcome ? (
            <WelcomeScreen
              onSuggestionClick={handleSuggestionClick}
              userName={navData.user.name}
            />
          ) : (
            <ChatInterface
              messages={messages}
              input={input}
              handleInputChange={handleInputChange}
              handleSubmit={handleSubmit}
              isLoading={isLoading}
            />
          )}
        </div>
      </SidebarInset>
    </SidebarProvider>
  );
}
