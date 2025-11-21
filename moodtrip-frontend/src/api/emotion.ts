// src/api/emotion.ts
const BASE = import.meta.env.VITE_API_BASE_URL ?? "";

export type EmotionResultDto = {
  scores: Record<string, number>;
  top_label: string;
  top_score: number;
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
    if (!data || !data.top_label) return null;
    return data;
  } catch {
    return null;
  }
}
