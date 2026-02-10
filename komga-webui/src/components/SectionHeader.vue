<template>
  <div class="d-flex align-center mb-3">
    <v-avatar :color="iconColor" size="32" class="mr-3">
      <v-icon dark small>{{ icon }}</v-icon>
    </v-avatar>
    <div class="flex-grow-1">
      <div class="d-flex align-center">
        <span class="text-subtitle-1 font-weight-bold">{{ title }}</span>
        <v-chip v-if="count" x-small class="ml-2">{{ count }}</v-chip>
      </div>
      <div class="text-caption text--secondary">{{ subtitle }}</div>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
  name: 'SectionHeader',
  props: {
    icon: String,
    iconColor: String,
    title: String,
    subtitle: String,
    count: Number,
  },
})
</script>
