import { authFetch, BASE } from "./auth";

export type EmotionResultDto = {
  scores: Record<string, number>;
  topLabel: string;
  topScore: number;
};

export async function analyzeEmotion(
  message: string,
): Promise<EmotionResultDto | null> {
  if (!message.trim()) return null;

  try {
    const url = `${BASE}/ai/extractEmotion?message=${encodeURIComponent(
      message,
    )}`;

    const res = await fetch(url, {
      method: "POST",
    });

    if (!res.ok) {
      return null;
    }

    const data = (await res.json()) as EmotionResultDto;
    if (!data || !data.topLabel) return null;
    return data;
  } catch {
    return null;
  }
}

export interface PoiRatingRequest {
  poiId: string;
  category: string;
  emotion: string;
  rating: number;
}

export async function submitPoiRating(data: PoiRatingRequest): Promise<boolean> {

  try {
    const res = await authFetch(`${BASE}/api/ratings`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(data),
    });

    if (!res.ok) {
      console.error("Submission failed with status:", res.status);
    }
    return res.ok;
  } catch (err) {
    console.error("Failed to submit POI rating error:", err);
    return false;
  }
}

export interface PoiRating {
  id: number;
  userId: number;
  poiId: string;
  category: string;
  emotion: string;
  rating: number;
  createdAt: string;
}

export async function fetchPoiRating(poiId: string, emotion: string): Promise<PoiRating | null> {
  const url = `${BASE}/api/ratings?poiId=${encodeURIComponent(poiId)}&emotion=${encodeURIComponent(emotion)}`;

  try {
    const res = await authFetch(url);
    if (!res.ok) {

      return null;
    }
    const data = await res.json();

    return data;
  } catch (err) {
    console.error("Failed to fetch POI rating error:", err);
    return null;
  }
}
