<script setup>
const props = defineProps({
  page: { type: Number, required: true },
  totalPages: { type: Number, required: true },
  busy: { type: Boolean, default: false },
})

const emit = defineEmits(['change'])

function go(next) {
  if (next >= 0 && next < props.totalPages) {
    emit('change', next)
  }
}
</script>

<template>
  <div v-if="totalPages > 1" class="pager">
    <button type="button" :disabled="busy || page <= 0" @click="go(page - 1)">上一页</button>
    <span class="pager-status">第 {{ page + 1 }} / {{ totalPages }} 页</span>
    <button type="button" :disabled="busy || page >= totalPages - 1" @click="go(page + 1)">下一页</button>
  </div>
</template>
