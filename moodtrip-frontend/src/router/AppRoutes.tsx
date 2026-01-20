import { Routes, Route, Navigate } from "react-router-dom";
import { AppLayout } from "@/layouts/AppLayout";
import { HomePage } from "@/app/dashboard/HomePage";
import Chatbot from "@/app/dashboard/Chatbot";
import LoginPage from "@/app/login/page";
import SignupPage from "@/app/signup/page";
import { RouteDetailsPage } from "@/app/route-details/RouteDetailsPage";

import AboutPage from "@/app/about/page";
import FAQPage from "@/app/faq/page";
import ContactPage from "@/app/contact/page";

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/chat" element={<Chatbot />} />
        <Route path="/route-details" element={<RouteDetailsPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />

        <Route path="/about" element={<AboutPage />} />
        <Route path="/faq" element={<FAQPage />} />
        <Route path="/contact" element={<ContactPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
