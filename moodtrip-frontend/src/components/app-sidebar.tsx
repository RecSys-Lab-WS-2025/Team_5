import * as React from "react"
import {
  SquarePen,
  Search,
  Smile,
  Meh,
  Frown,
  MoreHorizontal,
  Pencil,
  Link,
  Trash,
  User,
  LayoutDashboard,
  LogOut,
  Settings as SettingsIcon,
} from "lucide-react"

import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuAction,
  SidebarFooter,
} from "@/components/ui/sidebar"

import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu"

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog"

import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction,
} from "@/components/ui/alert-dialog"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Avatar, AvatarImage, AvatarFallback } from "@/components/ui/avatar"

const items = [
  { title: "New Chat", url: "#", icon: SquarePen },
  { title: "Search chats", url: "#", icon: Search },
]

type ChatItem = {
  id: string
  title: string
  url: string
  icon: React.ComponentType<any>
}

const initialChats: ChatItem[] = [
  { id: "1", title: "Happy Mood", url: "#", icon: Smile },
  { id: "2", title: "Neutral Mood", url: "#", icon: Meh },
  { id: "3", title: "Sad Mood", url: "#", icon: Frown },
]

const user = {
  name: "Valentina",
  email: "valentina@example.com",
  avatarUrl: "",
  initials: "VA",
}

