"use client"

import * as React from "react"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { getInitials } from "./getInitials"
import { uploadAvatar, updateProfile } from "@/api/user"
import { getUser } from "@/api/auth"
import { emitUserUpdated } from "@/lib/user-events"

type Props = {
  open: boolean
  onOpenChange: (v: boolean) => void
  user: { username?: string; email?: string; avatarUrl?: string | null }
  onUserUpdated?: (u: { username: string; avatarUrl?: string | null }) => void
}

function validateUsername(raw: string): string | null {
  const v = (raw ?? "").trim()
  if (v.length < 2) return "Username must be at least 2 characters."
  if (v.length > 50) return "Username must be at most 50 characters."
  return null
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null
}

function getErrorMessage(e: unknown): string {
  if (e instanceof Error && typeof e.message === "string" && e.message.trim()) return e.message
  if (isRecord(e) && typeof e.message === "string" && e.message.trim()) return e.message
  return "Failed to update profile."
}

type AuthUser = {
  username?: string
  avatarUrl?: string | null
  [k: string]: unknown
}

function parseAuthUser(raw: string | null): AuthUser | null {
  if (!raw) return null
  try {
    const v: unknown = JSON.parse(raw)
    if (isRecord(v)) return v as AuthUser
    return null
  } catch {
    return null
  }
}

export function EditProfileDialog({ open, onOpenChange, user, onUserUpdated }: Props) {
  const safeUsername = (user?.username ?? "").toString()
  const safeEmail = (user?.email ?? "").toString()

  const [username, setUsername] = React.useState<string>(safeUsername)
  const [avatarPreview, setAvatarPreview] = React.useState<string | undefined>(
    user?.avatarUrl ?? undefined,
  )
  const [avatarFile, setAvatarFile] = React.useState<File | null>(null)
  const [saving, setSaving] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)

  const objectUrlRef = React.useRef<string | null>(null)

  React.useEffect(() => {
    if (open) {
      setUsername((user?.username ?? "").toString())
      setAvatarPreview(user?.avatarUrl ?? undefined)
      setAvatarFile(null)
      setError(null)
    }

    return () => {
      if (objectUrlRef.current) {
        URL.revokeObjectURL(objectUrlRef.current)
        objectUrlRef.current = null
      }
    }
  }, [open, user?.username, user?.avatarUrl])

  const onPickAvatar: React.ChangeEventHandler<HTMLInputElement> = (e) => {
    const f = e.target.files?.[0] ?? null
    setAvatarFile(f)

    if (objectUrlRef.current) {
      URL.revokeObjectURL(objectUrlRef.current)
      objectUrlRef.current = null
    }

    if (f) {
      const url = URL.createObjectURL(f)
      objectUrlRef.current = url
      setAvatarPreview(url)
    } else {
      setAvatarPreview(user?.avatarUrl ?? undefined)
    }

    // allow re-pick same file
    e.currentTarget.value = ""
  }

  const displayAvatar = React.useMemo(() => {
    const src = (avatarPreview ?? "").trim()
    if (!src) return undefined
    if (src.startsWith("blob:")) return src
    const sep = src.includes("?") ? "&" : "?"
    return `${src}${sep}v=${Date.now()}`
  }, [avatarPreview])

  const onSave = async () => {
    const validationError = validateUsername(username)
    if (validationError) {
      setError(validationError)
      return
    }

    setSaving(true)
    setError(null)

    const nextUsername = username.trim()

    try {
      let avatarUrl: string | null | undefined = user?.avatarUrl ?? null

      if (avatarFile) {
        const up = await uploadAvatar(avatarFile)
        avatarUrl = up.avatarUrl
      }

      const updated = await updateProfile({
        username: nextUsername,
        avatarUrl: avatarUrl ?? null,
      })

      const finalUsername = (updated.username ?? nextUsername).trim()
      const finalAvatarUrl = updated.avatarUrl ?? avatarUrl ?? null

      onUserUpdated?.({ username: finalUsername, avatarUrl: finalAvatarUrl })

      const currentRaw = localStorage.getItem("auth_user")
      const current = parseAuthUser(currentRaw) ?? getUser()

      if (current) {
        const next: AuthUser = {
          ...current,
          username: finalUsername,
          avatarUrl: finalAvatarUrl,
        }
        localStorage.setItem("auth_user", JSON.stringify(next))
        emitUserUpdated()
      }

      onOpenChange(false)
    } catch (e: unknown) {
      setError(getErrorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  const validationError = validateUsername(username)

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent aria-describedby="edit-profile-desc" className="sm:max-w-[420px]">
        <DialogHeader>
          <DialogTitle className="text-base font-semibold">Edit profile</DialogTitle>
          <DialogDescription id="edit-profile-desc">
            Update your in-app username and avatar.
          </DialogDescription>
        </DialogHeader>

        <div className="mt-2 flex flex-col items-center gap-3">
          <div className="mt-2 flex flex-col items-center gap-3">
            <div className="relative">
              <label className="cursor-pointer">
                <Avatar className="h-20 w-20">
                  <AvatarImage key={displayAvatar} src={displayAvatar} alt={safeUsername || "User"} />
                  <AvatarFallback>{getInitials(safeUsername || "User")}</AvatarFallback>
                </Avatar>

                <input className="hidden" type="file" accept="image/*" onChange={onPickAvatar} />
              </label>

              <label
                className="
                  absolute
                  -bottom-1
                  left-1/2
                  -translate-x-1/2
                  cursor-pointer
                  rounded-full
                  bg-black/70
                  px-3
                  py-1
                  text-xs
                  text-white
                  hover:bg-black
                "
              >
                Upload
                <input className="hidden" type="file" accept="image/*" onChange={onPickAvatar} />
              </label>
            </div>
          </div>

          <div className="w-full space-y-2">
            <div className="space-y-1">
              <Label htmlFor="username">Display name</Label>
              <Input
                id="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Your name"
                aria-invalid={!!validationError}
              />
              {validationError && <p className="text-xs text-red-600">{validationError}</p>}
            </div>

            <div className="space-y-1">
              <Label>Email</Label>
              <Input value={safeEmail} disabled />
            </div>

            {error && <p className="text-xs text-red-600 whitespace-pre-wrap">{error}</p>}
          </div>

          <div className="mt-2 flex w-full justify-end gap-2">
            <Button variant="ghost" onClick={() => onOpenChange(false)} disabled={saving}>
              Cancel
            </Button>
            <Button
              onClick={onSave}
              disabled={saving || !!validationError}
              className="
                !bg-blue-100
                !text-blue-900
                hover:bg-blue-200
                !disabled:bg-blue-50
                !disabled:text-blue-300
              "
            >
              Save
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}
