import { Navbar } from "@/components/layout/navbar";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { useEffect, useRef } from "react";
import { saveUser } from "@/api/auth";

const BASE = import.meta.env.VITE_API_BASE_URL ?? "http://127.0.0.1:8080";

export function AppLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const hasProcessedAuth = useRef(false);
  useEffect(() => {
    if (hasProcessedAuth.current) return;
    const params = new URLSearchParams(window.location.search);
    const flag = params.get("auth");
    const userId = params.get("userId");

    if (!flag) return;

    hasProcessedAuth.current = true;

    if (flag === "success" && userId) {
      console.log("Processing Spotify auth success with userId:", userId);
      fetch(`${BASE}/api/users/${userId}`)
        .then((res) => {
          if (!res.ok) throw new Error("Failed to fetch user");
          return res.json();
        })
        .then((data) => {
         saveUser({
            id: data.id,
            username: data.username,
            email: data.email || "",
            
          }
        );
          alert("Hi! " + data.username + ", login is successful!");
          navigate("/chat");

          window.dispatchEvent(new Event("userLogin"));

          window.history.replaceState(
            null,
            document.title,
            window.location.pathname
          );
        })
        .catch((err) => {
          console.error("Failed to fetch user info:", err);
          alert("Login successful but failed to load user info");
          window.history.replaceState(null, document.title, "/login");
          navigate("/login", { replace: true });
        });
    } else if (flag === "error") {
      const raw = params.get("msg") || "Spotify Authorization failed";
      const msg = decodeURIComponent(raw);
      alert(`Spotify Authorization failed: ${msg}`);

      window.history.replaceState(null, document.title, "/login");
      navigate("/login", { replace: true });
    }
  }, [navigate]);

  // Hide Navbar on /chat
  const hideNavbar =
    location.pathname.startsWith("/chat") ||
    location.pathname.startsWith("/login") ||
    location.pathname.startsWith("/signup");

  return (
    <div className="min-h-screen flex flex-col">
      {/* ✅ Navbar sits fixed at top */}
      {!hideNavbar && (
        <header className="fixed top-0 left-0 w-full z-50">
          <Navbar />
        </header>
      )}

      {/* ✅ No padding at all — the pages will handle spacing if needed */}
      <main className="flex-1">
        <Outlet />
      </main>
    </div>
  );
}
