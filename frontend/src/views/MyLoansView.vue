<script setup>
import { onMounted, ref } from 'vue'

import { api } from '../api/client'
import Pager from '../components/Pager.vue'
import { useCurrentUser } from '../stores/currentUser'
import { notifyError, notifySuccess } from '../stores/notifications'

const PAGE_SIZE = 5

const { currentUser } = useCurrentUser()
const page = ref(0)
const items = ref([])
const totalPages = ref(0)
const loading = ref(false)
const returningId = ref(null)
const failed = ref(false)
// Guards against out-of-order responses: only the newest request may write state.
let latestRequest = 0

async function load() {
  const requestId = ++latestRequest
  loading.value = true
  try {
    let data = await api.currentLoans(page.value, PAGE_SIZE)
    if (requestId !== latestRequest) {
      return
    }
    // Returning the only row of the last page would leave that page empty, so step
    // back to the last page that still has content.
    if (page.value > 0 && data.totalPages > 0 && page.value >= data.totalPages) {
      page.value = data.totalPages - 1
      data = await api.currentLoans(page.value, PAGE_SIZE)
      if (requestId !== latestRequest) {
        return
      }
    }
    items.value = data.items
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

async function giveBack(loan) {
  returningId.value = loan.id
  try {
    await api.returnLoan(loan.id)
    notifySuccess(`已归还《${loan.bookTitle}》`)
    await load()
  } catch (error) {
    notifyError(error)
    // The row may be stale (another tab already returned it), so refresh the list.
    await load()
  } finally {
    returningId.value = null
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
      <h1>我的借阅</h1>
      <span class="muted">{{ currentUser?.name }}（id={{ currentUser?.id }}）</span>
    </div>

    <p v-if="loading" class="state">加载中…</p>
    <div v-else-if="failed" class="state">
      <p>加载失败，请稍后重试。</p>
      <button type="button" class="primary" @click="load">重试</button>
    </div>
    <p v-else-if="items.length === 0" class="state">当前没有未归还的书籍。</p>

    <table v-else class="data-table">
      <thead>
        <tr>
          <th>书名</th>
          <th>作者</th>
          <th>借阅时间</th>
          <th class="right">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="loan in items" :key="loan.id">
          <td>
            <RouterLink :to="`/books/${loan.bookId}`">{{ loan.bookTitle }}</RouterLink>
          </td>
          <td>{{ loan.bookAuthor }}</td>
          <td>{{ new Date(loan.borrowedAt).toLocaleString() }}</td>
          <td class="right">
            <button
              type="button"
              class="primary"
              :disabled="returningId === loan.id"
              @click="giveBack(loan)"
            >
              {{ returningId === loan.id ? '归还中…' : '归还' }}
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <Pager :page="page" :total-pages="totalPages" :busy="loading" @change="changePage" />
  </section>
</template>
