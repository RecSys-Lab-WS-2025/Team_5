import { Navbar } from "@/components/layout/navbar";
import { Outlet, useLocation } from "react-router-dom";

export function AppLayout() {
  const location = useLocation();

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
