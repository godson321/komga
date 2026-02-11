<template>
  <v-container fluid class="pa-6">
    <!-- Operations row -->
    <v-row class="mb-2" align="center">
      <v-col cols="auto">
        <v-tooltip bottom>
          <template v-slot:activator="{ on, attrs }">
            <v-btn color="primary" v-bind="attrs" v-on="on" :loading="loadingGlobal"
                   @click="confirmCropDoublePageDialog = true">
              <v-icon left>mdi-book-open-page-variant-outline</v-icon>
              {{ $t('cover_crop.crop_double_page') }}
            </v-btn>
          </template>
          <span>{{ hasActiveFilters ? $t('cover_crop.crop_double_page_tooltip_filtered') : $t('cover_crop.crop_double_page_tooltip') }}</span>
        </v-tooltip>
      </v-col>
      <v-col cols="auto">
        <v-tooltip bottom>
          <template v-slot:activator="{ on, attrs }">
            <v-btn color="warning" v-bind="attrs" v-on="on" :loading="loadingGlobal"
                   @click="confirmRestoreAllDialog = true">
              <v-icon left>mdi-undo-variant</v-icon>
              {{ $t('cover_crop.restore_all') }}
            </v-btn>
          </template>
          <span>{{ hasActiveFilters ? $t('cover_crop.restore_all_tooltip_filtered') : $t('cover_crop.restore_all_tooltip') }}</span>
        </v-tooltip>
      </v-col>
      <v-spacer/>
      <v-col cols="auto">
        <v-btn color="primary" outlined :disabled="effectiveSelectedCount === 0" :loading="loadingBatch"
               @click="batchCrop(true)">
          <v-icon left>mdi-arrow-collapse-left</v-icon>
          {{ $t('cover_crop.batch_crop_left', {count: effectiveSelectedCount}) }}
        </v-btn>
      </v-col>
      <v-col cols="auto">
        <v-btn color="primary" outlined :disabled="effectiveSelectedCount === 0" :loading="loadingBatch"
               @click="batchCrop(false)">
          <v-icon left>mdi-arrow-collapse-right</v-icon>
          {{ $t('cover_crop.batch_crop_right', {count: effectiveSelectedCount}) }}
        </v-btn>
      </v-col>
      <v-col cols="auto">
        <v-btn color="warning" outlined :disabled="effectiveSelectedCount === 0" :loading="loadingBatch"
               @click="batchRestore">
          <v-icon left>mdi-undo</v-icon>
          {{ $t('cover_crop.batch_restore', {count: effectiveSelectedCount}) }}
        </v-btn>
      </v-col>
    </v-row>

    <!-- Data table -->
    <v-data-table
      v-model="selected"
      :headers="headers"
      :items="books"
      :options.sync="options"
      :server-items-length="totalBooks"
      :loading="loading"
      show-select
      item-key="id"
      class="elevation-1"
      :footer-props="footerProps"
      hide-default-footer
    >
      <template v-slot:top>
        <v-container>
          <v-row align="center">
            <v-col cols="12" sm="3">
              <v-select v-model="filterLibraries"
                        :items="filterLibrariesOptions"
                        :label="$t('cover_crop.col_library')"
                        clearable
                        solo
                        multiple
                        chips
                        deletable-chips
                        hide-details
              />
            </v-col>
            <v-col cols="12" sm="3">
              <v-autocomplete v-model="filterSeries"
                              :items="seriesOptions"
                              :search-input.sync="seriesSearch"
                              :label="$t('cover_crop.col_series')"
                              clearable
                              solo
                              multiple
                              chips
                              deletable-chips
                              hide-details
                              :loading="seriesLoading"
                              cache-items
                              :no-data-text="$t('cover_crop.series_no_data')"
              />
            </v-col>
            <v-col cols="12" sm="3">
              <v-text-field v-model="bookSearch"
                            :label="$t('cover_crop.search_book')"
                            clearable
                            solo
                            hide-details
                            prepend-inner-icon="mdi-magnify"
              />
            </v-col>
            <v-col cols="12" sm="3">
              <v-select v-model="filterPageType"
                        :items="pageTypeOptions"
                        :label="$t('cover_crop.filter_page_type')"
                        solo
                        hide-details
              />
            </v-col>
          </v-row>
          <!-- Select all pages banner -->
          <v-alert v-if="selected.length > 0" dense text type="info" class="mt-2 mb-0">
            <div class="d-flex align-center">
              <span class="text-body-2">{{ $t('cover_crop.selected_actions', {count: effectiveSelectedCount}) }}</span>
              <span v-if="allCurrentPageSelected && !selectAllPages && totalBooks > books.length" class="text-body-2 ml-2">
                — {{ $t('cover_crop.current_page_selected', {count: books.length}) }}
                <a class="text-decoration-underline" style="cursor: pointer" @click="selectAllPages = true">
                  {{ $t('cover_crop.select_all_pages', {count: totalBooks}) }}
                </a>
              </span>
              <span v-if="selectAllPages" class="text-body-2 ml-2">
                — {{ $t('cover_crop.all_pages_selected', {count: totalBooks}) }}
                <a class="text-decoration-underline" style="cursor: pointer" @click="selectAllPages = false">
                  {{ $t('cover_crop.clear_select_all') }}
                </a>
              </span>
              <v-spacer/>
              <v-btn icon x-small @click="clearSelection">
                <v-icon small>mdi-close</v-icon>
              </v-btn>
            </div>
          </v-alert>
          <div v-if="totalBooks > 0" class="d-flex align-center mt-2">
            <v-btn icon @click="loadBooks">
              <v-icon>mdi-refresh</v-icon>
            </v-btn>
            <v-data-footer
              :options.sync="options"
              :pagination="tablePagination"
              :items-per-page-options="[20, 50, 100]"
              show-first-last-page
              class="flex-grow-1 cover-crop-footer"
            />
          </div>
        </v-container>
      </template>

      <template v-slot:footer>
        <v-data-footer
          :options.sync="options"
          :pagination="tablePagination"
          :items-per-page-options="[20, 50, 100]"
          show-first-last-page
          class="cover-crop-footer"
        />
      </template>

      <template v-slot:item.thumbnail="{ item }">
        <v-img
          :src="bookThumbnailUrl(item.id)"
          max-height="80"
          max-width="60"
          contain
          class="my-1 rounded"
        />
      </template>

      <template v-slot:item.libraryId="{ item }">
        {{ getLibraryName(item.libraryId) }}
      </template>

      <template v-slot:item.seriesTitle="{ item }">
        <router-link
          :to="{name: item.oneshot ? 'browse-oneshot' : 'browse-series', params: {seriesId: item.seriesId}}"
        >{{ item.seriesTitle }}
        </router-link>
      </template>

      <template v-slot:item.name="{ item }">
        <router-link
          :to="{name: item.oneshot ? 'browse-oneshot' : 'browse-book', params: {bookId: item.id, seriesId: item.seriesId}}"
        >{{ item.name }}
        </router-link>
      </template>

      <template v-slot:item.actions="{ item }">
        <v-btn icon small @click="cropBook(item.id, true)" :title="$t('cover_crop.crop_left')">
          <v-icon small>mdi-arrow-collapse-left</v-icon>
        </v-btn>
        <v-btn icon small @click="cropBook(item.id, false)" :title="$t('cover_crop.crop_right')">
          <v-icon small>mdi-arrow-collapse-right</v-icon>
        </v-btn>
        <v-btn icon small @click="restoreBook(item.id)" :title="$t('cover_crop.restore')">
          <v-icon small>mdi-undo</v-icon>
        </v-btn>
      </template>

    </v-data-table>

    <!-- Confirm auto-crop dialog -->
    <v-dialog v-model="confirmCropDoublePageDialog" max-width="500">
      <v-card>
        <v-card-title>{{ $t('cover_crop.crop_double_page') }}</v-card-title>
        <v-card-text>
          {{ hasActiveFilters ? $t('cover_crop.confirm_crop_double_page_filtered') : $t('cover_crop.confirm_crop_double_page') }}
        </v-card-text>
        <v-card-actions>
          <v-spacer/>
          <v-btn text @click="confirmCropDoublePageDialog = false">{{ $t('cover_crop.button_cancel') }}</v-btn>
          <v-btn color="primary" text @click="doCropDoublePage">{{ $t('cover_crop.button_confirm') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Confirm restore-all dialog -->
    <v-dialog v-model="confirmRestoreAllDialog" max-width="500">
      <v-card>
        <v-card-title>{{ $t('cover_crop.restore_all') }}</v-card-title>
        <v-card-text>
          {{ hasActiveFilters ? $t('cover_crop.confirm_restore_filtered') : $t('cover_crop.confirm_restore_all') }}
        </v-card-text>
        <v-card-actions>
          <v-spacer/>
          <v-btn text @click="confirmRestoreAllDialog = false">{{ $t('cover_crop.button_cancel') }}</v-btn>
          <v-btn color="warning" text @click="doRestoreAll">{{ $t('cover_crop.button_confirm') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-snackbar v-model="snackbar" :timeout="3000" color="success" top>
      {{ $t('cover_crop.notification_started') }}
    </v-snackbar>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import {ERROR, ErrorEvent} from '@/types/events'
import {BookDto} from '@/types/komga-books'
import {bookThumbnailUrl} from '@/functions/urls'
import {
  BookSearch,
  SearchConditionAllOfBook,
  SearchConditionAnyOfBook,
  SearchConditionBook,
  SearchConditionLibraryId,
  SearchConditionPoster,
  SearchConditionSeriesId,
  SearchOperatorIs,
} from '@/types/komga-search'
import {debounce} from 'lodash'

export default Vue.extend({
  name: 'CoverCropManagement',
  components: {},
  data: function () {
    return {
      books: [] as BookDto[],
      totalBooks: 0,
      loading: true,
      options: {} as any,
      selected: [] as BookDto[],
      filterLibraries: [] as string[],
      filterSeries: [] as string[],
      seriesSearch: '',
      seriesOptions: [] as { text: string, value: string }[],
      seriesLoading: false,
      bookSearch: '',
      filterPageType: '',
      loadingBatch: false,
      loadingGlobal: false,
      snackbar: false,
      selectAllPages: false,
      confirmCropDoublePageDialog: false,
      confirmRestoreAllDialog: false,
    }
  },
  watch: {
    options: {
      handler() {
        this.loadBooks()
      },
      deep: true,
    },
    selected(val: BookDto[]) {
      if (val.length < this.books.length) {
        this.selectAllPages = false
      }
    },
    filterLibraries() {
      this.selectAllPages = false
      if (this.options.page === 1) {
        this.loadBooks()
      } else {
        this.options.page = 1
      }
    },
    filterSeries() {
      if (this.options.page === 1) {
        this.loadBooks()
      } else {
        this.options.page = 1
      }
    },
    seriesSearch: {
      handler: debounce(async function (this: any, val: string) {
        if (!val || val.length < 2) return
        this.seriesLoading = true
        try {
          const page = await this.$komgaSeries.getSeriesList(
            {fullTextSearch: val},
            {size: 20},
          )
          this.seriesOptions = page.content.map((s: any) => ({
            text: s.metadata.title,
            value: s.id,
          }))
        } catch (e) {
          // ignore
        } finally {
          this.seriesLoading = false
        }
      }, 500),
    },
    bookSearch: {
      handler: debounce(function (this: any) {
        if (this.options.page === 1) {
          this.loadBooks()
        } else {
          this.options.page = 1
        }
      }, 500),
    },
    filterPageType() {
      if (this.options.page === 1) {
        this.loadBooks()
      } else {
        this.options.page = 1
      }
    },
  },
  computed: {
    hasActiveFilters(): boolean {
      return this.filterLibraries.length > 0 || this.filterSeries.length > 0 || !!(this.bookSearch && this.bookSearch.trim()) || !!this.filterPageType
    },
    allCurrentPageSelected(): boolean {
      return this.books.length > 0 && this.selected.length === this.books.length
    },
    effectiveSelectedCount(): number {
      return this.selectAllPages ? this.totalBooks : this.selected.length
    },
    pageCount(): number {
      return Math.ceil(this.totalBooks / (this.options.itemsPerPage || 20))
    },
    footerProps(): object {
      return {
        itemsPerPageOptions: [20, 50, 100],
        showFirstLastPage: true,
      }
    },
    tablePagination(): object {
      const page = this.options.page || 1
      const itemsPerPage = this.options.itemsPerPage || 20
      return {
        page,
        itemsPerPage,
        pageStart: (page - 1) * itemsPerPage,
        pageStop: Math.min(page * itemsPerPage, this.totalBooks),
        pageCount: this.pageCount,
        itemsLength: this.totalBooks,
      }
    },
    pageTypeOptions(): object[] {
      return [
        {text: this.$t('cover_crop.page_type_all'), value: ''},
        {text: this.$t('cover_crop.page_type_double'), value: 'DOUBLE'},
        {text: this.$t('cover_crop.page_type_single'), value: 'SINGLE'},
      ]
    },
    filterLibrariesOptions(): object[] {
      return this.$store.state.komgaLibraries.libraries.map(x => ({
        text: x.name,
        value: x.id,
      }))
    },
    headers(): object[] {
      return [
        {text: this.$t('cover_crop.col_thumbnail'), value: 'thumbnail', sortable: false, width: '80px'},
        {text: this.$t('cover_crop.col_library'), value: 'libraryId', sortable: false},
        {text: this.$t('cover_crop.col_series'), value: 'seriesTitle', sortable: false},
        {text: this.$t('cover_crop.col_book'), value: 'name'},
        {text: this.$t('cover_crop.col_actions'), value: 'actions', sortable: false, width: '140px'},
      ]
    },
  },
  methods: {
    bookThumbnailUrl,
    getLibraryName(libraryId: string): string {
      return this.$store.getters.getLibraryById(libraryId).name
    },
    buildSearchCondition(): SearchConditionBook | undefined {
      const conditions = [] as SearchConditionBook[]
      if (this.filterLibraries.length > 0) {
        conditions.push(new SearchConditionAnyOfBook(
          this.filterLibraries.map(x => new SearchConditionLibraryId(new SearchOperatorIs(x))),
        ))
      }
      if (this.filterSeries.length > 0) {
        conditions.push(new SearchConditionAnyOfBook(
          this.filterSeries.map(x => new SearchConditionSeriesId(new SearchOperatorIs(x))),
        ))
      }
      if (this.filterPageType) {
        conditions.push(new SearchConditionPoster(new SearchOperatorIs({
          doublePage: this.filterPageType === 'DOUBLE',
          selected: true,
        })))
      }
      return conditions.length > 0 ? new SearchConditionAllOfBook(conditions) : undefined
    },
    buildBookSearch(): BookSearch {
      return {
        condition: this.buildSearchCondition(),
        fullTextSearch: this.bookSearch?.trim() || undefined,
      } as BookSearch
    },
    async loadBooks() {
      this.loading = true
      const {sortBy, sortDesc, page, itemsPerPage} = this.options

      const pageRequest = {
        page: page - 1,
        size: itemsPerPage,
        sort: [],
      } as PageRequest

      for (let i = 0; i < sortBy.length; i++) {
        pageRequest.sort!!.push(`${sortBy[i]},${sortDesc[i] ? 'desc' : 'asc'}`)
      }

      try {
        const booksPage = await this.$komgaBooks.getBooksList(this.buildBookSearch(), pageRequest)
        this.totalBooks = booksPage.totalElements
        this.books = booksPage.content
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      }
      this.loading = false
    },
    async fetchAllFilteredBookIds(): Promise<string[]> {
      const booksPage = await this.$komgaBooks.getBooksList(this.buildBookSearch(), {unpaged: true})
      return booksPage.content.map(b => b.id)
    },

    // Single book actions
    async cropBook(bookId: string, keepLeft: boolean) {
      try {
        await this.$komgaBooks.cropThumbnail(bookId, keepLeft)
        this.snackbar = true
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      }
    },
    async restoreBook(bookId: string) {
      try {
        await this.$komgaBooks.restoreThumbnail(bookId)
        this.snackbar = true
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      }
    },

    // Batch actions on selected
    clearSelection() {
      this.selected = []
      this.selectAllPages = false
    },
    async getEffectiveBookIds(): Promise<string[]> {
      if (this.selectAllPages) {
        return this.fetchAllFilteredBookIds()
      }
      return this.selected.map(b => b.id)
    },
    async batchCrop(keepLeft: boolean) {
      this.loadingBatch = true
      try {
        const bookIds = await this.getEffectiveBookIds()
        await this.$komgaBooks.cropBatchThumbnails(bookIds, keepLeft)
        this.snackbar = true
        this.clearSelection()
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      } finally {
        this.loadingBatch = false
      }
    },
    async batchRestore() {
      this.loadingBatch = true
      try {
        const bookIds = await this.getEffectiveBookIds()
        await this.$komgaBooks.restoreBatchThumbnails(bookIds)
        this.snackbar = true
        this.clearSelection()
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      } finally {
        this.loadingBatch = false
      }
    },

    // Global actions: auto crop double pages & restore all
    async doCropDoublePage() {
      this.confirmCropDoublePageDialog = false
      this.loadingGlobal = true
      try {
        if (this.hasActiveFilters) {
          const bookIds = await this.fetchAllFilteredBookIds()
          await this.$komgaBooks.cropBatchThumbnails(bookIds, true, false)
        } else {
          await this.$komgaBooks.cropDoublePageThumbnails()
        }
        this.snackbar = true
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      } finally {
        this.loadingGlobal = false
      }
    },
    async doRestoreAll() {
      this.confirmRestoreAllDialog = false
      this.loadingGlobal = true
      try {
        if (this.hasActiveFilters) {
          const bookIds = await this.fetchAllFilteredBookIds()
          await this.$komgaBooks.restoreBatchThumbnails(bookIds)
        } else {
          await this.$komgaBooks.restoreAllThumbnails()
        }
        this.snackbar = true
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      } finally {
        this.loadingGlobal = false
      }
    },
  },
})
</script>

<style scoped>
.cover-crop-footer {
  border-top: none !important;
}

::v-deep .v-data-footer,
::v-deep .v-data-footer .v-select__selection,
::v-deep .v-data-footer .v-select__selections input {
  font-size: 1rem !important;
}

::v-deep .v-data-footer .v-icon {
  font-size: 1.4rem !important;
}

::v-deep .v-data-footer .v-btn--icon.v-size--default {
  height: 36px;
  width: 36px;
}
</style>
