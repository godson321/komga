<template>
  <v-card class="rounded-lg stat-card-modern h-100 overflow-hidden" flat style="background: transparent !important;">
    <div class="d-flex h-100" style="height: 100%">
      <!-- Series Section -->
      <div class="flex-grow-1 series-section d-flex flex-column justify-center align-center pa-4 relative">
        <div class="section-glow series-glow"></div>
        <v-icon class="section-icon mb-2" size="36">mdi-bookshelf</v-icon>
        <div class="text-h4 font-weight-bold white--text mb-1">{{ formatNumber(seriesCount) }}</div>
        <div class="white--text text-caption font-weight-medium text-uppercase ls-1" style="opacity: 0.85; font-size: 0.65rem !important;">{{ $t('dashboard_stats.total_series') }}</div>
      </div>

      <!-- Books Section -->
      <div class="flex-grow-1 books-section d-flex flex-column justify-center align-center pa-4 relative">
        <div class="section-glow books-glow"></div>
        <v-icon class="section-icon mb-2" size="36">mdi-book-open-variant</v-icon>
        <div class="text-h4 font-weight-bold white--text mb-1">{{ formatNumber(bookCount) }}</div>
        <div class="white--text text-caption font-weight-medium text-uppercase ls-1" style="opacity: 0.85; font-size: 0.65rem !important;">{{ $t('dashboard_stats.total_books') }}</div>
      </div>
    </div>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
  name: 'DashboardStatCard',
  props: {
    seriesCount: {type: Number, default: 0},
    bookCount: {type: Number, default: 0},
  },
  methods: {
    formatNumber(n: number): string {
      if (n >= 10000) return (n / 1000).toFixed(1) + 'k'
      if (n >= 1000) return n.toLocaleString()
      return String(n)
    },
  },
})
</script>

<style scoped>
.stat-card-modern {
  transition: all 0.2s ease;
  border: none !important;
  background: transparent !important;
}

.stat-card-modern:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15) !important;
}

.series-section {
  background: var(--db-stat-series-bg);
  z-index: 1;
  border-radius: 8px 0 0 8px;
}

.books-section {
  background: var(--db-stat-books-bg);
  z-index: 1;
  border-radius: 0 8px 8px 0;
}

.section-icon {
  color: rgba(255, 255, 255, 0.9);
}

.section-glow {
  position: absolute;
  width: 160px;
  height: 160px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  filter: blur(50px);
  opacity: 1;
  z-index: 0;
  pointer-events: none;
  transition: all 0.5s ease;
}

.stat-card-modern:hover .section-glow {
  opacity: 1;
  filter: blur(40px);
  transform: scale(1.2);
}

.series-glow {
  top: -60px;
  right: -60px;
}

.books-glow {
  bottom: -60px;
  left: -60px;
}

.relative {
  position: relative;
}

.stat-card-modern:hover .section-icon {
  transform: scale(1.1);
  transition: transform 0.2s ease;
}

.ls-1 {
  letter-spacing: 1px;
}
</style>
