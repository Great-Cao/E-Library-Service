<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { api } from '../api/client'
import { notifyError, notifySuccess } from '../stores/notifications'

const route = useRoute()

const book = ref(null)
const loading = ref(false)
const borrowing = ref(false)

async function load() {
  loading.value = true
  try {
    book.value = await api.getBook(route.params.id)
  } catch (error) {
    notifyError(error)
    book.value = null
  } finally {
    loading.value = false
  }
}

async function borrow() {
  borrowing.value = true
  try {
    await api.borrowBook(book.value.id)
    notifySuccess(`已借阅《${book.value.title}》`)
    await load()
  } catch (error) {
    notifyError(error)
  } finally {
    borrowing.value = false
  }
}

onMounted(load)
</script>

<template>
  <section>
    <p class="breadcrumb"><RouterLink to="/">← 返回书籍列表</RouterLink></p>

    <p v-if="loading" class="state">加载中…</p>
    <p v-else-if="!book" class="state">未找到该书籍。</p>

    <article v-else class="card">
      <h1>{{ book.title }}</h1>
      <p class="muted">{{ book.author }}</p>

      <dl class="details">
        <dt>ISBN</dt>
        <dd class="mono">{{ book.isbn }}</dd>
        <dt>可借 / 总量</dt>
        <dd>
          <span :class="['pill', book.availableCopies === 0 ? 'pill-out' : 'pill-in']">
            {{ book.availableCopies }} / {{ book.totalCopies }}
          </span>
        </dd>
        <dt>入库时间</dt>
        <dd>{{ new Date(book.createdAt).toLocaleString() }}</dd>
        <dt>简介</dt>
        <dd>{{ book.description || '（无）' }}</dd>
      </dl>

      <button
        type="button"
        class="primary"
        :disabled="borrowing || book.availableCopies === 0"
        @click="borrow"
      >
        {{ book.availableCopies === 0 ? '暂无库存' : borrowing ? '借阅中…' : '借阅' }}
      </button>
    </article>
  </section>
</template>
