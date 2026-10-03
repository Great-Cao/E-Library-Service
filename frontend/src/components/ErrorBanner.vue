<script setup>
import { useNotifications } from '../stores/notifications'

const { visible, kind, message, detail, clear } = useNotifications()
</script>

<template>
  <div v-if="visible" class="banner" :class="`banner-${kind}`" role="status">
    <span class="banner-message">{{ message }}</span>
    <span v-if="detail" class="banner-code">{{ detail }}</span>
    <button type="button" class="banner-close" @click="clear">关闭</button>
  </div>
</template>
