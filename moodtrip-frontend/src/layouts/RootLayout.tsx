import { Outlet } from "react-router-dom"
import { SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar"
import { AppSidebar } from "@/components/app-sidebar"
import "@/index.css"

export default function RootLayout() {
  return (
    <SidebarProvider>
      <AppSidebar />
      <main className="min-h-dvh w-full">
        <div className="sticky top-0 z-10 border-b bg-background/70 backdrop-blur supports-[backdrop-filter]:bg-background/60">
          <div className="flex h-12 items-center gap-2 px-4">
            <SidebarTrigger />
            <span className="font-medium">我的应用</span>
          </div>
        </div>
        <div className="p-4">
          <Outlet />
        </div>
      </main>
    </SidebarProvider>
  )
}
