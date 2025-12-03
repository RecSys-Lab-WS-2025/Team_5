"use client";

import { useState, useEffect } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { ChevronRight, LogOut, User } from "lucide-react";


import { Button } from "@/components/ui/button";
import {
  NavigationMenu,
  NavigationMenuContent,
  NavigationMenuItem,
  NavigationMenuLink,
  NavigationMenuList,
  NavigationMenuTrigger,
} from "@/components/ui/navigation-menu";
import { cn } from "@/lib/utils";
import { getUser, logout, type AuthUser } from "@/api/auth";

const ITEMS = [
  {
    label: "Features",
    href: "#features",
    dropdownItems: [
      {
        title: "Modern product teams",
        href: "/#feature-modern-teams",
        description:
          "Mainline is built on the habits that make the best product teams successful",
      },
      {
        title: "Resource Allocation",
        href: "/#resource-allocation",
        description: "Mainline your resource allocation and execution",
      },
    ],
  },
  { label: "About Us", href: "/about" },
  { label: "Pricing", href: "/pricing" },
  { label: "FAQ", href: "/faq" },
  { label: "Contact", href: "/contact" },
];

export const Navbar = () => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [openDropdown, setOpenDropdown] = useState<string | null>(null);
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
    window.dispatchEvent(new Event('userLogin')); // Notify other components
    navigate("/");
  };

  return (
    <section
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
          <span className="text-lg font-semibold !text-black select-none group-hover:!text-black">
            Moodtrip
          </span>
        </Link>

        {/* Desktop Navigation */}
        <NavigationMenu className="max-lg:hidden">
          <NavigationMenuList>
            {ITEMS.map((link) =>
              link.dropdownItems ? (
                <NavigationMenuItem key={link.label}>
                  <NavigationMenuTrigger className="data-[state=open]:bg-accent/50 !bg-transparent !text-black px-1.5 hover:!text-black whitespace-nowrap">
                    {link.label}
                  </NavigationMenuTrigger>
                  <NavigationMenuContent>
                    <ul className="w-[400px] space-y-2 p-4">
                      {link.dropdownItems.map((item) => (
                        <li key={item.title}>
                          <NavigationMenuLink asChild>
                            <Link
                              to={item.href}
                              className="group hover:bg-accent hover:text-accent-foreground focus:bg-accent focus:text-accent-foreground flex items-center gap-4 rounded-md p-3 leading-none no-underline outline-hidden transition-colors select-none"
                            >
                              <div className="space-y-1.5 transition-transform duration-300 group-hover:translate-x-1">
                                <div className="text-sm leading-none font-medium">
                                  {item.title}
                                </div>
                                <p className="text-muted-foreground line-clamp-2 text-sm leading-snug">
                                  {item.description}
                                </p>
                              </div>
                            </Link>
                          </NavigationMenuLink>
                        </li>
                      ))}
                    </ul>
                  </NavigationMenuContent>
                </NavigationMenuItem>
              ) : (
                <NavigationMenuItem key={link.label}>
                  <Link
                    to={link.href}
                    className={cn(
                      "relative bg-transparent px-1.5 text-sm font-medium !text-black hover:opacity-75 whitespace-nowrap",
                      pathname === link.href && "!text-black"
                    )}
                  >
                    {link.label}
                  </Link>
                </NavigationMenuItem>
              ),
            )}
          </NavigationMenuList>
        </NavigationMenu>

        {/* Right side */}
        <div className="flex items-center gap-2.5">
          {user ? (
            <div className="flex items-center gap-3 max-lg:hidden">
              <div className="flex items-center gap-2 text-sm font-medium text-black">
                <User className="w-4 h-4" />
                <span>{user.email}</span>
              </div>
              <Button
                variant="ghost"
                size="icon"
                onClick={handleLogout}
                className="h-8 w-8 hover:bg-gray-100 rounded-full"
                title="Logout"
              >
                <LogOut className="w-4 h-4" />
              </Button>
            </div>
          ) : (
            <Link to="/login" className="max-lg:hidden">
              <Button className="!bg-white hover:!bg-gray-200 !text-black">
                <span className="relative z-10 !text-black">Login</span>
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
          {ITEMS.map((link) =>
            link.dropdownItems ? (
              <div key={link.label} className="py-4 first:pt-0 last:pb-0">
                <button
                  onClick={() =>
                    setOpenDropdown(openDropdown === link.label ? null : link.label)
                  }
                  className="!text-black flex w-full items-center justify-between text-base font-medium"
                >
                  {link.label}
                  <ChevronRight
                    className={cn(
                      "size-4 transition-transform duration-200",
                      openDropdown === link.label ? "rotate-90" : "",
                    )}
                  />
                </button>
                <div
                  className={cn(
                    "overflow-hidden transition-all duration-300",
                    openDropdown === link.label
                      ? "mt-4 max-h-[1000px] opacity-100"
                      : "max-h-0 opacity-0",
                  )}
                >
                  <div className="bg-muted/50 space-y-3 rounded-lg p-4">
                    {link.dropdownItems.map((item) => (
                      <Link
                        key={item.title}
                        to={item.href}
                        className="group hover:bg-accent block rounded-md p-2 transition-colors"
                        onClick={() => {
                          setIsMenuOpen(false);
                          setOpenDropdown(null);
                        }}
                      >
                        <div className="transition-transform duration-200 group-hover:translate-x-1">
                          <div className="font-medium"> {item.title} </div>
                          <p className="text-muted-foreground mt-1 text-sm">
                            {item.description}
                          </p>
                        </div>
                      </Link>
                    ))}
                  </div>
                </div>
              </div>
            ) : (
              <Link
                key={link.label}
                to={link.href}
                className="py-4 text-base font-medium first:pt-0 last:pb-0 !text-black hover:!text-black"
                onClick={() => setIsMenuOpen(false)}
              >
                {link.label}
              </Link>
            ),
          )}
          {/* Mobile Login/User */}
          <div className="py-4">
            {user ? (
              <div className="flex flex-col gap-2">
                <div className="flex items-center gap-2 text-sm font-medium text-black">
                  <User className="w-4 h-4" />
                  <span>{user.email}</span>
                </div>
                <Button
                  variant="outline"
                  onClick={handleLogout}
                  className="w-full justify-start gap-2"
                >
                  <LogOut className="w-4 h-4" />
                  Logout
                </Button>
              </div>
            ) : (
              <Link
                to="/login"
                className="flex w-full items-center justify-center rounded-md bg-black px-4 py-2 text-sm font-medium text-white hover:bg-gray-800"
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
