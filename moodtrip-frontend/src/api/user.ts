const BASE = import.meta.env.VITE_API_BASE_URL ?? "";

export type UserProfile = {
  id: number | string;
  username: string;
  email: string;
  avatarUrl?: string | null;
};

export async function fetchCurrentUser(): Promise<UserProfile | null> {
  try {
    const res = await fetch(`${BASE}/api/users/me`, {
      method: "GET",
      credentials: "include",
    });

    if (!res.ok) {
      return null;
    }

    const data = await res.json();

    return {
      id: data.id,
      username: data.username ?? data.name ?? "User",
      email: data.email ?? "user@example.com",
      avatarUrl: data.avatarUrl ?? null,
    };
  } catch {
    return null;
  }
}
