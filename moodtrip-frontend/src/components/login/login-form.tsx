import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";

import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
  FieldSeparator,
} from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { login, saveToken, OAUTH } from "@/api/auth";

export function LoginForm({
  className,
  ...props
}: React.ComponentProps<"div">) {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setPending(true);
    try {
      const { token } = await login({ email, password });
      if (token) {
        saveToken(token);
      }
      navigate("/chat");
    } catch (err: any) {
      setError(err?.message || "Login failed");
    } finally {
      setPending(false);
    }
  };

  const handleSpotifyLogin = () => {
    window.location.href = OAUTH.SPOTIFY;
  };

  const handleGoogleLogin = () => {
    window.location.href = OAUTH.GOOGLE;
  };

  return (
    <div className={cn("flex flex-col gap-6", className)} {...props}>
      <Card>
        <CardHeader className="text-center">
          <CardTitle className="text-xl">Welcome back</CardTitle>
          <CardDescription>
            Login with your Spotify or Google account
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit}>
            <FieldGroup>
              <Field>
                <Button
                  variant="outline"
                  type="button"
                  className="w-full justify-center gap-2"
                  onClick={handleSpotifyLogin}
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    viewBox="0 0 24 24"
                    fill="currentColor"
                    className="h-4 w-4"
                  >
                    <path d="M12 0C5.373 0 0 5.373 0 12c0 6.627 5.373 12 12 12s12-5.373 12-12C24 5.373 18.627 0 12 0zm5.457 17.438a.75.75 0 0 1-1.033.25c-2.83-1.73-6.39-2.123-10.593-1.165a.75.75 0 1 1-.326-1.46c4.526-1.012 8.44-.56 11.546 1.28a.75.75 0 0 1 .406 1.095zm1.457-3.238a.9.9 0 0 1-1.238.3c-3.243-1.989-8.19-2.565-12.033-1.41a.9.9 0 0 1-.516-1.73c4.252-1.27 9.628-.637 13.3 1.64a.9.9 0 0 1 .487 1.2zm.185-3.356a1.05 1.05 0 0 1-1.448.352c-3.704-2.256-9.362-2.76-13.216-1.52a1.05 1.05 0 1 1-.632-2.004c4.327-1.364 10.54-.802 14.68 1.71a1.05 1.05 0 0 1 .616 1.462z" />
                  </svg>
                  Login with Spotify
                </Button>
                <Button
                  variant="outline"
                  type="button"
                  className="w-full justify-center gap-2"
                  onClick={handleGoogleLogin}
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    viewBox="0 0 24 24"
                    className="h-4 w-4"
                  >
                    <path
                      d="M12.48 10.92v3.28h7.84c-.24 1.84-.853 3.187-1.787 4.133-1.147 1.147-2.933 2.4-6.053 2.4-4.827 0-8.6-3.893-8.6-8.72s3.773-8.72 8.6-8.72c2.6 0 4.507 1.027 5.907 2.347l2.307-2.307C18.747 1.44 16.133 0 12.48 0 5.867 0 .307 5.387.307 12s5.56 12 12.173 12c3.573 0 6.267-1.173 8.373-3.36 2.16-2.16 2.84-5.213 2.84-7.667 0-.76-.053-1.467-.173-2.053H12.48z"
                      fill="currentColor"
                    />
                  </svg>
                  Login with Google
                </Button>
              </Field>
              <FieldSeparator className="*:data-[slot=field-separator-content]:bg-card">
                Or continue with
              </FieldSeparator>
              <Field>
                <FieldLabel htmlFor="email">Email</FieldLabel>
                <Input
                  id="email"
                  type="email"
                  placeholder="m@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </Field>
              <Field>
                <div className="flex items-center">
                  <FieldLabel htmlFor="password">Password</FieldLabel>
                </div>
                <Input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </Field>
              {error && (
                <FieldDescription className="text-red-600">
                  {error}
                </FieldDescription>
              )}
              <Field>
                <Button type="submit" disabled={pending}>
                  {pending ? "Signing in..." : "Login"}
                </Button>
                <FieldDescription className="text-center">
                  Don&apos;t have an account?
                  <Link
                    to="/signup"
                    className="ml-1 font-medium text-primary hover:underline hover:text-primary/80"
                  >
                    Sign up
                  </Link>
                </FieldDescription>
              </Field>
            </FieldGroup>
          </form>
        </CardContent>
      </Card>
      <FieldDescription className="px-6 text-center">
        By clicking continue, you agree to our <a href="#">Terms of Service</a>{" "}
        and <a href="#">Privacy Policy</a>.
      </FieldDescription>
    </div>
  );
}