function MoodHistoryItem({
  chat,
  onRename,
  onDelete,
}: {
  chat: ChatItem
  onRename: (id: string, newTitle: string) => void
  onDelete: (id: string) => void
}) {
  const [renameOpen, setRenameOpen] = React.useState(false)
  const [shareOpen, setShareOpen] = React.useState(false)
  const [deleteOpen, setDeleteOpen] = React.useState(false)
  const [menuOpen, setMenuOpen] = React.useState(false)
  const [newTitle, setNewTitle] = React.useState(chat.title)

  const shareUrl =
    (typeof window !== "undefined" ? window.location.origin : "https://example.com") +
    `/chat/${chat.id}`

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(shareUrl)
    } catch {}
  }

  const Icon = chat.icon

  return (
    <SidebarMenuItem>
      <SidebarMenuButton asChild>
        <a href={chat.url}>
          <Icon className="h-6 w-6" />
          <span>{chat.title}</span>
        </a>
      </SidebarMenuButton>

      <DropdownMenu open={menuOpen} onOpenChange={setMenuOpen}>
        <DropdownMenuTrigger asChild>
          <SidebarMenuAction className="focus:outline-none focus-visible:outline-none">
            <MoreHorizontal className="h-5 w-5" />
          </SidebarMenuAction>
        </DropdownMenuTrigger>
        <DropdownMenuContent side="right" align="start">
          <DropdownMenuItem
            onSelect={(e) => {
              e.preventDefault()
              setMenuOpen(false)
              setRenameOpen(true)
            }}
          >
            <Pencil className="mr-2 h-4 w-4" />
            <span>Rename</span>
          </DropdownMenuItem>

          <DropdownMenuItem
            onSelect={(e) => {
              e.preventDefault()
              setMenuOpen(false)
              setShareOpen(true)
            }}
          >
            <Link className="mr-2 h-4 w-4" />
            <span>Share</span>
          </DropdownMenuItem>

          <DropdownMenuSeparator />

          <DropdownMenuItem
            onSelect={(e) => {
              e.preventDefault()
              setMenuOpen(false)
              setDeleteOpen(true)
            }}
          >
            <Trash className="mr-2 h-4 w-4" />
            <span>Delete</span>
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <Dialog
        open={renameOpen}
        onOpenChange={(open) => {
          setRenameOpen(open)
          if (!open) setMenuOpen(false)
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Rename chat</DialogTitle>
            <DialogDescription>Update the title of this chat.</DialogDescription>
          </DialogHeader>
          <Input
            value={newTitle}
            onChange={(e) => setNewTitle(e.target.value)}
            placeholder="Enter a new title"
          />
          <DialogFooter>
            <Button variant="secondary" onClick={() => setRenameOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="secondary"
              onClick={() => {
                onRename(chat.id, newTitle.trim() || chat.title)
                setRenameOpen(false)
              }}
            >
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={shareOpen}
        onOpenChange={(open) => {
          setShareOpen(open)
          if (!open) setMenuOpen(false)
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Share chat</DialogTitle>
            <DialogDescription>Copy the link to share this chat.</DialogDescription>
          </DialogHeader>
          <div className="flex items-center gap-2">
            <Input readOnly value={shareUrl} />
            <Button onClick={handleCopy}>Copy</Button>
          </div>
          <DialogFooter>
            <Button variant="secondary" onClick={() => setShareOpen(false)}>
              Close
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <AlertDialog
        open={deleteOpen}
        onOpenChange={(open) => {
          setDeleteOpen(open)
          if (!open) setMenuOpen(false)
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete this chat?</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              className="text-red-600 hover:text-red-700"
              onClick={() => {
                onDelete(chat.id)
                setDeleteOpen(false)
              }}
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </SidebarMenuItem>
  )
}

export function AppSidebar() {
  const [chats, setChats] = React.useState<ChatItem[]>(initialChats)

  const handleRename = (id: string, newTitle: string) => {
    setChats((prev) => prev.map((c) => (c.id === id ? { ...c, title: newTitle } : c)))
  }

  const handleDelete = (id: string) => {
    setChats((prev) => prev.filter((c) => c.id !== id))
  }

  return (
    <Sidebar collapsible="icon" className="flex flex-col h-full group/sidebar">
      <SidebarContent className="flex-1">
        <SidebarGroup>
          <SidebarGroupLabel>Menu</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {items.map((item) => (
                <SidebarMenuItem key={item.title}>
                  <SidebarMenuButton asChild>
                    <a href={item.url}>
                      <item.icon className="h-6 w-6" />
                      <span>{item.title}</span>
                    </a>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>

        <SidebarGroup>
          <SidebarGroupLabel>Chats</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {chats.map((chat) => (
                <MoodHistoryItem
                  key={chat.id}
                  chat={chat}
                  onRename={handleRename}
                  onDelete={handleDelete}
                />
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>

      <SidebarFooter className="mt-auto pb-3 pt-0 mb-1 group-data-[collapsible=icon]/sidebar:!p-2 group-data-[collapsible=icon]/sidebar:!pt-2 group-data-[collapsible=icon]/sidebar:!pb-2 group-data-[collapsible=icon]/sidebar:!mb-0 group-data-[collapsible=icon]/sidebar:!gap-0">
        <SidebarMenu className="group-data-[collapsible=icon]/sidebar:items-center group-data-[collapsible=icon]/sidebar:!gap-0">
          <SidebarMenuItem>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <SidebarMenuButton
                  className="
                    w-full
                    flex items-center gap-3
                    rounded-lg
                    px-2 py-2
                    !h-auto min-h-[48px]
                    hover:bg-muted/60
                    overflow-visible
                    focus:outline-none focus-visible:outline-none
                  "
                >
                  <Avatar className="h-4.5 w-4.5 rounded-full shrink-0 group-data-[collapsible=icon]/sidebar:-ml-2">
                    <AvatarImage src={user.avatarUrl} alt={user.name} />
                    <AvatarFallback className="text-xs">{user.initials}</AvatarFallback>
                  </Avatar>

                  <div
                    className="
                      flex min-w-0 flex-col text-left leading-tight
                      group-data-[collapsible=icon]/sidebar:hidden
                    "
                  >
                    <span className="text-sm font-medium truncate">{user.name}</span>
                    <span className="text-xs text-muted-foreground truncate mt-1">
                      {user.email}
                    </span>
                  </div>
                </SidebarMenuButton>
              </DropdownMenuTrigger>

              <DropdownMenuContent
                align="start"
                side="right"
                className="w-56 rounded-xl shadow-md"
              >
                <DropdownMenuItem asChild>
                  <a href="/profile" className="flex items-center gap-2">
                    <User className="h-4 w-4 text-muted-foreground" />
                    <span>Profile</span>
                  </a>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <a href="/space" className="flex items-center gap-2">
                    <LayoutDashboard className="h-4 w-4 text-muted-foreground" />
                    <span>Personal Space</span>
                  </a>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <a href="/settings" className="flex items-center gap-2">
                    <SettingsIcon className="h-4 w-4 text-muted-foreground" />
                    <span>Settings</span>
                  </a>
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild>
                  <a
                    href="/logout"
                    className="flex items-center gap-2 text-red-600 hover:text-red-700"
                  >
                    <LogOut className="h-4 w-4" />
                    <span>Logout</span>
                  </a>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
    </Sidebar>
  )
}
