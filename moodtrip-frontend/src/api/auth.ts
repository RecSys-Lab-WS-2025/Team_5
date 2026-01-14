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

export async function refreshToken() {
  const res = await fetch(`${BASE}/api/auth/refresh`, {
    method: "POST",
    headers: { ...authHeaders() },
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

// --- Retry Logic for Session Expiration ---
let isRefreshing = false;

// Let's refine the queue to store input/init
type PendedRequest = {
  input: RequestInfo | URL;
  init: RequestInit & { _retryCount?: number };
  resolve: (res: Response) => void;
  reject: (err: unknown) => void;
};
let pendedQueue: PendedRequest[] = [];

/**
 * Rejects all pending requests and clears the queue.
 * Should be called when user explicitly logs out.
 */
export function clearPendedRequests() {
  console.log(`authFetch: Clearing ${pendedQueue.length} pended requests.`);
  pendedQueue.forEach(req => req.reject(new Error("Session terminated by user")));
  pendedQueue = [];
  isRefreshing = false;
}

export async function notifySessionUpdated() {
  isRefreshing = false;
  const queue = [...pendedQueue];
  pendedQueue = [];

  await Promise.all(
    queue.map(async (req) => {
      try {
        const freshHeaders = {
          ...(req.init.headers || {}),
          ...authHeaders(),
        };

        const updatedInit = {
          ...req.init,
          headers: freshHeaders,
          _retryCount: (req.init._retryCount || 0) + 1,
        };

        const res = await fetch(req.input, updatedInit);
        req.resolve(res);
      } catch (err) {
        req.reject(err);
      }
    }),
  );
}

/**
 * Global fetch wrapper that handles 401 interception and retry.
 */
export async function authFetch(
  input: RequestInfo | URL,
  init: RequestInit & { _retryCount?: number } = {},
): Promise<Response> {
  const headers: HeadersInit = {
    ...(init.headers || {}),
    ...authHeaders(),
  };

  try {
    const res = await fetch(input, { ...init, headers });

    if (res.status === 401) {
      const url = typeof input === 'string' ? input : (input instanceof URL ? input.href : input.url);

      // Safety checks: stop if it's the refresh endpoint itself or if we've already retried
      if (url?.includes('/api/auth/refresh')) {
        console.warn("authFetch: 401 from refresh endpoint. Failing all pending requests.");

        // Reject all pending requests waiting on a successful refresh
        while (pendedQueue.length > 0) {
          const pending = pendedQueue.shift();
          try {
            pending?.reject(new Error("Unauthorized: token refresh failed with 401."));
          } catch (e) {
            console.error("authFetch: error rejecting pending request after refresh 401:", e);
          }
        }

        // Reset refreshing state so the system can recover / re-initiate auth if needed
        isRefreshing = false;

        return res;
      }

      if (init._retryCount && init._retryCount >= 1) {
        console.warn("authFetch: 401 retry limit reached for request. Not retrying again.");
        return res;
      }
      console.warn("authFetch: 401 received. Pending request and triggering re-auth UI.");

      return new Promise<Response>((resolve, reject) => {
        pendedQueue.push({ input, init, resolve, reject });

        if (!isRefreshing) {
          isRefreshing = true;
          window.dispatchEvent(new CustomEvent("moodtrip-unauthorized"));
        }
      });
    }

    return res;
  } catch (error) {
    console.error("Network error during authFetch:", error);
    throw error;
  }
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
