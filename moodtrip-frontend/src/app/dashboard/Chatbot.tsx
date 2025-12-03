import { useEffect, useState, useRef, useCallback } from "react";
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

    window.addEventListener('storage', handleStorageChange);
    window.addEventListener('userLogin', handleStorageChange);

    return () => {
      window.removeEventListener('storage', handleStorageChange);
      window.removeEventListener('userLogin', handleStorageChange);
    };
  }, []);

  const [selectedChatId, setSelectedChatId] = useState<string | null>(null);
  const [chats, setChats] = useState<ChatSummary[]>([]);
  const [messages, setMessages] = useState<UIMessage[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [input, setInput] = useState("");
  const [emotionExtracted, setEmotionExtracted] = useState(true);
  const [pendingChatId, setPendingChatId] = useState<string | null>(null);
  const skipLoadRef = useRef(false);

  // Helper to parse backend message content
  const parseMessageContent = (content: string) => {
    if (content.startsWith("EmotionResult[")) {
      // Regex to extract content between "content=" and ", success="
      // Using [\s\S]* to match any character including newlines
      const match: RegExpMatchArray | null = content.match(/content=([\s\S]*?),\s*success=(true|false)/);
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
      case "ANNOYED":
        return Annoyed;
      default:
        return undefined;
    }
  };

  const loadChats = useCallback(async () => {
    try {
      const data = await getMyConversations();
      // Sort by ID descending (newest first)
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

  // Load chats on mount
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

      // Determine emotionExtracted status
      setEmotionExtracted(uiMsgs.length > 0);
    } catch (e) {
      console.error("Failed to load messages", e);
      setMessages([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Load messages when chat selected
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
    try {
      const newChat = await startConversation();
      const newChatSummary: ChatSummary = {
        id: newChat.id.toString(),
        title: newChat.title,
        icon: getEmotionIcon(newChat.emotion),
      };
      setChats((prev) => [newChatSummary, ...prev]);
      // Do NOT select it immediately. Keep showing Welcome Screen.
      setSelectedChatId(null);
      setMessages([]);
      setEmotionExtracted(false);
      setPendingChatId(newChat.id.toString());
    } catch (e) {
      console.error("Failed to create new chat", e);
    }
  };

  const handleSelectChat = (chatId: string) => {
    setSelectedChatId(chatId);
    setPendingChatId(null); // Clear pending if user manually selects a chat
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) =>
    setInput(e.target.value);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const text = input.trim();
    if (!text || !selectedChatId) return;

    setInput("");

    // Optimistic update
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
        // Attempt to extract emotion
        const res = await apiExtractEmotion(Number(selectedChatId), text);
        console.log("Extract Emotion Result:", res);

        if (res.success) {
          setEmotionExtracted(true);
        } else {
          // Failed to extract, next time still extract
          setEmotionExtracted(false);
        }

        // If backend returns a content (bot response), display it
        if (res.content) {
          const botMsg: UIMessage = {
            id: (Date.now() + 1).toString(),
            role: "assistant",
            parts: [{ type: "text", text: parseMessageContent(res.content) }],
          };
          setMessages((prev) => [...prev, botMsg]);
        }

        // Mock Survey on success
        if (res.success) {
          const surveyMsg: UIMessage = {
            id: (Date.now() + 2).toString(),
            role: "assistant",
            parts: [{ type: "text", text: "[SURVEY_FORM_TRIGGER]" }],
          };
          setMessages((prev) => [...prev, surveyMsg]);
        }
      } else {
        // Already extracted, just store message
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
      } catch (e) {
        console.error(e);
        return;
      }
    }

    // Enter the chat
    // Prevent loadMessages from overwriting our optimistic state
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

      // Mock Survey on success
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

  const handleIntroClick = () => {
    // For introduction, we can just trigger a suggestion click with the intro text
    // This simplifies things and reuses the existing logic
    const userText = "Could you briefly introduce what this Moodtrip website does?";
    handleSuggestionClick(userText);
  };

  const handleQuickStartClick = () => {
    const userText = "How do I quickly get started using Moodtrip? Please give me a short guide.";
    handleSuggestionClick(userText);
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
        <header className="sticky top-0 z-50 flex h-16 shrink-0 items-center gap-2 bg-white">
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
              onSurveySubmit={async (data: SurveyData) => {
                if (!selectedChatId) return;
                try {
                  const res = await submitSurvey(Number(selectedChatId), data);
                  console.log("Received Route:", res.route);

                  // Persist survey as a user message
                  const surveyContent = `[SURVEY_DATA] ${JSON.stringify(data)}`;
                  await apiSendMessage(Number(selectedChatId), surveyContent, true);

                  // Update local state to show the persisted survey immediately
                  const persistedSurveyMsg: UIMessage = {
                    id: Date.now().toString(),
                    role: "user",
                    parts: [{ type: "text", text: surveyContent }],
                  };
                  setMessages((prev) => [...prev, persistedSurveyMsg]);

                  // Optional: Add a success message from bot
                  let botText = "Thank you! I've received your preferences. I'll now generate a personalized trip for you.";
                  if (res.spotifyPlaylistLink) {
                    botText += `\n\nI also created a Spotify playlist for you based on the conversation mood: ${res.spotifyPlaylistLink}`;
                  }

                  // Persist bot message
                  await apiSendMessage(Number(selectedChatId), botText, false);

                  const successMsg: UIMessage = {
                    id: (Date.now() + 1).toString(),
                    role: "assistant",
                    parts: [{ type: "text", text: botText }],
                  };
                  setMessages((prev) => [...prev, successMsg]);
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
