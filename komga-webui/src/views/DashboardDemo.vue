<template>
  <v-container fluid class="pa-6">
    <div class="text-h4 font-weight-bold mb-2">Dashboard 组件展示</div>
    <div class="text-caption text--secondary mb-6">点击标题旁的序号标记你喜欢的方案，可以混搭</div>

    <!-- ============================================ -->
    <!-- SECTION A: 欢迎横幅 -->
    <!-- ============================================ -->
    <div class="text-h6 font-weight-bold mb-3">A. 欢迎横幅</div>

    <!-- A1: 渐变欢迎条 -->
    <div class="text-subtitle-2 mb-2 text--secondary">A1 - 渐变欢迎条</div>
    <v-card class="mb-6 rounded-lg overflow-hidden" flat>
      <div class="welcome-banner-a1 pa-5 d-flex align-center justify-space-between">
        <div>
          <div class="text-h5 font-weight-bold white--text">下午好 👋</div>
          <div class="white--text" style="opacity: 0.85;">你有 42 本书未读，继续加油！</div>
        </div>
        <div class="d-flex align-center">
          <div class="text-center mx-4">
            <div class="text-h4 font-weight-bold white--text">5</div>
            <div class="white--text text-caption" style="opacity: 0.7;">连续天数</div>
          </div>
          <div class="text-center mx-4">
            <div class="text-h4 font-weight-bold white--text">33</div>
            <div class="white--text text-caption" style="opacity: 0.7;">本周阅读</div>
          </div>
        </div>
      </div>
    </v-card>

    <!-- A2: 极简欢迎行 -->
    <div class="text-subtitle-2 mb-2 text--secondary">A2 - 极简欢迎行</div>
    <div class="d-flex align-center justify-space-between mb-6 pa-3">
      <div>
        <span class="text-h5 font-weight-bold">我的书库</span>
        <v-chip small color="primary" class="ml-2">3 个库</v-chip>
        <v-chip small outlined class="ml-1">240 系列</v-chip>
        <v-chip small outlined class="ml-1">4,580 本</v-chip>
      </div>
      <div class="text-caption text--secondary">
        <v-icon small class="mr-1">mdi-clock-outline</v-icon>
        最近阅读: 2 小时前
      </div>
    </div>

    <v-divider class="mb-8"/>

    <!-- ============================================ -->
    <!-- SECTION B: 统计卡片 -->
    <!-- ============================================ -->
    <div class="text-h6 font-weight-bold mb-3">B. 统计卡片</div>

    <!-- B1: 纯数字 + 图标左侧竖线 -->
    <div class="text-subtitle-2 mb-2 text--secondary">B1 - 左侧色条 + 大数字</div>
    <div class="d-flex mb-6" style="gap: 12px;">
      <v-card v-for="s in statsB1" :key="s.label" class="flex-grow-1 rounded-lg overflow-hidden" flat outlined style="min-width: 140px;">
        <div class="d-flex" style="height: 100%;">
          <div :style="{width: '4px', background: s.color}"/>
          <div class="pa-3 flex-grow-1">
            <div class="text-caption text--secondary">{{ s.label }}</div>
            <div class="text-h5 font-weight-bold my-1">{{ s.value }}</div>
            <div class="text-caption" :style="{color: s.trendColor}">
              <v-icon x-small :color="s.trendColor">{{ s.trendIcon }}</v-icon>
              {{ s.trend }}
            </div>
          </div>
        </div>
      </v-card>
    </div>

    <!-- B2: 图标顶部 + 底部进度条 -->
    <div class="text-subtitle-2 mb-2 text--secondary">B2 - 顶部图标 + 底部进度条</div>
    <div class="d-flex mb-6" style="gap: 12px;">
      <v-card v-for="s in statsB2" :key="s.label" class="flex-grow-1 rounded-lg text-center" flat outlined style="min-width: 140px;">
        <div class="pa-3">
          <v-avatar :color="s.color" size="40" class="mb-2">
            <v-icon dark small>{{ s.icon }}</v-icon>
          </v-avatar>
          <div class="text-h5 font-weight-bold">{{ s.value }}</div>
          <div class="text-caption text--secondary mb-2">{{ s.label }}</div>
        </div>
        <v-progress-linear :value="s.progress" :color="s.color" height="3"/>
      </v-card>
    </div>

    <!-- B3: 背景渐变卡 -->
    <div class="text-subtitle-2 mb-2 text--secondary">B3 - 背景渐变色卡</div>
    <div class="d-flex mb-6" style="gap: 12px;">
      <v-card v-for="s in statsB3" :key="s.label" class="flex-grow-1 rounded-lg" flat :style="{background: s.bg, minWidth: '140px'}">
        <div class="pa-4">
          <div class="d-flex align-center justify-space-between mb-2">
            <v-icon color="white" small>{{ s.icon }}</v-icon>
            <span class="white--text text-caption" style="opacity: 0.8;">{{ s.sub }}</span>
          </div>
          <div class="text-h4 font-weight-bold white--text">{{ s.value }}</div>
          <div class="white--text text-caption" style="opacity: 0.8;">{{ s.label }}</div>
        </div>
      </v-card>
    </div>

    <!-- B4: 迷你 sparkline 卡 (当前方案改进版) -->
    <div class="text-subtitle-2 mb-2 text--secondary">B4 - Sparkline 背景卡（改进版：更淡的背景）</div>
    <div class="d-flex mb-6" style="gap: 12px;">
      <v-card v-for="(s, i) in statsB4" :key="s.label" class="flex-grow-1 rounded-lg overflow-hidden" flat outlined style="min-width: 140px; position: relative;">
        <svg class="b4-spark" viewBox="0 0 100 32" preserveAspectRatio="none">
          <defs>
            <linearGradient :id="'b4g'+i" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" :stop-color="s.color" stop-opacity="0.08"/>
              <stop offset="100%" :stop-color="s.color" stop-opacity="0"/>
            </linearGradient>
          </defs>
          <path :d="s.area" :fill="`url(#b4g${i})`"/>
          <polyline :points="s.line" fill="none" :stroke="s.color" stroke-width="1.2" stroke-linejoin="round"/>
        </svg>
        <div class="pa-3" style="position: relative; z-index: 1;">
          <div class="text-caption text--secondary">{{ s.label }}</div>
          <div class="d-flex align-center justify-space-between mt-1">
            <span class="text-h5 font-weight-bold">{{ s.value }}</span>
            <v-chip x-small :color="s.up ? 'success' : 'grey lighten-1'" :dark="s.up" style="height: 18px;">
              {{ s.trend }}
            </v-chip>
          </div>
        </div>
      </v-card>
    </div>

    <v-divider class="mb-8"/>

    <!-- ============================================ -->
    <!-- SECTION C: 阅读进度 / 活动 -->
    <!-- ============================================ -->
    <div class="text-h6 font-weight-bold mb-3">C. 阅读活动</div>

    <!-- C1: GitHub 风格热力图 -->
    <div class="text-subtitle-2 mb-2 text--secondary">C1 - GitHub 热力图风格（最近30天）</div>
    <v-card class="mb-6 rounded-lg" flat outlined>
      <v-card-text>
        <div class="d-flex flex-wrap" style="gap: 3px;">
          <div v-for="(d, i) in heatmapData" :key="i"
               class="heatmap-cell"
               :style="{background: d === 0 ? 'rgba(128,128,128,0.08)' : `rgba(76,175,80,${0.15 + d * 0.2})`}"
               :title="`${d} 本`"
          />
        </div>
        <div class="d-flex align-center mt-2 text-caption text--secondary">
          <span class="mr-2">少</span>
          <div v-for="n in 5" :key="'h'+n" class="heatmap-cell-legend" :style="{background: `rgba(76,175,80,${n * 0.2})`}"/>
          <span class="ml-2">多</span>
        </div>
      </v-card-text>
    </v-card>

    <!-- C2: 周历紧凑行 -->
    <div class="text-subtitle-2 mb-2 text--secondary">C2 - 本周阅读紧凑行</div>
    <v-card class="mb-6 rounded-lg" flat outlined>
      <v-card-text class="d-flex align-center">
        <div class="mr-4">
          <div class="text-h4 font-weight-bold primary--text">33</div>
          <div class="text-caption text--secondary">本周</div>
        </div>
        <v-divider vertical class="mx-2" style="height: 40px;"/>
        <div class="d-flex flex-grow-1 align-end justify-space-around" style="height: 50px;">
          <div v-for="(d, i) in weekData" :key="i" class="d-flex flex-column align-center" style="flex: 1;">
            <div class="text-caption mb-1" style="font-size: 10px;">{{ d.count > 0 ? d.count : '' }}</div>
            <div :style="{width: '20px', height: d.height + 'px', background: d.today ? 'var(--v-primary-base)' : 'rgba(128,128,128,0.2)', borderRadius: '3px 3px 0 0', transition: 'height 0.3s'}"/>
            <div class="text-caption mt-1" :class="{'font-weight-bold': d.today}" style="font-size: 10px;">{{ d.label }}</div>
          </div>
        </div>
        <v-divider vertical class="mx-2" style="height: 40px;"/>
        <div class="d-flex flex-column ml-4">
          <div class="d-flex align-center text-caption mb-1">
            <v-icon x-small color="orange" class="mr-1">mdi-fire</v-icon>
            连续 5 天
          </div>
          <div class="d-flex align-center text-caption">
            <v-icon x-small class="mr-1">mdi-calendar</v-icon>
            本月 47 本
          </div>
        </div>
      </v-card-text>
    </v-card>

    <v-divider class="mb-8"/>

    <!-- ============================================ -->
    <!-- SECTION D: 阅读目标 / 成就 -->
    <!-- ============================================ -->
    <div class="text-h6 font-weight-bold mb-3">D. 阅读目标 / 成就</div>

    <!-- D1: 年度阅读目标 -->
    <div class="text-subtitle-2 mb-2 text--secondary">D1 - 年度阅读目标进度</div>
    <v-card class="mb-6 rounded-lg" flat outlined>
      <v-card-text class="d-flex align-center">
        <div class="mr-4" style="position: relative; flex-shrink: 0;">
          <v-progress-circular :value="62" :size="70" :width="6" color="primary" rotate="-90">
            <span class="text-body-1 font-weight-bold">62%</span>
          </v-progress-circular>
        </div>
        <div class="flex-grow-1">
          <div class="d-flex align-center justify-space-between">
            <span class="text-body-1 font-weight-bold">2026 阅读目标</span>
            <span class="text-caption text--secondary">1,237 / 2,000 本</span>
          </div>
          <v-progress-linear :value="62" height="8" rounded color="primary" class="mt-2 mb-1"/>
          <div class="d-flex justify-space-between text-caption text--secondary">
            <span>平均每天 3.4 本</span>
            <span>剩余 330 天完成 763 本</span>
          </div>
        </div>
      </v-card-text>
    </v-card>

    <!-- D2: 成就徽章 -->
    <div class="text-subtitle-2 mb-2 text--secondary">D2 - 成就徽章</div>
    <div class="d-flex mb-6" style="gap: 12px;">
      <v-card v-for="a in achievements" :key="a.title" class="rounded-lg text-center" flat outlined style="width: 120px;">
        <div class="pa-3">
          <div class="mb-1" style="font-size: 28px;">{{ a.emoji }}</div>
          <div class="text-caption font-weight-bold">{{ a.title }}</div>
          <div class="text-caption text--secondary" style="font-size: 10px;">{{ a.desc }}</div>
        </div>
      </v-card>
    </div>

    <v-divider class="mb-8"/>

    <!-- ============================================ -->
    <!-- SECTION E: 库概览 -->
    <!-- ============================================ -->
    <div class="text-h6 font-weight-bold mb-3">E. 库概览</div>

    <!-- E1: 水平库卡片 -->
    <div class="text-subtitle-2 mb-2 text--secondary">E1 - 库卡片概览（带比例条）</div>
    <div class="d-flex mb-6" style="gap: 12px;">
      <v-card v-for="lib in libraryCards" :key="lib.name" class="flex-grow-1 rounded-lg" flat outlined style="min-width: 180px;">
        <div class="pa-3">
          <div class="d-flex align-center mb-2">
            <v-avatar :color="lib.color" size="28" class="mr-2">
              <v-icon dark x-small>mdi-book-multiple</v-icon>
            </v-avatar>
            <span class="text-body-2 font-weight-bold">{{ lib.name }}</span>
          </div>
          <div class="d-flex justify-space-between text-caption text--secondary mb-1">
            <span>{{ lib.series }} 系列 · {{ lib.books }} 本</span>
            <span>{{ lib.readPercent }}% 已读</span>
          </div>
          <!-- Stacked bar -->
          <div class="d-flex" style="height: 6px; border-radius: 3px; overflow: hidden;">
            <div :style="{width: lib.readPercent + '%', background: '#4caf50'}"/>
            <div :style="{width: lib.progressPercent + '%', background: '#ff9800'}"/>
            <div :style="{flex: 1, background: 'rgba(128,128,128,0.1)'}"/>
          </div>
          <div class="d-flex mt-1 text-caption" style="font-size: 10px;">
            <span class="mr-3"><span style="color: #4caf50;">●</span> 已读</span>
            <span class="mr-3"><span style="color: #ff9800;">●</span> 在读</span>
            <span><span style="color: rgba(128,128,128,0.4);">●</span> 未读</span>
          </div>
        </div>
      </v-card>
    </div>

    <v-divider class="mb-8"/>

    <!-- ============================================ -->
    <!-- SECTION F: 最近动态时间线 -->
    <!-- ============================================ -->
    <div class="text-h6 font-weight-bold mb-3">F. 最近动态</div>

    <div class="text-subtitle-2 mb-2 text--secondary">F1 - 时间线动态</div>
    <v-card class="mb-6 rounded-lg" flat outlined>
      <v-card-text>
        <div v-for="(event, i) in timeline" :key="i" class="d-flex mb-3">
          <div class="mr-3 d-flex flex-column align-center" style="width: 24px;">
            <v-avatar :color="event.color" size="24">
              <v-icon dark style="font-size: 12px;">{{ event.icon }}</v-icon>
            </v-avatar>
            <div v-if="i < timeline.length - 1" style="width: 2px; flex-grow: 1; background: rgba(128,128,128,0.15); margin-top: 4px;"/>
          </div>
          <div class="flex-grow-1">
            <div class="text-body-2">{{ event.text }}</div>
            <div class="text-caption text--secondary">{{ event.time }}</div>
          </div>
        </div>
      </v-card-text>
    </v-card>

    <v-divider class="mb-8"/>

    <!-- ============================================ -->
    <!-- SECTION G: 组合布局方案 -->
    <!-- ============================================ -->
    <div class="text-h6 font-weight-bold mb-3">G. 组合布局预览（推荐）</div>

    <div class="text-subtitle-2 mb-2 text--secondary">G1 - 方案：欢迎行 + B1统计 + C2活动行 + 原有sections</div>
    <v-card class="mb-6 rounded-lg pa-4" flat outlined>
      <!-- 欢迎行 -->
      <div class="d-flex align-center justify-space-between mb-4">
        <div>
          <span class="text-h5 font-weight-bold">我的书库</span>
          <v-chip small color="primary" class="ml-2">3 个库</v-chip>
        </div>
        <div class="text-caption text--secondary">
          <v-icon x-small color="orange" class="mr-1">mdi-fire</v-icon>
          连续 5 天 ·
          <v-icon x-small class="mr-1">mdi-clock-outline</v-icon>
          最近: 2h前
        </div>
      </div>
      <!-- 统计行 -->
      <div class="d-flex mb-4" style="gap: 12px;">
        <v-card v-for="s in statsB1" :key="'g1'+s.label" class="flex-grow-1 rounded-lg overflow-hidden" flat outlined style="min-width: 120px;">
          <div class="d-flex" style="height: 100%;">
            <div :style="{width: '4px', background: s.color}"/>
            <div class="pa-3 flex-grow-1">
              <div class="text-caption text--secondary">{{ s.label }}</div>
              <div class="text-h5 font-weight-bold my-1">{{ s.value }}</div>
              <div class="text-caption" :style="{color: s.trendColor}">
                <v-icon x-small :color="s.trendColor">{{ s.trendIcon }}</v-icon> {{ s.trend }}
              </div>
            </div>
          </div>
        </v-card>
      </div>
      <!-- 活动行 -->
      <v-card flat outlined class="rounded-lg">
        <v-card-text class="d-flex align-center py-2">
          <div class="mr-3 text-center">
            <div class="text-h5 font-weight-bold primary--text">33</div>
            <div class="text-caption text--secondary">本周</div>
          </div>
          <v-divider vertical class="mx-2" style="height: 36px;"/>
          <div class="d-flex flex-grow-1 align-end justify-space-around" style="height: 40px;">
            <div v-for="(d, i) in weekData" :key="'g1w'+i" class="d-flex flex-column align-center" style="flex: 1;">
              <div :style="{width: '16px', height: d.height * 0.7 + 'px', background: d.today ? 'var(--v-primary-base)' : 'rgba(128,128,128,0.18)', borderRadius: '2px 2px 0 0'}"/>
              <div class="text-caption" style="font-size: 9px;" :class="{'font-weight-bold': d.today}">{{ d.label }}</div>
            </div>
          </div>
        </v-card-text>
      </v-card>
      <div class="text-caption text--secondary text-center mt-3">↓ 下方接原有的 Keep Reading / On Deck / Recently Added 等 sections ↓</div>
    </v-card>

  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'

