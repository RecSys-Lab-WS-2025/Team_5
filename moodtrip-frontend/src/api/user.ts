import { BASE, authFetch } from "./auth"

export type UserProfile = {
  id: number | string
  username: string
  email: string
  createdAt?: string
  avatarUrl?: string | null
}

function resolveAvatarUrl(url?: string | null): string | null {
  if (!url) return null
  if (url.startsWith("http")) return url
  return `${BASE}${url}`
}

export async function fetchCurrentUser(): Promise<UserProfile | null> {
  try {
    const res = await authFetch(`${BASE}/api/users/me`, { method: "GET" })
    if (!res.ok) return null

    const data = await res.json()

    return {
      id: data.id,
      username: data.username ?? data.name ?? "User",
      email: data.email ?? "",
      createdAt: data.createdAt,
      avatarUrl: resolveAvatarUrl(data.avatarUrl),
    }
  } catch {
    return null
  }
}

export async function updateProfile(body: {
  username: string
  avatarUrl?: string | null
}): Promise<{
  username?: string
  avatarUrl?: string | null
}> {
  const res = await authFetch(`${BASE}/api/users/me/profile`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  })

  if (!res.ok) {
    const err = await res.text()
    throw new Error(err)
  }

  const data = await res.json()

  return {
    username: data.username,
    avatarUrl: resolveAvatarUrl(data.avatarUrl),
  }
}

export async function uploadAvatar(file: File): Promise<{ avatarUrl: string }> {
  const formData = new FormData()
  formData.append("file", file)

  const res = await authFetch(`${BASE}/api/users/me/avatar`, {
    method: "POST",
    body: formData,
  })

  if (!res.ok) {
    const err = await res.text()
    throw new Error(err)
  }

  const data = await res.json()

  const resolved = resolveAvatarUrl(data.avatarUrl)
  if (!resolved) throw new Error("Avatar upload succeeded but avatarUrl is missing/null.")

  return { avatarUrl: resolved }
}
