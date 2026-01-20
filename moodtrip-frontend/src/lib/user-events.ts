export const USER_UPDATED_EVENT = "moodtrip-user-updated"

export function emitUserUpdated() {
  window.dispatchEvent(new CustomEvent(USER_UPDATED_EVENT))
}

/**
 * Subscribe to user updates (e.g. after profile update / avatar upload).
 * Returns an unsubscribe function.
 */
export function onUserUpdated(handler: () => void) {
  const listener = () => handler()
  window.addEventListener(USER_UPDATED_EVENT, listener)
  return () => window.removeEventListener(USER_UPDATED_EVENT, listener)
}
