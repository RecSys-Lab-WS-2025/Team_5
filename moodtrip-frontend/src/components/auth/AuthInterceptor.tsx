"use client";

import React, { useEffect, useState } from "react";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { refreshToken, saveToken, saveUser, logout, getToken, notifySessionUpdated, clearPendedRequests } from "@/api/auth";
import { Loader2, AlertCircle, Clock, LogOut } from "lucide-react";

export function AuthInterceptor({ children }: { children: React.ReactNode }) {
    const [isOpen, setIsOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const handleUnauthorized = () => {
            const hasToken = !!getToken();


            if (!hasToken) {
                // If there is no token locally, this is an unauthenticated visit; redirect to login directly without showing the renewal dialog.
                window.location.href = "/login";
                return;
            }

            setIsOpen(true);
        };

        window.addEventListener("moodtrip-unauthorized", handleUnauthorized);
        return () => {
            window.removeEventListener("moodtrip-unauthorized", handleUnauthorized);
        };
    }, []);

    const handleExtend = async () => {
        setIsLoading(true);
        setError(null);

        const currentToken = getToken();
        if (!currentToken) {
            console.error("AuthInterceptor: No token found in localStorage.");
            setError("No session token found. Please sign out and sign in again.");
            setIsLoading(false);
            return;
        }

        try {

            const data = await refreshToken();


            if (data.token && data.user) {
                saveToken(data.token);
                saveUser(data.user);
                setIsOpen(false);

                notifySessionUpdated();
            } else {
                console.error("AuthInterceptor: Invalid response data format:", data);
                setError("Session extension failed. Please log in again.");
            }
        } catch (err: unknown) {
            console.error("AuthInterceptor: Extension request failed:", err);
            setError("Your session has expired completely (or server error). Please sign out and sign in again.");
        } finally {
            setIsLoading(false);
        }
    };

    const handleLogout = () => {

        clearPendedRequests(); // Reject all waiting promises
        logout();
        window.location.href = "/login";
    };

    return (
        <>
            {children}
            <Dialog open={isOpen} onOpenChange={(open: boolean) => {
                // Force user to make a choice if session expired, but explain why closing is blocked
                if (!open && isOpen) {
                    setError("You need to extend your session or sign out before continuing.");
                    return;
                }
                setIsOpen(open);
            }}>
                <DialogContent className="sm:max-w-[400px]">
                    <DialogHeader>
                        <DialogTitle className="flex items-center gap-2">
                            <Clock className="h-5 w-5 text-orange-500" />
                            Session Expiring
                        </DialogTitle>
                        <DialogDescription>
                            Your session has timed out. Would you like to extend it or sign out?
                        </DialogDescription>
                    </DialogHeader>

                    <div className="py-6 space-y-4">
                        {error && (
                            <div className="flex items-center gap-2 text-sm text-red-600 bg-red-50 p-3 rounded-md border border-red-100">
                                <AlertCircle className="h-4 w-4" />
                                <span>{error}</span>
                            </div>
                        )}

                        <div className="flex flex-col gap-3">
                            <Button
                                onClick={handleExtend}
                                className="w-full bg-blue-50 hover:bg-blue-100 text-blue-900 border border-blue-200 font-bold h-11"
                                disabled={isLoading}
                            >
                                {isLoading ? (
                                    <>
                                        <Loader2 className="mr-2 h-4 w-4 animate-spin text-blue-900" />
                                        Extending...
                                    </>
                                ) : (
                                    "Extend Session"
                                )}
                            </Button>

                            <Button
                                variant="outline"
                                onClick={handleLogout}
                                className="w-full border-gray-200 hover:bg-gray-50 text-gray-700 h-11"
                                disabled={isLoading}
                            >
                                <LogOut className="mr-2 h-4 w-4" />
                                Sign Out
                            </Button>
                        </div>
                    </div>
                </DialogContent>
            </Dialog>
        </>
    );
}
