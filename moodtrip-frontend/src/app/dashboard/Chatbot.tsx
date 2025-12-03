import { useEffect, useState, useRef } from "react";
import type { UIMessage } from "@ai-sdk/react";

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
  const storedUser = getUser();
  const displayUser = {
    name: storedUser?.username ?? navData.user.name,
    email: storedUser?.email ?? navData.user.email,
    avatar: undefined,
  };

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
      // Try to extract content field
      // Format: EmotionResult[..., content=Actual text, success=...]
      // We use a regex that looks for content= and ends before , success=
      // Note: This is a best-effort parser for the Java record toString
      const match = content.match(/content=(.*), success=(true|false)\]$/);
      if (match && match[1]) {
        return match[1];
      }
    }
    return content;
  };

  // Load chats on mount
  useEffect(() => {
    loadChats();
  }, []);

  async function loadChats() {
    try {
      const data = await getMyConversations();
      const mapped: ChatSummary[] = data.map((c) => ({
        id: c.id.toString(),
        title: c.title,
        icon: undefined, // Could map emotion to icon here
      }));
      setChats(mapped.reverse());
    } catch (e) {
      console.error("Failed to load chats", e);
    }
  }

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
  }, [selectedChatId]);

  async function loadMessages(chatId: string) {
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
  }

  const handleNewChat = async () => {
    try {
      const newChat = await startConversation();
      const newChatSummary: ChatSummary = {
        id: newChat.id.toString(),
        title: newChat.title,
        icon: undefined,
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
          icon: undefined,
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
    } catch (e) {
      console.error(e);
    } finally {
      setIsLoading(false);
    }
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
            />
          )}
        </div>
      </SidebarInset>
    </SidebarProvider>
  );
}
