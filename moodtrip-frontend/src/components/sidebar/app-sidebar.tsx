"use client"

import * as React from "react"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar"
import { NavMain } from "@/components/sidebar/nav-main"
import { NavSecondary } from "@/components/sidebar/nav-secondary"
import { NavUser } from "@/components/sidebar/nav-user"
import { NavChats } from "@/components/sidebar/nav-chats"
import { Plus } from "lucide-react"

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
  ...props
}: Props) {
  const { open } = useSidebar()

  const [authUser, setAuthUser] = React.useState<LocalAuthUser | null>(() => {
    return (getUser() as LocalAuthUser | null) ?? null
  })

  React.useEffect(() => {
    let cancelled = false

    ;(async () => {
      const server = await fetchCurrentUser()
      if (cancelled) return

      if (server) {
        const next = mergeServerIntoLocal(server, authUser)

        // saveUser expects AuthUser; runtime JSON can still include extra fields.
        saveUser(next as AuthUser)

        // Ensure avatarUrl is persisted even if AuthUser doesn't include it in TS type.
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
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton size="lg" asChild>
              <button
                type="button"
                onClick={onNewChat}
                className="flex w-full items-center justify-center gap-2 rounded-lg px-4 py-2 transition-colors duration-200 !bg-blue-800 hover:!bg-[#142c65] active:!bg-[#0e204a] !text-white focus-visible:!outline-none focus-visible:!ring-0 focus-visible:!ring-offset-0"
              >
                <Plus className="h-4 w-4" />
                <span className="text-sm font-semibold">New Chat</span>
              </button>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>

      <div className="shrink-0">
        <NavMain
          items={navMain}
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
