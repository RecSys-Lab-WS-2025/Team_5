import * as React from "react"
import { Smile, Frown, Meh, Heart, Send, Compass } from "lucide-react"

import { SidebarTrigger } from "@/components/ui/sidebar"
import { AppSidebar } from "@/components/app-sidebar"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

function Chip({ children, onClick }: { children: React.ReactNode; onClick?: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="rounded-full border px-3 py-1.5 text-sm hover:bg-muted/60 transition"
    >
      {children}
    </button>
  )
}

function ChatWelcome({ onPick }: { onPick: (text: string) => void }) {
  return (
    <div className="mx-auto w-full max-w-3xl px-6 pt-10 pb-24">


      <h1 className="text-2xl md:text-3xl font-semibold tracking-tight">Hey there 👋 How have you been lately?</h1>
      <p className="mt-3 text-muted-foreground leading-7">
        I’d love to hear how things have been going for you recently. Maybe something fun happened, or perhaps it’s been a bit stressful. 
        Whatever it is, feel free to share — I’m here to listen and help you unwind through travel suggestions that match your mood.
      </p>

      <div className="mt-8 rounded-2xl border p-5">
        <div className="text-sm text-muted-foreground mb-3">You can start by telling me something like:</div>
        <div className="flex flex-wrap gap-2">
          <Chip onClick={() => onPick("I've been really happy lately, everything feels light and fun.")}>I've been feeling really happy lately</Chip>
          <Chip onClick={() => onPick("It's been a bit stressful at work recently.")}>Work has been a bit stressful</Chip>
          <Chip onClick={() => onPick("Honestly, I’ve been feeling a little down these days.")}>Feeling a little down</Chip>
          <Chip onClick={() => onPick("Things have been calm and peaceful lately.")}>Calm and peaceful</Chip>
          <Chip onClick={() => onPick("I've had some exciting news recently!")}>I've had something exciting happen</Chip>
        </div>
      </div>

      <p className="mt-6 text-sm text-muted-foreground">
        Just talk to me like you would to a friend — no rush, no judgment. Once I get a sense of your mood, I'll find a travel route to match it.
      </p>
    </div>
  )
}

function Composer() {
  const [value, setValue] = React.useState("")

  return (
    <div className="mx-auto w-full max-w-3xl px-6">
      <div className="sticky bottom-4">
        <div className="rounded-2xl border bg-background shadow-sm p-2 md:p-3">
          <form
            onSubmit={(e) => {
              e.preventDefault()
              // UI only, no chat logic yet
            }}
          >
            <div className="flex items-end gap-2">
              <div className="flex-1">
                <label htmlFor="chat-input" className="sr-only">Input</label>
                <textarea
                  id="chat-input"
                  value={value}
                  onChange={(e) => setValue(e.target.value)}
                  placeholder="How have you been feeling lately? (Enter to send, Shift+Enter for newline)"
                  rows={1}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && !e.shiftKey) {
                      e.preventDefault()
                    }
                  }}
                  className="w-full resize-none bg-transparent outline-none placeholder:text-muted-foreground/70 px-3 py-2 text-sm md:text-base"
                />
              </div>
              <Button type="submit" size="icon" className="shrink-0" variant="secondary" aria-label="Send">
                <Send className="h-4 w-4" />
              </Button>
            </div>
            <div className="px-3 pt-2 text-[11px] text-muted-foreground">
              I'll listen to how you're feeling and use that to suggest travel ideas that might lift your mood.
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}

export default function Chat() {
  const [draft, setDraft] = React.useState("")

  return (
    <div className="flex h-dvh w-full bg-background">
      <AppSidebar />
      <main className="flex-1 overflow-y-auto">
        <header className="sticky top-0 z-10 flex h-12 items-center gap-2 border-b bg-background/80 backdrop-blur px-3">
          <SidebarTrigger className="mr-1" />
          <span className="text-sm text-muted-foreground">Travel Mood Assistant</span>
        </header>

        <ChatWelcome onPick={(text) => {
          setDraft(text)
          const el = document.getElementById("chat-input") as HTMLTextAreaElement | null
          if (el) el.focus()
        }} />
        <Composer />
      </main>
    </div>
  )
}
