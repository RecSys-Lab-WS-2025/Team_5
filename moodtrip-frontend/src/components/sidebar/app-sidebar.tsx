"use client";

import * as React from "react";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar";
import { NavMain } from "@/components/sidebar/nav-main";
import { NavSecondary } from "@/components/sidebar/nav-secondary";
import { NavUser } from "@/components/sidebar/nav-user";
import { NavChats } from "@/components/sidebar/nav-chats";
import { Plus } from "lucide-react";

export type ChatSummary = {
  id: string;
  title: string;
  icon?: React.ComponentType<{ className?: string }>;
  preview?: string;
};

type NavMainProps = React.ComponentProps<typeof NavMain>;
type NavSecondaryProps = React.ComponentProps<typeof NavSecondary>;

type Props = React.ComponentProps<typeof Sidebar> & {
  user: { name: string; email: string; avatar?: string };
  navMain: NavMainProps["items"];
  navSecondary: NavSecondaryProps["items"];
  chats: ChatSummary[];
  selectedChatId: string | null;
  onNewChat: () => void;
  onSelectChat: (id: string) => void;
  onIntroductionClick?: () => void;
  onQuickStartClick?: () => void;
  onRefreshChats?: () => void;
};

export function AppSidebar({
  user,
  navMain,
  navSecondary,
  chats,
  selectedChatId,
  onNewChat,
  onSelectChat,
  onIntroductionClick,
  onQuickStartClick,
  onRefreshChats,
  ...props
}: Props) {
  const { open } = useSidebar();

  React.useEffect(() => {
    if (open && onRefreshChats) {
      onRefreshChats();
    }
  }, [open, onRefreshChats]);

  return (
    <Sidebar variant="inset" {...props}>
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton size="lg" asChild>
              <button
                type="button"
                onClick={onNewChat}
                className="flex w-full items-center justify-center gap-2 rounded-lg px-4 py-2 transition-colors duration-200 !bg-[#1a1a1a] hover:!bg-[#2a2a2a] active:!bg-[#333333] !text-white focus-visible:!outline-none focus-visible:!ring-0 focus-visible:!ring-offset-0"
              >
                <Plus className="h-4 w-4" />
                <span className="text-sm font-medium">New Chat</span>
              </button>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>

      <SidebarContent>
        <NavMain
          items={navMain}
          onIntroductionClick={onIntroductionClick}
          onQuickStartClick={onQuickStartClick}
        />

        <NavChats
          chats={chats}
          selectedChatId={selectedChatId}
          onSelectChat={onSelectChat}
        />

        <NavSecondary items={navSecondary} className="mt-auto" />
      </SidebarContent>

      <SidebarFooter>
        <NavUser user={user} />
      </SidebarFooter>
    </Sidebar>
  );
}
