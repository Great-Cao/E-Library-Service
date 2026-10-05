<script setup>
import { onMounted, ref } from 'vue'

import { api } from '../api/client'
import Pager from '../components/Pager.vue'
import { notifyError } from '../stores/notifications'

const PAGE_SIZE = 5

const page = ref(0)
const items = ref([])
const totalElements = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const failed = ref(false)
// Guards against out-of-order responses: only the newest request may write state.
let latestRequest = 0

async function load() {
  const requestId = ++latestRequest
  loading.value = true
  try {
    const data = await api.listBooks(page.value, PAGE_SIZE)
    if (requestId !== latestRequest) {
      return
    }
    items.value = data.items
    totalElements.value = data.totalElements
    totalPages.value = data.totalPages
    failed.value = false
  } catch (error) {
    if (requestId === latestRequest) {
      failed.value = true
      notifyError(error)
    }
  } finally {
    if (requestId === latestRequest) {
      loading.value = false
    }
  }
}

function changePage(next) {
  page.value = next
  load()
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-head">
      <h1>书籍</h1>
      <span class="muted">共 {{ totalElements }} 本</span>
    </div>

    <p v-if="loading" class="state">加载中…</p>
    <div v-else-if="failed" class="state">
      <p>加载失败，请稍后重试。</p>
      <button type="button" class="primary" @click="load">重试</button>
    </div>
    <p v-else-if="items.length === 0" class="state">暂无书籍。</p>

    <table v-else class="data-table">
      <thead>
        <tr>
          <th>书名</th>
          <th>作者</th>
          <th>ISBN</th>
          <th class="right">可借 / 总量</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="book in items" :key="book.id">
          <td>
            <RouterLink :to="`/books/${book.id}`">{{ book.title }}</RouterLink>
          </td>
          <td>{{ book.author }}</td>
          <td class="mono">{{ book.isbn }}</td>
          <td class="right">
            <span :class="['pill', book.availableCopies === 0 ? 'pill-out' : 'pill-in']">
              {{ book.availableCopies }} / {{ book.totalCopies }}
            </span>
          </td>
        </tr>
      </tbody>
    </table>

    <Pager :page="page" :total-pages="totalPages" :busy="loading" @change="changePage" />
  </section>
</template>
