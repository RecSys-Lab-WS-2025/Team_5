"use client"

import * as React from "react"
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
  const [searchTerm, setSearchTerm] = React.useState("")

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value)
  }

  const filteredChats = React.useMemo(() => {
    if (!searchTerm) {
      return chats
    }
    const lowerCaseSearchTerm = searchTerm.toLowerCase()
    return chats.filter(chat =>
      chat.title.toLowerCase().includes(lowerCaseSearchTerm)
    )
  }, [chats, searchTerm])

  return (
    <SidebarGroup className="group-data-[collapsible=icon]:hidden flex-1 flex flex-col min-h-0">
      
      {/* 1. FIXED HEADER SECTION (Label + SearchForm) */}
      <div className="shrink-0">
        <SidebarGroupLabel>Recent Chats</SidebarGroupLabel>
        <SearchForm 
          value={searchTerm} 
          onChange={handleSearchChange} 
          onSubmit={(e) => e.preventDefault()}
        />
      </div>

      {/* 2. SCROLLABLE CHAT LIST (SidebarMenu) */}
      <div className="flex-1 overflow-y-auto pt-2">
        <SidebarMenu>
          {filteredChats.map((item) => {
            const Icon = item.icon ?? Folder
            const isActive = item.id === selectedChatId
            return (
            <SidebarMenuItem key={item.id}>
              {/* SidebarMenuButton remains default to keep background/text hover effects */}
              <SidebarMenuButton 
                asChild 
                className={`
                  flex items-center gap-2 
                  ${isActive ? '!bg-accent' : ''}
                `}
                onClick={() => onSelectChat?.(item.id)} 
                aria-current={isActive ? "page" : undefined}
              >
                <a className="flex items-center gap-2 h-full">
                  <Icon className="size-4 shrink-0" />
                  <span className="leading-none flex items-center">{item.title}</span>
                </a>
              </SidebarMenuButton>

              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  {/* MODIFIED: Focus on removing all ring/border/shadow effects while allowing background/color hover */}
                  <SidebarMenuAction
                    showOnHover
                    className="
                      flex items-center justify-center p-0 m-0
                      
                      /* ONLY REMOVE RING, BORDER, SHADOW, AND FOCUS OUTLINE */
                      !ring-0 !ring-offset-0 !border-none !shadow-none
                      !hover:ring-0 !hover:ring-offset-0 !hover:border-none !hover:shadow-none
                      !focus-visible:outline-none !focus-visible:ring-0 !focus-visible:ring-offset-0
                      
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

          {filteredChats.length === 0 && searchTerm && (
            <SidebarMenuItem>
              <div className="text-xs text-muted-foreground px-4 py-2">
                  No chats matching "{searchTerm}"
              </div>
            </SidebarMenuItem>
          )}
        </SidebarMenu>
      </div>
    </SidebarGroup>
  )
}