<template>
  <v-container fluid class="pa-6">
    <!-- Page Header -->
    <div class="d-flex align-center mb-5">
      <v-icon class="mr-2">mdi-chart-box-outline</v-icon>
      <span class="text-h5 font-weight-bold">{{ $t('metrics.title') }}</span>
    </div>

    <!-- Overview Stat Cards -->
    <v-row dense>
      <v-col cols="6" sm="4" md="2" v-if="booksFileSize">
        <v-card class="rounded-lg overflow-hidden" flat outlined style="height: 100%">
          <div class="d-flex" style="height: 100%">
            <div style="width: 4px; background: #1976D2; flex-shrink: 0"/>
            <div class="pa-3">
              <div class="d-flex align-center text-caption text--secondary mb-1">
                <v-icon x-small class="mr-1" color="#1976D2">mdi-harddisk</v-icon>
                {{ $t('common.disk_space') }}
              </div>
              <div class="text-h6 font-weight-bold">
                {{ getFileSize(booksFileSize.measurements[0].value) }}
              </div>
            </div>
          </div>
        </v-card>
      </v-col>

      <v-col cols="6" sm="4" md="2" v-if="series">
        <v-card class="rounded-lg overflow-hidden" flat outlined style="height: 100%">
          <div class="d-flex" style="height: 100%">
            <div style="width: 4px; background: #FF9800; flex-shrink: 0"/>
            <div class="pa-3">
              <div class="d-flex align-center text-caption text--secondary mb-1">
                <v-icon x-small class="mr-1" color="#FF9800">mdi-book-multiple</v-icon>
                {{ $tc('common.series', 2) }}
              </div>
              <div class="text-h6 font-weight-bold">
                {{ formatNumber(series.measurements[0].value) }}
              </div>
            </div>
          </div>
        </v-card>
      </v-col>

      <v-col cols="6" sm="4" md="2" v-if="books">
        <v-card class="rounded-lg overflow-hidden" flat outlined style="height: 100%">
          <div class="d-flex" style="height: 100%">
            <div style="width: 4px; background: #4CAF50; flex-shrink: 0"/>
            <div class="pa-3">
              <div class="d-flex align-center text-caption text--secondary mb-1">
                <v-icon x-small class="mr-1" color="#4CAF50">mdi-book-open-page-variant</v-icon>
                {{ $t('common.books') }}
              </div>
              <div class="text-h6 font-weight-bold">
                {{ formatNumber(books.measurements[0].value) }}
              </div>
            </div>
          </div>
        </v-card>
      </v-col>

      <v-col cols="6" sm="4" md="2" v-if="collections">
        <v-card class="rounded-lg overflow-hidden" flat outlined style="height: 100%">
          <div class="d-flex" style="height: 100%">
            <div style="width: 4px; background: #9C27B0; flex-shrink: 0"/>
            <div class="pa-3">
              <div class="d-flex align-center text-caption text--secondary mb-1">
                <v-icon x-small class="mr-1" color="#9C27B0">mdi-bookmark-multiple</v-icon>
                {{ $t('common.collections') }}
              </div>
              <div class="text-h6 font-weight-bold">
                {{ formatNumber(collections.measurements[0].value) }}
              </div>
            </div>
          </div>
        </v-card>
      </v-col>

      <v-col cols="6" sm="4" md="2" v-if="readlists">
        <v-card class="rounded-lg overflow-hidden" flat outlined style="height: 100%">
          <div class="d-flex" style="height: 100%">
            <div style="width: 4px; background: #009688; flex-shrink: 0"/>
            <div class="pa-3">
              <div class="d-flex align-center text-caption text--secondary mb-1">
                <v-icon x-small class="mr-1" color="#009688">mdi-playlist-play</v-icon>
                {{ $t('common.readlists') }}
              </div>
              <div class="text-h6 font-weight-bold">
                {{ formatNumber(readlists.measurements[0].value) }}
              </div>
            </div>
          </div>
        </v-card>
      </v-col>

      <v-col cols="6" sm="4" md="2" v-if="sidecars">
        <v-card class="rounded-lg overflow-hidden" flat outlined style="height: 100%">
          <div class="d-flex" style="height: 100%">
            <div style="width: 4px; background: #607D8B; flex-shrink: 0"/>
            <div class="pa-3">
              <div class="d-flex align-center text-caption text--secondary mb-1">
                <v-icon x-small class="mr-1" color="#607D8B">mdi-file-tree</v-icon>
                {{ $t('common.sidecars') }}
              </div>
              <div class="text-h6 font-weight-bold">
                {{ formatNumber(sidecars.measurements[0].value) }}
              </div>
            </div>
          </div>
        </v-card>
      </v-col>
    </v-row>

    <!-- Library Distribution Section -->
    <template v-if="fileSizeAllTags || booksAllTags || seriesAllTags || sidecarsAllTags">
      <v-divider class="my-6"/>
      <div class="d-flex align-center mb-4">
        <v-icon small class="mr-2">mdi-chart-donut</v-icon>
        <span class="text-subtitle-1 font-weight-bold">{{ $t('metrics.library_distribution') }}</span>
      </div>

      <v-row>
        <v-col cols="12" sm="6" v-if="fileSizeAllTags">
          <v-card class="rounded-lg" flat outlined>
            <v-card-title class="text-body-2 font-weight-bold">
              {{ $t('metrics.library_disk_space') }}
            </v-card-title>
            <v-card-text>
              <pie-chart :data="fileSizeAllTags" :donut="true" :bytes="true" legend="bottom"/>
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" sm="6" v-if="booksAllTags">
          <v-card class="rounded-lg" flat outlined>
            <v-card-title class="text-body-2 font-weight-bold">
              {{ $t('metrics.library_books') }}
            </v-card-title>
            <v-card-text>
              <pie-chart :data="booksAllTags" :donut="true" legend="bottom"/>
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" sm="6" v-if="seriesAllTags">
          <v-card class="rounded-lg" flat outlined>
            <v-card-title class="text-body-2 font-weight-bold">
              {{ $t('metrics.library_series') }}
            </v-card-title>
            <v-card-text>
              <pie-chart :data="seriesAllTags" :donut="true" legend="bottom"/>
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" sm="6" v-if="sidecarsAllTags">
          <v-card class="rounded-lg" flat outlined>
            <v-card-title class="text-body-2 font-weight-bold">
              {{ $t('metrics.library_sidecars') }}
            </v-card-title>
            <v-card-text>
              <pie-chart :data="sidecarsAllTags" :donut="true" legend="bottom"/>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </template>

    <!-- Task Performance Section -->
    <template v-if="tasksCount || tasksTotalTime">
      <v-divider class="my-6"/>
      <div class="d-flex align-center mb-4">
        <v-icon small class="mr-2">mdi-chart-bar</v-icon>
        <span class="text-subtitle-1 font-weight-bold">{{ $t('metrics.task_performance') }}</span>
      </div>

      <v-row>
        <v-col cols="12" sm="6" v-if="tasksCount">
          <v-card class="rounded-lg" flat outlined>
            <v-card-title class="text-body-2 font-weight-bold">
              {{ $t('metrics.tasks_executed') }}
            </v-card-title>
            <v-card-text>
              <bar-chart :data="tasksCount"/>
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" sm="6" v-if="tasksTotalTime">
          <v-card class="rounded-lg" flat outlined>
            <v-card-title class="text-body-2 font-weight-bold">
              {{ $t('metrics.tasks_total_time') }}
            </v-card-title>
            <v-card-text>
              <bar-chart :data="tasksTotalTime" suffix="s" :round="0"/>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </template>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import {MetricDto} from '@/types/komga-metrics'
