<template>
  <v-row class="mb-4">
    <!-- Continue Reading - compact list with progress -->
    <v-col cols="12" md="6">
      <v-card class="rounded-lg" flat outlined style="height: 100%;">
        <v-card-title class="text-subtitle-1 font-weight-bold pb-1">
          <v-icon class="mr-2" small>mdi-book-open-variant</v-icon>
          {{ $t('dashboard_quick.continue_reading') }}
        </v-card-title>
        <v-card-text class="pt-0">
          <v-list dense class="pa-0">
            <v-list-item
              v-for="(book, i) in continueReadingBooks"
              :key="i"
              class="px-0 quick-item"
              @click="navigateToBook(book)"
            >
              <v-list-item-avatar tile size="40" class="my-1 rounded">
                <v-img
                  :src="book.thumbnailUrl"
                  :lazy-src="book.thumbnailUrl"
                >
                  <template v-slot:placeholder>
                    <v-row class="fill-height ma-0" align="center" justify="center">
                      <v-icon small>mdi-image</v-icon>
                    </v-row>
                  </template>
                </v-img>
              </v-list-item-avatar>
              <v-list-item-content>
                <v-list-item-title class="text-body-2">{{ book.title }}</v-list-item-title>
                <v-list-item-subtitle class="text-caption">{{ book.series }}</v-list-item-subtitle>
                <v-progress-linear
                  :value="book.progress"
                  height="3"
                  rounded
                  class="mt-1"
                  :color="book.progress > 80 ? 'success' : 'primary'"
                />
              </v-list-item-content>
              <v-list-item-action class="my-0">
                <v-btn icon x-small color="primary">
                  <v-icon small>mdi-play-circle</v-icon>
                </v-btn>
              </v-list-item-action>
            </v-list-item>
          </v-list>
          <div v-if="continueReadingBooks.length === 0" class="text-center text-caption text--secondary py-4">
            {{ $t('dashboard_quick.no_in_progress') }}
          </div>
        </v-card-text>
      </v-card>
    </v-col>

    <!-- Library Quick Access -->
    <v-col cols="12" md="6">
      <v-card class="rounded-lg" flat outlined style="height: 100%;">
        <v-card-title class="text-subtitle-1 font-weight-bold pb-1">
          <v-icon class="mr-2" small>mdi-lightning-bolt</v-icon>
          {{ $t('dashboard_quick.quick_access') }}
        </v-card-title>
        <v-card-text class="pt-0">
          <v-list dense class="pa-0">
            <v-list-item
              v-for="(lib, i) in libraryItems"
              :key="i"
              class="px-0 quick-item"
              @click="navigateToLibrary(lib)"
            >
              <v-list-item-avatar size="36" class="my-1">
                <v-avatar :color="lib.color" size="36">
                  <v-icon dark small>{{ lib.icon }}</v-icon>
                </v-avatar>
              </v-list-item-avatar>
              <v-list-item-content>
                <v-list-item-title class="text-body-2">{{ lib.name }}</v-list-item-title>
                <v-list-item-subtitle class="text-caption">
                  {{ $t('dashboard_quick.lib_summary', {series: lib.seriesCount, books: lib.bookCount}) }}
                </v-list-item-subtitle>
              </v-list-item-content>
              <v-list-item-action class="my-0">
                <v-chip x-small outlined>
                  {{ lib.unreadCount }} {{ $t('dashboard_quick.unread') }}
                </v-chip>
              </v-list-item-action>
            </v-list-item>
          </v-list>

          <!-- Quick action buttons -->
          <v-divider class="my-2"/>
          <div class="d-flex flex-wrap">
            <v-btn x-small text color="primary" class="mr-2 mb-1" @click="$router.push({name: 'search'})">
              <v-icon x-small class="mr-1">mdi-magnify</v-icon>
              {{ $t('dashboard_quick.search') }}
            </v-btn>
            <v-btn x-small text color="primary" class="mr-2 mb-1" @click="$router.push({name: 'browse-collections', params: {libraryId: 'all'}})">
              <v-icon x-small class="mr-1">mdi-bookmark-multiple</v-icon>
              {{ $t('dashboard_quick.collections') }}
            </v-btn>
            <v-btn x-small text color="primary" class="mb-1" @click="$router.push({name: 'browse-readlists', params: {libraryId: 'all'}})">
              <v-icon x-small class="mr-1">mdi-playlist-play</v-icon>
              {{ $t('dashboard_quick.readlists') }}
            </v-btn>
          </div>
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>
</template>

<script lang="ts">
import Vue from 'vue'
import {LibraryDto} from '@/types/komga-libraries'

interface ContinueBook {
  id: string
  title: string
  series: string
  progress: number
  thumbnailUrl: string
  seriesId: string
}

interface LibraryItem {
  id: string
  name: string
  icon: string
  color: string
  seriesCount: number
  bookCount: number
  unreadCount: number
}

const LIBRARY_COLORS = ['#1976D2', '#388E3C', '#F57C00', '#7B1FA2', '#C62828', '#00838F']

export default Vue.extend({
  name: 'DashboardQuickAccess',
  props: {
    mockMode: {type: Boolean, default: true},
  },
  computed: {
    libraries(): LibraryDto[] {
      return this.$store.getters.getLibrariesPinned || []
    },
    continueReadingBooks(): ContinueBook[] {
      // TODO: connect to real reading progress API
      return []
    },
    libraryItems(): LibraryItem[] {
      if (this.libraries.length === 0) {
        return []
      }
      return this.libraries.map((lib: LibraryDto, index: number) => ({
        id: lib.id,
        name: lib.name,
        icon: 'mdi-book-multiple',
        color: LIBRARY_COLORS[index % LIBRARY_COLORS.length],
        seriesCount: 0,
        bookCount: 0,
        unreadCount: 0,
      }))
    },
  },
  methods: {
    navigateToBook(book: ContinueBook) {
      if (!this.mockMode) {
        this.$router.push({name: 'browse-book', params: {bookId: book.id}})
      }
    },
    navigateToLibrary(lib: LibraryItem) {
      this.$router.push({name: 'libraries', params: {libraryId: lib.id}})
    },
  },
})
</script>

<style scoped>
.quick-item {
  border-radius: 8px;
  transition: background-color 0.15s ease;
  min-height: 52px !important;
}

.quick-item:hover {
  background-color: rgba(128, 128, 128, 0.08);
}
</style>
