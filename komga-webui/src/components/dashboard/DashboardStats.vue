<template>
  <div class="stats-grid">
    <!-- Series & Books Combined -->
    <v-card class="stat-card rounded-lg" flat outlined>
      <div class="pa-4 d-flex align-center">
        <div class="stat-icon-wrap mr-3" style="background: rgba(94, 53, 177, 0.1);">
          <v-icon color="#5E35B1" size="22">mdi-library-shelves</v-icon>
        </div>
        <div class="flex-grow-1">
          <div class="d-flex align-center mb-1">
            <v-icon size="14" color="#388E3C" class="mr-1">mdi-book-multiple</v-icon>
            <span class="stat-label text--secondary mr-2">{{ $t('dashboard_stats.total_series') }}</span>
            <span class="font-weight-bold text-body-2">{{ formatNumber(seriesCount) }}</span>
          </div>
          <div class="d-flex align-center">
            <v-icon size="14" color="#F57C00" class="mr-1">mdi-book-open-variant</v-icon>
            <span class="stat-label text--secondary mr-2">{{ $t('dashboard_stats.total_books') }}</span>
            <span class="font-weight-bold text-body-2">{{ formatNumber(bookCount) }}</span>
          </div>
        </div>
      </div>
    </v-card>

    <!-- Read Progress -->
    <v-card class="stat-card rounded-lg" flat outlined>
      <div class="pa-4 d-flex align-center">
        <svg viewBox="0 0 36 36" class="donut-chart mr-3">
          <circle cx="18" cy="18" r="14" fill="none" stroke="rgba(128,128,128,0.12)" stroke-width="3.5"/>
          <circle cx="18" cy="18" r="14" fill="none" stroke="#4CAF50" stroke-width="3.5"
                  stroke-linecap="round"
                  :stroke-dasharray="readDash"
                  stroke-dashoffset="22"
                  style="transition: stroke-dasharray 0.6s ease;"
          />
          <text x="18" y="19" text-anchor="middle" dominant-baseline="middle"
                class="donut-text" fill="currentColor">{{ readPercent }}%</text>
        </svg>
        <div>
          <div class="d-flex align-center mb-1">
            <span class="stat-dot mr-2" style="background: #4CAF50;"/>
            <span class="stat-label text--secondary">{{ $t('dashboard_stats.books_read') }}</span>
            <span class="font-weight-bold ml-2 text-body-2">{{ formatNumber(readCount) }}</span>
          </div>
          <div class="d-flex align-center">
            <span class="stat-dot mr-2" style="background: #78909C;"/>
            <span class="stat-label text--secondary">{{ $t('dashboard_stats.unread') }}</span>
            <span class="font-weight-bold ml-2 text-body-2">{{ formatNumber(bookCount - readCount) }}</span>
          </div>
        </div>
      </div>
    </v-card>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
  name: 'DashboardStats',
  props: {
    seriesCount: {type: Number, default: 0},
    bookCount: {type: Number, default: 0},
    readCount: {type: Number, default: 0},
  },
  computed: {
    readPercent(): number {
      return this.bookCount > 0 ? Math.round(this.readCount / this.bookCount * 100) : 0
    },
    readDash(): string {
      const circ = 2 * Math.PI * 14
      const filled = (this.readPercent / 100) * circ
      return `${filled} ${circ - filled}`
    },
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
.stats-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
}

.stat-card {
  transition: transform 0.15s ease, box-shadow 0.15s ease;
  flex: 1;
  min-height: 0;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08) !important;
}

.stat-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-label {
  font-size: 12px;
  line-height: 1.3;
}

.stat-value {
  font-size: 24px;
  line-height: 1.2;
}

.donut-chart {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
}

.donut-text {
  font-size: 9px;
  font-weight: 600;
}

.stat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}
</style>