import {getFileSize} from '@/functions/file'

export default Vue.extend({
  name: 'MetricsView',
  data: () => ({
    getFileSize,
    tasks: undefined as unknown as MetricDto,
    tasksCount: undefined as unknown as { [key: string]: number | undefined } | undefined,
    tasksTotalTime: undefined as unknown as { [key: string]: number | undefined } | undefined,
    series: undefined as unknown as MetricDto,
    seriesAllTags: undefined as unknown as { [key: string]: number | undefined } | undefined,
    books: undefined as unknown as MetricDto,
    booksAllTags: undefined as unknown as { [key: string]: number | undefined } | undefined,
    sidecars: undefined as unknown as MetricDto,
    sidecarsAllTags: undefined as unknown as { [key: string]: number | undefined } | undefined,
    booksFileSize: undefined as unknown as MetricDto,
    fileSizeAllTags: undefined as unknown as { [key: string]: number | undefined } | undefined,
    collections: undefined as unknown as MetricDto,
    readlists: undefined as unknown as MetricDto,
  }),
  computed: {},
  mounted() {
    this.loadData()
  },
  methods: {
    formatNumber(n: number): string {
      return Math.round(n).toLocaleString()
    },
    getTaskTypeName(taskType: string): string {
      const key = `common.task_type.${taskType}`
      return this.$te(key) ? this.$t(key) as string : taskType
    },
    getLibraryNameById(id: string): string {
      return this.$store.getters.getLibraryById(id).name
    },
    async loadData() {
      this.$komgaMetrics.getMetric('komga.tasks.execution')
        .then(m => {
          this.tasks = m
          this.getStatisticForEachTagValue(m, 'type', 'COUNT', this.getTaskTypeName)
            .then(m => this.tasksCount = m)
            .catch(() => {
            })
          this.getStatisticForEachTagValue(m, 'type', 'TOTAL_TIME', this.getTaskTypeName)
            .then(m => this.tasksTotalTime = m)
            .catch(() => {
            })
        })
        .catch(() => {
        })

      this.$komgaMetrics.getMetric('komga.series')
        .then(m => {
            this.series = m
            this.getStatisticForEachTagValue(m, 'library', 'VALUE', this.getLibraryNameById)
              .then(v => this.seriesAllTags = v)
          },
        )
        .catch(() => {
        })

      this.$komgaMetrics.getMetric('komga.books')
        .then(m => {
            this.books = m
            this.getStatisticForEachTagValue(m, 'library', 'VALUE', this.getLibraryNameById)
              .then(v => this.booksAllTags = v)
          },
        )
        .catch(() => {
        })

      this.$komgaMetrics.getMetric('komga.books.filesize')
        .then(m => {
            this.booksFileSize = m
            this.getStatisticForEachTagValue(m, 'library', 'VALUE', this.getLibraryNameById)
              .then(v => this.fileSizeAllTags = v)
          },
        )
        .catch(() => {
        })

      this.$komgaMetrics.getMetric('komga.sidecars')
        .then(m => {
            this.sidecars = m
            this.getStatisticForEachTagValue(m, 'library', 'VALUE', this.getLibraryNameById)
              .then(v => this.sidecarsAllTags = v)
          },
        )
        .catch(() => {
        })

      this.$komgaMetrics.getMetric('komga.collections')
        .then(m => this.collections = m)
        .catch(() => {
        })

      this.$komgaMetrics.getMetric('komga.readlists')
        .then(m => this.readlists = m)
        .catch(() => {
        })
    },
    async getStatisticForEachTagValue(metric: MetricDto, tag: string, statistic: string = 'VALUE', tagTransform: (t: string) => string = x => x): Promise<{
      [key: string]: number | undefined
    } | undefined> {
      const tagDto = metric.availableTags.find(x => x.tag === tag)
      if (tagDto) {
        const result = {} as { [key: string]: number | undefined }
        for (const tagValue of tagDto.values) {
          result[tagTransform(tagValue)] = (await this.$komgaMetrics.getMetric(metric.name, [{
            key: tag,
            value: tagValue,
          }])).measurements.find(x => x.statistic === statistic)?.value
        }
        return result
      }
      return undefined
    },
  },
})
</script>

<style scoped>

</style>
