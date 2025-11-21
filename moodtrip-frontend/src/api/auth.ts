const BASE = import.meta.env.VITE_API_BASE_URL ?? "http://127.0.0.1:8080";

export type LoginBody = { email: string; password: string };
export type SignupBody = { username: string; email: string; password: string };

export async function login(body: LoginBody) {
const res = await fetch(`${BASE}/api/auth/login`, {
method: "POST",
headers: { "Content-Type": "application/json" },
credentials: "include",
body: JSON.stringify(body),
});
if (!res.ok) throw new Error(await parseErr(res));
return res.json() as Promise<{ token?: string; user?: any }>;
}

export async function signup(body: SignupBody) {
const res = await fetch(`${BASE}/api/auth/signup`, {
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
  //TODO: dynamically input current UserId, now is the mock version
    import.meta.env.VITE_SPOTIFY_AUTH_URL ??
    `${BASE}/api/spotify/login?userId=3`,
  GOOGLE:
    import.meta.env.VITE_GOOGLE_AUTH_URL ??
    `${BASE}/oauth2/authorization/google`,
};
