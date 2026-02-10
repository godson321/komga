<template>
  <v-card class="rounded-lg activity-card h-100 overflow-hidden" flat style="background: transparent !important;">
    <div class="activity-bg d-flex flex-column h-100 pa-4 relative" style="height: 100%">
      <div class="activity-glow"></div>
      
      <!-- Header -->
      <div class="d-flex align-center justify-space-between mb-2 z-1">
        <div class="d-flex align-center">
          <v-icon class="mr-2" size="20" style="color: rgba(255,255,255,0.7)">mdi-chart-bar</v-icon>
          <div class="white--text text-caption font-weight-medium text-uppercase ls-1" style="opacity: 0.7; font-size: 0.6rem !important;">{{ $t('dashboard_activity.title') }}</div>
        </div>
        <div class="streak-pill d-flex align-center px-2 py-1 rounded-pill">
          <v-icon x-small class="mr-1" style="color: var(--db-activity-accent)">mdi-fire</v-icon>
          <span class="text-caption font-weight-bold" style="color: var(--db-activity-accent)">{{ streak }}</span>
        </div>
      </div>

      <!-- Total number -->
      <div class="text-h4 font-weight-bold white--text mb-1 z-1">{{ weekTotal }}</div>
      <div class="white--text text-caption mb-3 z-1" style="opacity: 0.5; font-size: 0.6rem !important;">{{ $t('dashboard_welcome.weekly_read') }}</div>

      <!-- Bar Chart -->
      <div class="chart-area d-flex align-end z-1" style="gap: 6px">
        <div v-for="(day, i) in weeklyData" :key="i" class="d-flex flex-column align-center flex-grow-1">
          <v-tooltip top content-class="custom-tooltip">
            <template v-slot:activator="{ on, attrs }">
              <div 
                class="mini-bar"
                v-bind="attrs"
                v-on="on"
                :style="{
                  height: barHeight(day.count) + 'px',
                  background: day.isToday ? 'var(--db-activity-accent)' : 'rgba(255,255,255,0.15)',
                  opacity: day.count === 0 ? 0.3 : 1,
                  boxShadow: day.isToday && day.count > 0 ? '0 0 8px var(--db-activity-accent)' : 'none',
                }"
              ></div>
            </template>
            <span>{{ day.count }}</span>
          </v-tooltip>
          <div class="white--text mt-1 font-weight-medium" 
               :style="{opacity: day.isToday ? 1 : 0.5, fontSize: '0.6rem'}">{{ day.label }}</div>
        </div>
      </div>
    </div>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'

interface DayActivity {
  label: string
  count: number
  isToday: boolean
}

export default Vue.extend({
  name: 'DashboardReadingActivity',
  props: {
    mockMode: {type: Boolean, default: true},
    days: {type: Number, default: 7},
  },
  data() {
    return {
      streak: 0,
    }
  },
  computed: {
    weeklyData(): DayActivity[] {
      // TODO: connect to real reading activity API
      const dayLabels = ['一', '二', '三', '四', '五', '六', '日']
      const today = new Date().getDay()
      const todayIndex = today === 0 ? 6 : today - 1

      return dayLabels.map((label, i) => ({
        label,
        count: 0,
        isToday: i === todayIndex,
      }))
    },
    weekTotal(): number {
      return this.weeklyData.reduce((sum, d) => sum + d.count, 0)
    },
    maxCount(): number {
      return Math.max(...this.weeklyData.map(d => d.count), 1)
    },
  },
  methods: {
    barHeight(count: number): number {
      const maxH = 45 // max bar height in px, reduced to avoid text overlap
      if (count === 0) return 4
      return Math.max(6, Math.round((count / this.maxCount) * maxH))
    },
  },
})
</script>

<style scoped>
.activity-card {
  transition: all 0.2s ease;
  border: none !important;
  background: transparent !important;
}

.activity-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2) !important;
}

.activity-bg {
  background: var(--db-activity-bg);
  position: relative;
  overflow: hidden;
  border-radius: inherit;
}

.activity-glow {
  position: absolute;
  right: -80px;
  top: -80px;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, var(--db-activity-glow) 0%, transparent 50%);
  filter: blur(20px);
  opacity: 0.6;
  z-index: 0;
  pointer-events: none;
  transition: all 0.5s ease;
}

.activity-card:hover .activity-glow {
  opacity: 0.8;
  transform: scale(1.1);
}
.streak-pill {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.chart-area {
  flex: 1;
  min-height: 0;
}

.mini-bar {
  width: 100%;
  min-width: 0;
  border-radius: 4px 4px 0 0;
  transition: height 0.4s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.3s ease;
}

.mini-bar:hover {
  opacity: 1 !important;
  filter: brightness(1.3);
}

.z-1 {
  z-index: 1;
}

.ls-1 {
  letter-spacing: 1px;
}

.relative {
  position: relative;
}
</style>
