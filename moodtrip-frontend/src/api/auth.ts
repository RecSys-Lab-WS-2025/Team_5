export const BASE = import.meta.env.VITE_API_BASE_URL ?? "http://127.0.0.1:8080";

export type LoginBody = { email: string; password: string };
export type SignupBody = { username: string; email: string; password: string };

export type AuthUser = {
  id: number;
  username: string;
  email: string;
  createdAt?: string;
};

export async function login(body: LoginBody) {
  const res = await fetch(`${BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(await parseErr(res));
  return res.json() as Promise<{ token?: string; user?: AuthUser }>;
}

export async function signup(body: SignupBody) {
  const res = await fetch(`${BASE}/api/users`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(await parseErr(res));
  return res.json();
}

export function saveToken(token: string) {
  localStorage.setItem("auth_token", token);
}

export function getToken() {
  return localStorage.getItem("auth_token");
}

export function clearToken() {
  localStorage.removeItem("auth_token");
}

export function saveUser(user: AuthUser) {
  localStorage.setItem("auth_user", JSON.stringify(user));
}

export function getUser(): AuthUser | null {
  const raw = localStorage.getItem("auth_user");
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

export function clearUser() {
  localStorage.removeItem("auth_user");
}

export function logout() {
  clearToken();
  clearUser();
}

export function authHeaders(): HeadersInit {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function authFetch(
  input: RequestInfo | URL,
  init: RequestInit = {},
): Promise<Response> {
  const headers: HeadersInit = {
    ...(init.headers || {}),
    ...authHeaders(),
  };

  const res = await fetch(input, { ...init, headers });
  if (res.status === 401) {
    // optional: handle global unauthorized, e.g. redirect from a central place
    // window.location.href = "/login";
  }
  return res;
}

async function parseErr(res: Response) {
  try {
    const data = await res.json();
    return data?.message || data?.error || res.statusText;
  } catch {
    return res.statusText;
  }
}

export const OAUTH = {
  SPOTIFY:
    import.meta.env.VITE_SPOTIFY_AUTH_URL ?? `${BASE}/api/spotify/login`,
};
