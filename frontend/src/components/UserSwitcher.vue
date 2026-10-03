<script setup>
import { useCurrentUser } from '../stores/currentUser'

const { users, currentUserId, setCurrentUser } = useCurrentUser()

function onChange(event) {
  setCurrentUser(event.target.value)
}
</script>

<template>
  <label class="user-switcher">
    <span class="user-switcher-label">当前用户</span>
    <select :value="currentUserId" @change="onChange">
      <option v-for="user in users" :key="user.id" :value="user.id">
        {{ user.name }}（id={{ user.id }}）
      </option>
    </select>
  </label>
</template>
