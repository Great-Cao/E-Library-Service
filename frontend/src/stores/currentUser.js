import { computed, ref } from 'vue'

// The backend has no authentication; these are the seeded demo users.
export const DEMO_USERS = [
  { id: 1, name: 'Alice Chen' },
  { id: 2, name: 'Brian Lee' },
]

const STORAGE_KEY = 'e-library.currentUserId'

function initialUserId() {
  const stored = Number(window.localStorage.getItem(STORAGE_KEY))
  return DEMO_USERS.some((user) => user.id === stored) ? stored : DEMO_USERS[0].id
}

const currentUserId = ref(initialUserId())

export function getCurrentUserId() {
  return currentUserId.value
}

export function useCurrentUser() {
  return {
    users: DEMO_USERS,
    currentUserId: computed(() => currentUserId.value),
    currentUser: computed(() => DEMO_USERS.find((user) => user.id === currentUserId.value)),
    setCurrentUser(id) {
      const next = Number(id)
      if (!DEMO_USERS.some((user) => user.id === next)) {
        return
      }
      currentUserId.value = next
      window.localStorage.setItem(STORAGE_KEY, String(next))
    },
  }
}
