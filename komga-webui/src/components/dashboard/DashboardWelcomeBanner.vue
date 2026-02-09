<template>
  <v-card class="rounded-lg overflow-hidden welcome-card" flat style="background: transparent !important;">
    <div class="welcome-banner pa-4 d-flex flex-column h-100 relative" style="height: 100%">
      <div class="banner-bg-pattern"></div>
      
      <div class="relative z-1 mb-3">
        <div class="text-h5 font-weight-bold white--text mb-1">{{ greeting }} 👋</div>
        <div class="white--text text-body-2" style="opacity: 0.9;">
          {{ $t('dashboard_welcome.unread_message', {count: unreadCount}) }}
        </div>
      </div>

      <div class="d-flex relative z-1">
        <div class="stat-badge mr-2 text-center flex-grow-1 elevation-2 d-flex flex-column justify-center">
          <div class="text-h5 font-weight-bold white--text mb-1">{{ streak }}</div>
          <div class="white--text text-caption font-weight-medium text-uppercase ls-1 badge-label">
            {{ $t('dashboard_welcome.streak_days') }}
          </div>
        </div>
        <div class="stat-badge ml-2 text-center flex-grow-1 elevation-2 d-flex flex-column justify-center">
          <div class="text-h5 font-weight-bold white--text mb-1">{{ weeklyRead }}</div>
          <div class="white--text text-caption font-weight-medium text-uppercase ls-1 badge-label">
            {{ $t('dashboard_welcome.weekly_read') }}
          </div>
        </div>
      </div>
    </div>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
  name: 'DashboardWelcomeBanner',
  props: {
    unreadCount: {type: Number, default: 0},
    streak: {type: Number, default: 0},
    weeklyRead: {type: Number, default: 0},
  },
  computed: {
    greeting(): string {
      const hour = new Date().getHours()
      if (hour < 6) return this.$t('dashboard_welcome.greeting_night') as string
      if (hour < 12) return this.$t('dashboard_welcome.greeting_morning') as string
      if (hour < 18) return this.$t('dashboard_welcome.greeting_afternoon') as string
      return this.$t('dashboard_welcome.greeting_evening') as string
    },
  },
})
</script>

<style scoped>
.welcome-card {
  height: 100%;
  transition: transform 0.2s ease;
  border: none !important;
  background: transparent !important;
}

.welcome-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(21, 101, 192, 0.25) !important;
}

.welcome-banner {
  background: linear-gradient(135deg, var(--db-banner-from) 0%, var(--db-banner-to) 100%);
  position: relative;
  overflow: hidden;
  border-radius: inherit;
}

.banner-bg-pattern {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 10% 20%, rgba(255, 255, 255, 0.1) 0%, transparent 20%),
    radial-gradient(circle at 90% 80%, rgba(255, 255, 255, 0.1) 0%, transparent 20%);
  z-index: 0;
  transition: transform 0.5s ease;
}

.welcome-card:hover .banner-bg-pattern {
  transform: scale(1.1);
}

.stat-badge {
  background: var(--db-banner-badge-bg);
  border: 1px solid var(--db-banner-badge-border);
  border-radius: 12px;
  padding: 8px 12px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.welcome-card:hover .stat-badge {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.stat-badge:hover {
  filter: brightness(1.2);
  transform: translateY(-2px);
}

.badge-label {
  opacity: 0.8;
  font-size: 0.7rem !important;
}

.ls-1 {
  letter-spacing: 1px;
}
</style>
