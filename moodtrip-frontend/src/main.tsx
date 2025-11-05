import React from "react"
import ReactDOM from "react-dom/client"
import "./index.css"

import { SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar"
import { AppSidebar } from "@/components/app-sidebar"

import App from "@/components/chat" 

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <SidebarProvider>
      <App /> 
    </SidebarProvider>
  </React.StrictMode>
)
