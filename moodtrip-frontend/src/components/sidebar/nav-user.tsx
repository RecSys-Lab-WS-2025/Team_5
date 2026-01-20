"use client"

import * as React from "react"
import { ChevronsUpDown, LogOut } from "lucide-react"
import { useNavigate } from "react-router-dom"

import { logout, clearPendedRequests } from "@/api/auth"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { SidebarMenu, SidebarMenuButton, SidebarMenuItem, useSidebar } from "@/components/ui/sidebar"

import { EditProfileDialog } from "@/components/profile/EditProfileDialog"
import { getInitials } from "@/components/profile/getInitials"

export function NavUser({
  user,
}: {
  user: {
    username: string
    email: string
    avatarUrl?: string | null
  }
}) {
  const { isMobile } = useSidebar()
  const navigate = useNavigate()

  const [openEdit, setOpenEdit] = React.useState(false)
  const [localUser, setLocalUser] = React.useState(user)

  React.useEffect(() => setLocalUser(user), [user])

  const handleLogout = () => {
    clearPendedRequests()
    logout()
    navigate("/home")
  }

  const avatarSrc =
    localUser.avatarUrl && localUser.avatarUrl.trim().length > 0
      ? `${localUser.avatarUrl}${localUser.avatarUrl.includes("?") ? "&" : "?"}v=${Date.now()}`
      : undefined

  return (
    <>
      <SidebarMenu>
        <SidebarMenuItem>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <SidebarMenuButton
                size="lg"
                className="
                  data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground
                  !ring-0 !ring-offset-0 !border-none !shadow-none
                  !hover:ring-0 !hover:ring-offset-0 !hover:border-none !hover:shadow-none
                  !focus-visible:outline-none !focus-visible:ring-0 !focus-visible:ring-offset-0
                "
              >
                <Avatar className="h-8 w-8 rounded-lg">
                  <AvatarImage src={avatarSrc} alt={localUser.username} />
                  <AvatarFallback className="rounded-lg">
                    {getInitials(localUser.username)}
                  </AvatarFallback>
                </Avatar>

                <div className="grid flex-1 text-left text-sm leading-tight">
                  <span className="truncate font-medium">{localUser.username}</span>
                  <span className="truncate text-xs">{localUser.email}</span>
                </div>

                <ChevronsUpDown className="ml-auto size-4" />
              </SidebarMenuButton>
            </DropdownMenuTrigger>

            <DropdownMenuContent
              className="w-(--radix-dropdown-menu-trigger-width) min-w-56 rounded-lg"
              side={isMobile ? "bottom" : "right"}
              align="end"
              sideOffset={4}
            >
              <DropdownMenuItem onClick={() => setOpenEdit(true)} className="flex items-center gap-2">
                <Avatar className="h-8 w-8 rounded-lg">
                  <AvatarImage src={avatarSrc} alt={localUser.username} />
                  <AvatarFallback className="rounded-lg">
                    {getInitials(localUser.username)}
                  </AvatarFallback>
                </Avatar>
                <div className="grid flex-1 text-left text-sm leading-tight">
                  <span className="truncate font-medium">{localUser.username}</span>
                  <span className="truncate text-xs">{localUser.email}</span>
                </div>
              </DropdownMenuItem>

              <DropdownMenuSeparator />

              <DropdownMenuItem onClick={handleLogout}>
                <LogOut />
                Log out
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </SidebarMenuItem>
      </SidebarMenu>

      <EditProfileDialog
        open={openEdit}
        onOpenChange={setOpenEdit}
        user={localUser}
        onUserUpdated={(u) => setLocalUser((prev) => ({ ...prev, ...u }))}
      />
    </>
  )
}
