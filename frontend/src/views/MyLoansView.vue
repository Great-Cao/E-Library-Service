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

async function load() {
  loading.value = true
  try {
    const data = await api.currentLoans(page.value, PAGE_SIZE)
    items.value = data.items
    totalPages.value = data.totalPages
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
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

    <Pager :page="page" :total-pages="totalPages" @change="changePage" />
  </section>
</template>
