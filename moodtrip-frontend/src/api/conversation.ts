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
 * 开启新的对话
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
 * 获取当前用户的所有对话
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
 * 获取某个对话的所有消息
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
 * 发送消息(暂时不提取情绪,直接保存)
 * 返回保存的消息对象
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
 * 提取情绪(第一次发送消息时使用)
 * 返回情绪分析结果
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
 * 提交问卷
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
