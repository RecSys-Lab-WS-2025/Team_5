import { LoginForm } from "@/components/login/login-form";

export default function LoginPage() {
  return (
    <div className="bg-blue-50 flex min-h-svh flex-col items-center justify-center gap-6 p-6 md:p-10">
      <div className="flex w-full max-w-sm flex-col gap-6">
        <a
          href="#"
          className="flex items-center gap-2 self-center font-medium text-gray-800"
        >
          <img
            src="/favicon.png"
            alt="Moodtrip logo"
            width={30}
            height={30}
            className="object-contain"
          />
          Moodtrip.
        </a>
        <LoginForm />
      </div>
    </div>
  );
}
