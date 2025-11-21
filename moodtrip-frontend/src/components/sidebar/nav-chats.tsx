"use client"

import {
  Folder,
  MoreHorizontal,
  Share,
  Trash2,
} from "lucide-react"
import { SearchForm } from "@/components/sidebar/search-form"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  SidebarGroup,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuAction,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar"


export type NavChatsProps = {
  chats: {
    id: string
    title: string
    icon?: React.ComponentType<{ className?: string }>
  }[]
  selectedChatId?: string | null
  onSelectChat?: (id: string) => void
}

export function NavChats({ chats, selectedChatId, onSelectChat }: NavChatsProps) {
  const { isMobile } = useSidebar()

  return (
    <SidebarGroup className="group-data-[collapsible=icon]:hidden">
      <SidebarGroupLabel>Recent Chats</SidebarGroupLabel>
      <SearchForm />
      <SidebarMenu>
        {chats.map((item) => {
          const Icon = item.icon ?? Folder
          const isActive = item.id === selectedChatId
          return (
          <SidebarMenuItem key={item.id}>
            <SidebarMenuButton asChild className="flex items-center gap-2" onClick={() => onSelectChat?.(item.id)} aria-current={isActive ? "page" : undefined}>
              <a  className="flex items-center gap-2 h-full">
                <Icon className="size-4 shrink-0" />
                <span className="leading-none flex items-center">{item.title}</span>
              </a>
            </SidebarMenuButton>

            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <SidebarMenuAction
                  showOnHover
                  className="
                    flex items-center justify-center p-0 m-0
                    bg-transparent hover:bg-transparent
                    focus-visible:outline-none focus-visible:ring-0 
                    ring-0 ring-offset-0 hover:ring-0 focus:ring-0
                    border-none hover:border-none shadow-none
                    hover:text-muted-foreground/70
                    w-4 h-4 rounded-none
                    top-[calc(50%-0.5rem)]
                  "
                >
                  <MoreHorizontal className="size-4 shrink-0" />
                  <span className="sr-only">More</span>
                </SidebarMenuAction>
              </DropdownMenuTrigger>

              <DropdownMenuContent
                className="w-48"
                side={isMobile ? 'bottom' : 'right'}
                align={isMobile ? 'end' : 'start'}
              >
                <DropdownMenuItem>
                  <Folder className="text-muted-foreground" />
                  <span>Rename Chat</span>
                </DropdownMenuItem>
                <DropdownMenuItem>
                  <Share className="text-muted-foreground" />
                  <span>Share Chat</span>
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem>
                  <Trash2 className="text-muted-foreground" />
                  <span>Delete Chat</span>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </SidebarMenuItem>
        )
        })}

        <SidebarMenuItem>
          <SidebarMenuButton className="flex items-center gap-2">
            <MoreHorizontal className="size-4 shrink-0" />
            <span className="leading-none text-sm">More</span>
          </SidebarMenuButton>
        </SidebarMenuItem>
      </SidebarMenu>
    </SidebarGroup>
  )
}
