// File: Navbar.tsx

"use client";

import { useState, useEffect } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { LogOut, User } from "lucide-react";


import { Button } from "@/components/ui/button";
import {
  NavigationMenu,
  NavigationMenuItem,
  NavigationMenuList,
} from "@/components/ui/navigation-menu";
import { cn } from "@/lib/utils";
import { getUser, logout, type AuthUser } from "@/api/auth";

const ITEMS = [
  { label: "About", href: "/about" },
  { label: "FAQ", href: "/faq" },
  { label: "Contact", href: "/contact" },
];

export const Navbar = () => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const pathname = useLocation().pathname;
  const navigate = useNavigate();
  const [user, setUser] = useState<AuthUser | null>(getUser());

  useEffect(() => {
    const handleStorageChange = () => {
      setUser(getUser());
    };

    window.addEventListener('storage', handleStorageChange);
    window.addEventListener('userLogin', handleStorageChange);

    return () => {
      window.removeEventListener('storage', handleStorageChange);
      window.removeEventListener('userLogin', handleStorageChange);
    };
  }, []);

  const handleLogout = () => {
    logout();
    setUser(null);
    window.dispatchEvent(new Event('userLogin'));
    navigate("/");
  };
  

  const handleStartTripNavigation = (e: React.MouseEvent<HTMLAnchorElement | HTMLButtonElement>) => {
    e.preventDefault(); 
    const currentUser = getUser();
    if (!currentUser) {
      navigate("/login");
    } else {
      navigate("/chat");
    }
    setIsMenuOpen(false);
  };

  return (
    <section
      // MODIFIED: Re-added bg-background/70, border, and backdrop-blur-md for the frosted glass effect
      className={cn(
        "bg-background/70 absolute left-1/2 z-50 w-[min(90%,1000px)] -translate-x-1/2 rounded-4xl border backdrop-blur-md transition-all duration-300",
        "top-5 lg:top-12",
        "text-black [&_*]:!text-black"
      )}
    >
      <div className="flex items-center justify-between px-6 py-3">
        <Link
          to="/"
          className="flex shrink-0 items-center gap-2 group hover:opacity-80 transition-opacity"
        >
          <img
            src="/favicon.png"
            alt="Moodtrip logo"
            width={30}
            height={30}
            className="object-contain"
          />
          <span className="text-xl font-bold !text-black select-none group-hover:!text-black">
            Moodtrip
          </span>
        </Link>

        {/* Desktop Navigation */}
        <NavigationMenu className="max-lg:hidden">
          <NavigationMenuList className="justify-around w-full gap-6">
            
            {/* NEW: Start Trip with conditional routing */}
            <NavigationMenuItem key="Start Trip">
              <a
                href="/chat"
                onClick={handleStartTripNavigation}
                className={cn(
                  "relative bg-transparent px-4 text-base font-semibold !text-black hover:opacity-75 whitespace-nowrap",
                  pathname === "/chat" && "!text-black"
                )}
              >
                Start Trip
              </a>
            </NavigationMenuItem>

            {/* Original ITEMS map */}
            {ITEMS.map((link) => (
              <NavigationMenuItem key={link.label}>
                <Link
                  to={link.href}
                  className={cn(
                    "relative bg-transparent px-4 text-base font-semibold !text-black hover:opacity-75 whitespace-nowrap",
                    pathname === link.href && "!text-black"
                  )}
                >
                  {link.label}
                </Link>
              </NavigationMenuItem>
            ))}
          </NavigationMenuList>
        </NavigationMenu>

        {/* Right side */}
        <div className="flex items-center gap-4">
          {user ? (
            <div className="flex items-center gap-3 max-lg:hidden">
              <div className="flex items-center gap-2 text-base font-semibold text-black">
                <User className="w-5 h-5" />
                <span>{user.email}</span>
              </div>
              <Button
                variant="ghost"
                size="icon"
                onClick={handleLogout}
                className="h-9 w-9 !bg-background hover:bg-gray-100 rounded-full" 
                title="Logout"
                aria-label="Logout"
              >
                <LogOut className="w-5 h-5" />
              </Button>
            </div>
          ) : (
            <Link to="/login" className="max-lg:hidden">
              <Button className="!bg-background hover:!bg-gray-100 !text-black text-base px-5 py-3 h-auto border border-black">
                <span className="relative z-10 !text-black font-semibold">Login</span>
              </Button>
            </Link>
          )}

          {/* Hamburger button (mobile) */}
          <button
            className="text-muted-foreground relative flex size-8 lg:hidden"
            onClick={() => setIsMenuOpen(!isMenuOpen)}
          >
            <span className="sr-only">Open main menu</span>
            <div className="absolute top-1/2 left-1/2 block w-[18px] -translate-x-1/2 -translate-y-1/2">
              <span
                aria-hidden="true"
                className={`absolute block h-0.5 w-full rounded-full bg-current transition duration-500 ease-in-out ${isMenuOpen ? "rotate-45" : "-translate-y-1.5"
                  }`}
              />
              <span
                aria-hidden="true"
                className={`absolute block h-0.5 w-full rounded-full bg-current transition duration-500 ease-in-out ${isMenuOpen ? "opacity-0" : ""
                  }`}
              />
              <span
                aria-hidden="true"
                className={`absolute block h-0.5 w-full rounded-full bg-current transition duration-500 ease-in-out ${isMenuOpen ? "-rotate-45" : "translate-y-1.5"
                  }`}
              />
            </div>
          </button>
        </div>
      </div>

      {/* Mobile Navigation */}
      <div
        className={cn(
          "bg-background fixed inset-x-0 top-[calc(100%+1rem)] flex flex-col rounded-2xl border p-6 transition-all duration-300 ease-in-out lg:hidden",
          isMenuOpen
            ? "visible translate-y-0 opacity-100"
            : "invisible -translate-y-4 opacity-0",
        )}
      >
        <nav className="divide-border flex flex-1 flex-col divide-y">
          
          {/* NEW: Start Trip with conditional routing for mobile */}
          <a
            href="/chat"
            onClick={handleStartTripNavigation}
            className="py-4 text-lg font-bold first:pt-0 !text-black hover:!text-black" 
          >
            Start Trip
          </a>

          {/* Original ITEMS map */}
          {ITEMS.map((link) => (
            <Link
              key={link.label}
              to={link.href}
              className="py-4 text-lg font-bold !text-black hover:!text-black" 
              onClick={() => setIsMenuOpen(false)}
            >
              {link.label}
            </Link>
          ))}
          
          {/* Mobile Login/User */}
          <div className="py-4">
            {user ? (
              <div className="flex flex-col gap-3">
                <div className="flex items-center gap-3 text-base font-semibold text-black">
                  <User className="w-5 h-5" />
                  <span>{user.email}</span>
                </div>
                <Button
                  variant="outline"
                  onClick={handleLogout}
                  className="w-full !bg-background justify-start gap-2 h-10"
                >
                  <LogOut className="w-5 h-5" />
                  Logout
                </Button>
              </div>
            ) : (
              <Link
                to="/login"
                className="flex w-full items-center justify-center rounded-md !bg-background border border-black px-4 py-3 text-base font-bold text-black hover:bg-gray-100" 
                onClick={() => setIsMenuOpen(false)}
              >
                Login
              </Link>
            )}
          </div>
        </nav>
      </div>
    </section>
  );
};