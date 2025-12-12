import { Navbar } from "@/components/layout/navbar";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { useEffect, useRef } from "react";
import { saveUser, saveToken } from "@/api/auth";

export function AppLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const hasProcessedAuth = useRef(false);
  useEffect(() => {
    if (hasProcessedAuth.current) return;
    const params = new URLSearchParams(window.location.search);
    const flag = params.get("auth");
    const token = params.get("token");
    const userId = params.get("userId");
    const username = params.get("username");
    const email = params.get("email");

    if (!flag) return;

    hasProcessedAuth.current = true;

    if (flag === "success" && token && userId && username) {
      const decodedUsername = decodeURIComponent(username);
      const decodedEmail = email ? decodeURIComponent(email) : "";

      saveToken(token);

      saveUser({
        id: parseInt(userId, 10),
        username: decodedUsername,
        email: decodedEmail,
      });

      window.dispatchEvent(new Event("userLogin"));

      window.history.replaceState(
        null,
        document.title,
        window.location.pathname
      );

      navigate("/", {
        replace: true,
        state: {
          fromLogin: true,
          username: decodedUsername,
        },
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
    location.pathname.startsWith("/route-details") ||
    location.pathname.startsWith("/signup");

  return (
    <div className="min-h-screen flex flex-col">
      {!hideNavbar && (
        <header className="fixed top-0 left-0 w-full z-50">
          <Navbar />
        </header>
      )}

      <main className="flex-1">
        <Outlet />
      </main>
    </div>
  );
}
