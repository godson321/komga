<template>
  <v-card class="rounded-lg progress-card h-100 overflow-hidden" flat style="background: transparent !important;">
    <div class="progress-bg d-flex h-100 pa-4 relative" style="height: 100%">
      <div class="progress-glow"></div>
      
      <!-- Donut chart -->
      <div class="d-flex align-center mr-4 flex-shrink-0 z-1">
        <div class="donut-wrapper">
          <svg viewBox="0 0 36 36" class="donut-chart-xl">
            <circle cx="18" cy="18" r="14" fill="none" stroke="rgba(255,255,255,0.1)" stroke-width="2.5"/>
            <circle cx="18" cy="18" r="14" fill="none" stroke="var(--db-progress-ring)" stroke-width="2.5"
                    stroke-linecap="round"
                    :stroke-dasharray="readDash"
                    stroke-dashoffset="22"
                    class="donut-progress"
            />
            <text x="18" y="17" text-anchor="middle" dominant-baseline="middle"
                  class="donut-text-xl" fill="white">{{ readPercent }}%</text>
            <text x="18" y="23" text-anchor="middle" dominant-baseline="middle"
                  class="donut-label" fill="rgba(255,255,255,0.6)">已读</text>
          </svg>
        </div>
      </div>
      
      <!-- Right side data -->
      <div class="flex-grow-1 d-flex flex-column z-1">
        <!-- Read -->
        <div class="mb-auto">
          <div class="white--text text-caption font-weight-medium text-uppercase ls-1 mb-1" style="opacity: 0.7; font-size: 0.6rem !important;">{{ $t('dashboard_stats.books_read') }}</div>
          <div class="text-h5 font-weight-bold white--text lh-1 mb-2">{{ formatNumber(readCount) }}</div>
          <div class="progress-bar-track">
            <div class="progress-bar-fill read-fill" :style="{width: readPercent + '%'}"></div>
          </div>
        </div>
        
        <!-- Unread -->
        <div>
          <div class="white--text text-caption font-weight-medium text-uppercase ls-1 mb-1" style="opacity: 0.7; font-size: 0.6rem !important;">{{ $t('dashboard_stats.unread') }}</div>
          <div class="text-h5 font-weight-bold white--text lh-1 mb-2">{{ formatNumber(bookCount - readCount) }}</div>
          <div class="progress-bar-track">
            <div class="progress-bar-fill unread-fill" :style="{width: (100 - readPercent) + '%'}"></div>
          </div>
        </div>
      </div>
    </div>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
  name: 'DashboardReadProgress',
  props: {
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
.progress-card {
  transition: all 0.2s ease;
  border: none !important;
  background: transparent !important;
}

.progress-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2) !important;
}

.progress-bg {
  background: var(--db-progress-bg);
  position: relative;
  overflow: hidden;
  border-radius: inherit;
}

.progress-glow {
  position: absolute;
  top: 50%;
  left: 25%;
  transform: translate(-50%, -50%);
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, var(--db-progress-glow) 0%, transparent 60%);
  opacity: 0.8;
  z-index: 0;
  pointer-events: none;
  transition: all 0.5s ease;
}

.progress-card:hover .progress-glow {
  opacity: 1;
  transform: translate(-50%, -50%) scale(1.1);
}
.donut-wrapper {
  position: relative;
}

.donut-chart-xl {
  width: 120px;
  height: 120px;
  flex-shrink: 0;
}

.donut-progress {
  transition: stroke-dasharray 0.6s ease;
}

.donut-text-xl {
  font-size: 7px;
  font-weight: 700;
}

.donut-label {
  font-size: 3.5px;
  font-weight: 500;
}

.progress-bar-track {
  width: 100%;
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}

.read-fill {
  background: var(--db-progress-ring);
}

.unread-fill {
  background: rgba(255, 255, 255, 0.3);
}

.relative {
  position: relative;
}

.z-1 {
  z-index: 1;
}

.lh-1 {
  line-height: 1;
}

.ls-1 {
  letter-spacing: 1px;
}
</style>
