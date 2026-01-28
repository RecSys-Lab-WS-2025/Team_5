"use client";

import * as React from "react";
import { Folder, MoreHorizontal, Share, Trash2 } from "lucide-react";
import { SearchForm } from "@/components/sidebar/search-form";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  SidebarGroup,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuAction,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar";

export type NavChatsProps = {
  chats: {
    id: string;
    title: string;
    icon?: React.ComponentType<{ className?: string }>;
  }[];
  selectedChatId?: string | null;
  onSelectChat?: (id: string) => void;
  onRenameChat?: (id: string, currentTitle: string) => void;
  onDeleteChat?: (id: string) => void;
  onShareChat?: (id: string) => void;
};

export function NavChats({
  chats,
  selectedChatId,
  onSelectChat,
  onRenameChat,
  onDeleteChat,
  onShareChat,
}: NavChatsProps) {
  const { isMobile } = useSidebar();
  const [searchTerm, setSearchTerm] = React.useState("");

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value);
  };

  const filteredChats = React.useMemo(() => {
    if (!searchTerm) {
      return chats;
    }
    const lowerCaseSearchTerm = searchTerm.toLowerCase();
    return chats.filter((chat) =>
      chat.title.toLowerCase().includes(lowerCaseSearchTerm)
    );
  }, [chats, searchTerm]);

  return (
    <SidebarGroup className="group-data-[collapsible=icon]:hidden flex-1 flex flex-col min-h-0">
      <div className="shrink-0">
        <SidebarGroupLabel>Recent Chats</SidebarGroupLabel>
        <SearchForm
          value={searchTerm}
          onChange={handleSearchChange}
          onSubmit={(e) => e.preventDefault()}
        />
      </div>

      <div className="flex-1 overflow-y-auto pt-2">
        <SidebarMenu>
          {filteredChats.map((item) => {
            const Icon = item.icon ?? Folder;
            const isActive = item.id === selectedChatId;
            return (
              <SidebarMenuItem key={item.id}>
                <SidebarMenuButton
                  asChild
                  className={`flex items-center gap-2 ${
                    isActive ? "!bg-accent" : ""
                  }`}
                  onClick={() => onSelectChat?.(item.id)}
                  aria-current={isActive ? "page" : undefined}
                >
                  <a className="flex h-full min-w-0 items-center gap-2">
                    <Icon className="size-4 shrink-0" />
                    <span className="flex-1 min-w-0 truncate leading-none">
                      {item.title}
                    </span>
                  </a>
                </SidebarMenuButton>

                <DropdownMenu modal={false}>
                  <DropdownMenuTrigger asChild>
                    <SidebarMenuAction
                      showOnHover
                      className="
                        !p-0 !m-0 
                        w-4 h-4 
                        flex items-center justify-center
                        !ring-0 !ring-offset-0 !border-none !shadow-none
                        hover:!bg-transparent active:!bg-transparent
                      "
                    >
                      <MoreHorizontal className="size-4 shrink-0" />
                      <span className="sr-only">More</span>
                    </SidebarMenuAction>
                  </DropdownMenuTrigger>

                  <DropdownMenuContent
                    className="w-48"
                    side={isMobile ? "bottom" : "right"}
                    align={isMobile ? "end" : "start"}
                  >
                    <DropdownMenuItem
                      onClick={() => onRenameChat?.(item.id, item.title)}
                    >
                      <Folder className="text-muted-foreground" />
                      <span>Rename Chat</span>
                    </DropdownMenuItem>

                    <DropdownMenuItem onClick={() => onShareChat?.(item.id)}>
                      <Share className="text-muted-foreground" />
                      <span>Share Chat</span>
                    </DropdownMenuItem>

                    <DropdownMenuSeparator />

                    <DropdownMenuItem
                      className="text-destructive focus:text-destructive"
                      onClick={() => onDeleteChat?.(item.id)}
                    >
                      <Trash2 className="text-muted-foreground" />
                      <span>Delete Chat</span>
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </SidebarMenuItem>
            );
          })}

          {filteredChats.length === 0 && searchTerm && (
            <SidebarMenuItem>
              <div className="px-4 py-2 text-xs text-muted-foreground">
                No chats matching "{searchTerm}"
              </div>
            </SidebarMenuItem>
          )}
        </SidebarMenu>
      </div>
    </SidebarGroup>
  );
}
