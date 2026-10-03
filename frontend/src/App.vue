<script setup>
import { RouterLink, RouterView, useRoute } from 'vue-router'

import ErrorBanner from './components/ErrorBanner.vue'
import UserSwitcher from './components/UserSwitcher.vue'
import { useCurrentUser } from './stores/currentUser'

const route = useRoute()
const { currentUserId } = useCurrentUser()
</script>

<template>
  <header class="topbar">
    <div class="brand">
      E-Library Service
      <span class="brand-sub">演示前端</span>
    </div>
    <nav class="nav">
      <RouterLink to="/">书籍</RouterLink>
      <RouterLink to="/me/loans">我的借阅</RouterLink>
    </nav>
    <UserSwitcher />
  </header>

  <ErrorBanner />

  <main class="page">
    <!-- Re-keying on the user id reloads the current view when identity changes. -->
    <RouterView :key="`${currentUserId}-${route.fullPath}`" />
  </main>
</template>
