import { Navbar } from "@/components/layout/navbar";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { useEffect } from "react";

export function AppLayout() {
  const location = useLocation();

  const navigate = useNavigate();

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const flag = params.get("spotify");
    if (!flag) return;

    if (flag === "success") {
      alert("Spotify Authorization successful");
    } else if (flag === "error") {
      const raw = params.get("msg") || "Spotify Authorization failed";
      const msg = decodeURIComponent(raw);
      alert(`Spotify Authorization failed：${msg}`);
    }

    window.history.replaceState(null, document.title, window.location.pathname);

    navigate("/", { replace: true });
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
