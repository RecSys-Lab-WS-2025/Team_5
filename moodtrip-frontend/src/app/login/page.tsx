import { LoginForm } from "@/components/login/login-form";
import { Link } from "react-router-dom";

export default function LoginPage() {
  return (
    <div className="relative flex min-h-svh items-center justify-center p-6 md:p-10">
      <div
        className="absolute inset-0 bg-cover bg-center bg-no-repeat"
        style={{ backgroundImage: "url('/bg.png')" }}
      />

      <div className="relative z-10 w-full max-w-md rounded-3xl border border-white/30 bg-white/10 px-8 py-10 shadow-xl shadow-sky-900/10 backdrop-blur-2xl flex flex-col items-center gap-6">
        <Link
          to="/"
          className="flex items-center gap-2 font-bold text-2xl !text-gray-800 !hover:text-gray-800 !hover:no-underline"
        >
          <img
            src="/favicon.png"
            alt="Moodtrip logo"
            width={50}
            height={50}
            className="object-contain"
          />
          Moodtrip.
        </Link>

        <LoginForm className="w-full" />
      </div>
    </div>
  );
}
