<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { api } from '../api/client'
import { notifyError, notifySuccess } from '../stores/notifications'

const route = useRoute()

const book = ref(null)
const loading = ref(false)
const borrowing = ref(false)
const notFound = ref(false)
const failed = ref(false)

async function load() {
  loading.value = true
  try {
    book.value = await api.getBook(route.params.id)
    notFound.value = false
    failed.value = false
  } catch (error) {
    book.value = null
    // A missing book is a normal outcome; anything else is a failure worth retrying.
    notFound.value = error?.code === 'BOOK_NOT_FOUND'
    failed.value = !notFound.value
    notifyError(error)
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
    <p v-else-if="notFound" class="state">未找到该书籍。</p>
    <div v-else-if="failed" class="state">
      <p>加载失败，请稍后重试。</p>
      <button type="button" class="primary" @click="load">重试</button>
    </div>

    <article v-else-if="book" class="card">
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