function miniSparkline(pts: number[]) {
  const w = 100, h = 32, max = Math.max(...pts, 1)
  const step = w / (pts.length - 1)
  const coords = pts.map((v, i) => [i * step, h - (v / max) * h * 0.75 - h * 0.1])
  const line = coords.map(c => c.join(',')).join(' ')
  const area = `M0,${h} ` + coords.map(c => `L${c[0]},${c[1]}`).join(' ') + ` L${w},${h} Z`
  return {line, area}
}

export default Vue.extend({
  name: 'DashboardDemo',
  computed: {
    statsB1(): any[] {
      return [
        {label: '系列总数', value: '240', color: '#1976D2', trend: '+18 本月', trendColor: '#4caf50', trendIcon: 'mdi-trending-up'},
        {label: '书籍总数', value: '4,580', color: '#FF9800', trend: '+126 本月', trendColor: '#4caf50', trendIcon: 'mdi-trending-up'},
        {label: '已读完', value: '1,237', color: '#4CAF50', trend: '27% 完成率', trendColor: '#4caf50', trendIcon: 'mdi-check'},
        {label: '未读', value: '3,343', color: '#78909C', trend: '73% 剩余', trendColor: '#78909c', trendIcon: 'mdi-book-clock'},
      ]
    },
    statsB2(): any[] {
      return [
        {label: '系列总数', value: '240', icon: 'mdi-bookshelf', color: '#1976D2', progress: 100},
        {label: '书籍总数', value: '4,580', icon: 'mdi-book-open-page-variant', color: '#FF9800', progress: 100},
        {label: '已读完', value: '1,237', icon: 'mdi-check-circle', color: '#4CAF50', progress: 27},
        {label: '未读', value: '3,343', icon: 'mdi-book-clock', color: '#78909C', progress: 73},
      ]
    },
    statsB3(): any[] {
      return [
        {label: '系列总数', value: '240', icon: 'mdi-bookshelf', bg: 'linear-gradient(135deg, #1976D2, #42A5F5)', sub: '3 个库'},
        {label: '书籍总数', value: '4,580', icon: 'mdi-book-open-page-variant', bg: 'linear-gradient(135deg, #FF9800, #FFB74D)', sub: '+126'},
        {label: '已读完', value: '1,237', icon: 'mdi-check-circle', bg: 'linear-gradient(135deg, #4CAF50, #81C784)', sub: '27%'},
        {label: '未读', value: '3,343', icon: 'mdi-book-clock', bg: 'linear-gradient(135deg, #546E7A, #90A4AE)', sub: '73%'},
      ]
    },
    statsB4(): any[] {
      const s1 = miniSparkline([18, 22, 19, 25, 28, 24, 30])
      const s2 = miniSparkline([120, 135, 128, 150, 142, 160, 158])
      const s3 = miniSparkline([5, 8, 12, 10, 15, 18, 22])
      const s4 = miniSparkline([95, 88, 82, 78, 72, 65, 60])
      return [
        {label: '系列总数', value: '240', color: '#1976D2', trend: '+12%', up: true, ...s1},
        {label: '书籍总数', value: '4,580', color: '#FF9800', trend: '+8%', up: true, ...s2},
        {label: '已读完', value: '1,237', color: '#4CAF50', trend: '+23%', up: true, ...s3},
        {label: '未读', value: '3,343', color: '#78909C', trend: '-5%', up: false, ...s4},
      ]
    },
    heatmapData(): number[] {
      // 30 days of mock reading data
      return [2, 0, 5, 3, 1, 0, 4, 7, 2, 0, 0, 3, 5, 8, 1, 2, 6, 0, 3, 4, 0, 1, 7, 3, 2, 5, 0, 4, 3, 6]
    },
    weekData(): any[] {
      const labels = ['一', '二', '三', '四', '五', '六', '日']
      const counts = [3, 7, 2, 5, 0, 12, 4]
      const max = Math.max(...counts)
      const today = new Date().getDay()
      const todayIdx = today === 0 ? 6 : today - 1
      return labels.map((label, i) => ({
        label,
        count: counts[i],
        height: Math.max(3, (counts[i] / max) * 40),
        today: i === todayIdx,
      }))
    },
    achievements(): any[] {
      return [
        {emoji: '🔥', title: '连续7天', desc: '连续阅读'},
        {emoji: '📚', title: '百本达成', desc: '累计100本'},
        {emoji: '⚡', title: '速读者', desc: '单日10本'},
        {emoji: '🌙', title: '夜猫子', desc: '凌晨阅读'},
        {emoji: '🏆', title: '全勤月', desc: '本月每天'},
      ]
    },
    libraryCards(): any[] {
      return [
        {name: '漫画库', color: '#1976D2', series: 128, books: 3456, readPercent: 35, progressPercent: 8},
        {name: '轻小说', color: '#388E3C', series: 45, books: 890, readPercent: 52, progressPercent: 12},
        {name: '同人志', color: '#F57C00', series: 67, books: 234, readPercent: 15, progressPercent: 5},
      ]
    },
    timeline(): any[] {
      return [
        {icon: 'mdi-book-open-variant', color: '#4CAF50', text: '读完了 进击的巨人 第42话', time: '2 小时前'},
        {icon: 'mdi-plus', color: '#1976D2', text: '新增 12 本书到 漫画库', time: '5 小时前'},
        {icon: 'mdi-book-open-variant', color: '#4CAF50', text: '读完了 ONE PIECE Vol.8', time: '昨天'},
        {icon: 'mdi-star', color: '#FF9800', text: '达成成就: 连续7天阅读', time: '昨天'},
        {icon: 'mdi-library', color: '#9C27B0', text: '创建了新库: 同人志', time: '3 天前'},
      ]
    },
  },
})
</script>

<style scoped>
.welcome-banner-a1 {
  background: linear-gradient(135deg, #1565C0, #42A5F5, #1E88E5);
  min-height: 80px;
}

.heatmap-cell {
  width: 14px;
  height: 14px;
  border-radius: 2px;
}

.heatmap-cell-legend {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  margin: 0 1px;
}

.b4-spark {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 45%;
  pointer-events: none;
}
</style>
