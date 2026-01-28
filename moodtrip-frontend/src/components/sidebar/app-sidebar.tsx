"use client"

import * as React from "react"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  useSidebar,
} from "@/components/ui/sidebar"
import { NavMain } from "@/components/sidebar/nav-main"
import { NavSecondary } from "@/components/sidebar/nav-secondary"
import { NavUser } from "@/components/sidebar/nav-user"
import { NavChats } from "@/components/sidebar/nav-chats"

import { getUser, saveUser, type AuthUser } from "@/api/auth"
import { fetchCurrentUser, type UserProfile } from "@/api/user"
import { onUserUpdated } from "@/lib/user-events"

export type ChatSummary = {
  id: string
  title: string
  icon?: React.ComponentType<{ className?: string }>
  preview?: string
  emotion?: string
}

type NavMainProps = React.ComponentProps<typeof NavMain>
type NavSecondaryProps = React.ComponentProps<typeof NavSecondary>

type Props = React.ComponentProps<typeof Sidebar> & {
  navMain: NavMainProps["items"]
  navSecondary: NavSecondaryProps["items"]
  chats: ChatSummary[]
  selectedChatId: string | null
  onNewChat: () => void
  onSelectChat: (id: string) => void
  onRenameChat?: (id: string, currentTitle: string) => void
  onDeleteChat?: (id: string) => void
  onShareChat?: (id: string) => void
  onIntroductionClick?: () => void
  onQuickStartClick?: () => void
  onRefreshChats?: () => void
  onLogoClick?: () => void
}

type LocalAuthUser = AuthUser & { avatarUrl?: string | null }

function mergeServerIntoLocal(
  server: UserProfile,
  prev: LocalAuthUser | null,
): LocalAuthUser {
  return {
    id: Number(server.id),
    username: server.username,
    email: server.email,
    createdAt: server.createdAt ?? prev?.createdAt,
    avatarUrl: server.avatarUrl ?? prev?.avatarUrl ?? null,
  }
}

export function AppSidebar({
  navMain,
  navSecondary,
  chats,
  selectedChatId,
  onNewChat,
  onSelectChat,
  onRenameChat,
  onDeleteChat,
  onShareChat,
  onIntroductionClick,
  onQuickStartClick,
  onRefreshChats,
  onLogoClick,
  ...props
}: Props) {
  const { open } = useSidebar()

  const [authUser, setAuthUser] = React.useState<LocalAuthUser | null>(() => {
    return (getUser() as LocalAuthUser | null) ?? null
  })

  React.useEffect(() => {
    let cancelled = false

      ; (async () => {
        const server = await fetchCurrentUser()
        if (cancelled) return

        if (server) {
          const next = mergeServerIntoLocal(server, authUser)
          saveUser(next as AuthUser)
          localStorage.setItem("auth_user", JSON.stringify(next))
          setAuthUser(next)
        } else {
          const local = getUser() as LocalAuthUser | null
          setAuthUser(local)
        }
      })()

    const off = onUserUpdated(() => {
      const local = getUser() as LocalAuthUser | null
      setAuthUser(local)
    })

    return () => {
      cancelled = true
      off()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  React.useEffect(() => {
    if (open && onRefreshChats) onRefreshChats()
  }, [open, onRefreshChats])

  return (
    <Sidebar variant="inset" {...props}>
      <SidebarHeader className="px-3 py-3">
        <button
          type="button"
          onClick={onLogoClick}
          style={{ all: "unset", cursor: "pointer" }}
          aria-label="Go to home"
          title="Home"
        >
          <div className="flex items-center gap-2">
            <img src="/logo.png" alt="Moodtrip" className="h-6 w-6 shrink-0" />
            <span className="text-sm font-semibold tracking-tight">Moodtrip</span>
          </div>
        </button>
      </SidebarHeader>

      <div className="shrink-0">
        <NavMain
          items={navMain}
          onNewChat={onNewChat}
          onIntroductionClick={onIntroductionClick}
          onQuickStartClick={onQuickStartClick}
        />
      </div>

      <SidebarContent>
        <NavChats
          chats={chats}
          selectedChatId={selectedChatId}
          onSelectChat={onSelectChat}
          onRenameChat={onRenameChat}
          onDeleteChat={onDeleteChat}
          onShareChat={onShareChat}
        />
      </SidebarContent>

      <SidebarFooter>
        <NavSecondary items={navSecondary} className="border-t pt-2" />
        {authUser ? (
          <NavUser
            user={{
              username: authUser.username,
              email: authUser.email,
              avatarUrl: authUser.avatarUrl ?? null,
            }}
          />
        ) : null}
      </SidebarFooter>
    </Sidebar>
  )
}
