import { ref } from 'vue'

const visible = ref(false)
const kind = ref('error')
const message = ref('')
const detail = ref('')
let timer = null

function show(nextKind, text, code, timeoutMs) {
  kind.value = nextKind
  message.value = text
  detail.value = code ?? ''
  visible.value = true
  if (timer) {
    clearTimeout(timer)
  }
  timer = setTimeout(clear, timeoutMs)
}

/** Surfaces a backend or network failure in the shared banner. */
export function notifyError(error) {
  show('error', error?.message ?? '请求失败', error?.code, 8000)
}

export function notifySuccess(text) {
  show('success', text, '', 3000)
}

export function clear() {
  visible.value = false
  message.value = ''
  detail.value = ''
  if (timer) {
    clearTimeout(timer)
    timer = null
  }
}

export function useNotifications() {
  return { visible, kind, message, detail, clear }
}
