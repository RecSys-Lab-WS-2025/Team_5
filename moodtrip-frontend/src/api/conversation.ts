// src/api/conversation.ts
import { authFetch } from "./auth";
import type { FeatureCollection } from "geojson";

const BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export type ConversationDto = {
  id: number;
  userId: number;
  title: string;
  emotion: string;
  createdAt: string;
};

export type MessageDto = {
  id: number;
  conversationId: number;
  sender: "USER" | "BOT";
  content: string;
  timestamp: string;
};

export type EmotionResultDto = {
  scores: Record<string, number>;
  topLabel: string;
  topScore: number;
  content: string;
  success: boolean;
};

/**
 * Start a new conversation
 */
export async function startConversation(): Promise<ConversationDto> {
  const res = await authFetch(`${BASE}/api/conversations/start`, {
    method: "POST",
  });
  if (!res.ok) {
    const errorText = await res.text();
    console.error("Start conversation failed:", {
      status: res.status,
      statusText: res.statusText,
      error: errorText,
    });
    throw new Error(
      `Failed to start conversation: ${res.status} ${res.statusText}`
    );
  }
  return res.json();
}

/**
 * Get all conversations of the current user
 */
export async function getMyConversations(): Promise<ConversationDto[]> {
  const res = await authFetch(`${BASE}/api/conversations/me`, {
    method: "GET",
  });
  if (!res.ok) {
    throw new Error("Failed to fetch conversations");
  }
  return res.json();
}

/**
 * Get all messages of a specific conversation
 */
export async function getConversationMessages(
  conversationId: number
): Promise<MessageDto[]> {
  const res = await authFetch(
    `${BASE}/api/conversations/${conversationId}/messages`,
    {
      method: "GET",
    }
  );
  if (!res.ok) {
    throw new Error("Failed to fetch messages");
  }
  return res.json();
}

/**
 * Send a message (temporarily not performing emotion extraction; directly saving)
 * Returns the saved message object
 */
export async function sendMessage(
  conversationId: number,
  content: string,
  isUser: boolean = true
): Promise<MessageDto> {
  const res = await authFetch(
    `${BASE}/api/conversations/${conversationId}/message`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ content, isUser }),
    }
  );
  if (!res.ok) {
    const errorText = await res.text();
    console.error("Send message failed:", {
      status: res.status,
      statusText: res.statusText,
      url: res.url,
      error: errorText,
    });
    throw new Error(`Failed to send message: ${res.status} ${res.statusText}`);
  }
  return res.json();
}

/**
 * Extract emotion (used for the first user message)
 * Returns emotion analysis result
 */
export async function extractEmotion(
  conversationId: number,
  message: string
): Promise<EmotionResultDto> {
  const res = await authFetch(
    `${BASE}/api/conversations/extract-emotion?conversationId=${conversationId}&message=${encodeURIComponent(
      message
    )}`,
    {
      method: "POST",
    }
  );
  if (!res.ok) {
    const errorText = await res.text();
    console.error("Extract emotion failed:", {
      status: res.status,
      statusText: res.statusText,
      url: res.url,
      error: errorText,
    });
    throw new Error(
      `Failed to extract emotion: ${res.status} ${res.statusText}`
    );
  }
  return res.json();
}

/**
 * Submit the survey
 */
export interface SurveyData {
  latitude: number;
  longitude: number;
  locationName: string;
  rangeMeters: number;
  startDate: string;
  endDate: string;
  poiCategories: string[];
}

export type SurveyResponse =
  | {
    routeStatus: "SUCCEEDED";
    route: FeatureCollection;
    spotifyPlaylistLink: string | null;
  }
  | {
    routeStatus: "FAILED";
    userMessage?: string;
    route?: FeatureCollection | null;
    spotifyPlaylistLink?: string | null;
  };

export async function submitSurvey(
  conversationId: number,
  data: SurveyData
): Promise<SurveyResponse> {
  const res = await authFetch(
    `${BASE}/api/surveys?conversationId=${conversationId}`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(data),
    }
  );

  let body: unknown;
  try {
    body = await res.json();
  } catch {
    body = null;
  }

  if (!res.ok) {
    const errorMessage =
      (body as { userMessage?: string } | null)?.userMessage ??
      `Failed to submit survey: ${res.status} ${res.statusText}`;
    const error = new Error(errorMessage);
    (error as { response?: unknown }).response = body;
    throw error;
  }

  return body as SurveyResponse;
}

/**
 * Rename a conversation by ID
 */
export async function renameConversation(
  conversationId: number,
  title: string
): Promise<ConversationDto> {
  const res = await authFetch(
    `${BASE}/api/conversations/${conversationId}/title?title=${encodeURIComponent(
      title
    )}`,
    {
      method: "PUT",
    }
  );

  if (!res.ok) {
    const errorText = await res.text();
    console.error("Rename conversation failed:", {
      status: res.status,
      statusText: res.statusText,
      url: res.url,
      error: errorText,
    });
    throw new Error(
      `Failed to rename conversation: ${res.status} ${res.statusText}`
    );
  }

  return res.json();
}

/**
 * Delete a conversation by ID
 */
export async function deleteConversation(
  conversationId: number
): Promise<void> {
  const res = await authFetch(
    `${BASE}/api/conversations/${conversationId}`,
    {
      method: "DELETE",
    }
  );

  if (!res.ok) {
    const errorText = await res.text();
    console.error("Delete conversation failed:", {
      status: res.status,
      statusText: res.statusText,
      url: res.url,
      error: errorText,
    });
    throw new Error(
      `Failed to delete conversation: ${res.status} ${res.statusText}`
    );
  }
}