"use client"

import { ChevronRight, SquarePen, type LucideIcon } from "lucide-react"

import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible"
import {
  SidebarGroup,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
} from "@/components/ui/sidebar"

export function NavMain({
  items,
  onIntroductionClick,
  onQuickStartClick,
  onNewChat,
}: {
  items: {
    title: string
    url: string
    icon: LucideIcon
    isActive?: boolean
    items?: { title: string; url: string }[]
  }[]
  onIntroductionClick?: () => void
  onQuickStartClick?: () => void
  onNewChat?: () => void
}) {
  return (
    <SidebarGroup>
      <SidebarGroupLabel>Platform</SidebarGroupLabel>

      <SidebarMenu>
        {onNewChat ? (
          <SidebarMenuItem>
            <SidebarMenuButton
              onClick={onNewChat}
              tooltip="New chat"
              className="group flex items-center justify-between gap-2 focus-visible:outline-none"
            >
              <span className="flex items-center gap-2">
                <SquarePen className="size-4 shrink-0" />
                <span className="text-sm font-normal leading-none">
                  New chat
                </span>
              </span>
            </SidebarMenuButton>
          </SidebarMenuItem>
        ) : null}

        {items.map((item) => (
          <Collapsible key={item.title} asChild defaultOpen={item.isActive}>
            <SidebarMenuItem>
              <CollapsibleTrigger asChild>
                <SidebarMenuButton
                  asChild
                  tooltip={item.title}
                  className="group flex items-center justify-between gap-2 focus-visible:outline-none"
                >
                  <a
                    href={item.url}
                    className="flex w-full items-center justify-between gap-2"
                  >
                    <span className="flex items-center gap-2">
                      <item.icon className="size-4 shrink-0" />
                      <span className="leading-none">{item.title}</span>
                    </span>

                    {item.items?.length ? (
                      <ChevronRight className="size-4 shrink-0 -mt-px transition-transform duration-200 group-data-[state=open]:rotate-90" />
                    ) : null}
                  </a>
                </SidebarMenuButton>
              </CollapsibleTrigger>

              {item.items?.length ? (
                <CollapsibleContent>
                  <SidebarMenuSub>
                    {item.items.map((sub) => {
                      const isIntro = sub.title === "Introduction"
                      const isQuick = sub.title === "Quick-Start"

                      const handler =
                        (isIntro && onIntroductionClick) ||
                        (isQuick && onQuickStartClick) ||
                        undefined

                      return (
                        <SidebarMenuSubItem key={sub.title}>
                          <SidebarMenuSubButton asChild>
                            <a
                              href={sub.url}
                              className="text-sm font-normal"
                              onClick={(e) => {
                                if (!handler) return
                                e.preventDefault()
                                handler()
                              }}
                            >
                              {sub.title}
                            </a>
                          </SidebarMenuSubButton>
                        </SidebarMenuSubItem>
                      )
                    })}
                  </SidebarMenuSub>
                </CollapsibleContent>
              ) : null}
            </SidebarMenuItem>
          </Collapsible>
        ))}
      </SidebarMenu>
    </SidebarGroup>
  )
}
