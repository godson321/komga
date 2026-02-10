<template>
  <div :style="$vuetify.breakpoint.xs ? 'margin-bottom: 56px' : undefined">
    <toolbar-sticky v-if="selectedSeries.length === 0 && selectedBooks.length === 0">
      <!--   Action menu   -->
      <library-actions-menu v-if="isAdmin && library"
                            :library="library"/>

      <v-toolbar-title>
        <span>{{ library ? library.name : '' }}</span>
      </v-toolbar-title>

      <v-spacer/>

      <library-navigation v-if="$vuetify.breakpoint.mdAndUp" :libraryId="libraryId"/>

      <v-spacer/>

    </toolbar-sticky>

    <library-navigation v-if="$vuetify.breakpoint.smAndDown" :libraryId="libraryId"
                        bottom-navigation/>

    <multi-select-bar
      v-model="selectedSeries"
      kind="series"
      @unselect-all="selectedSeries = []"
      @mark-read="markSelectedSeriesRead"
      @mark-unread="markSelectedSeriesUnread"
      @add-to-collection="addToCollection"
      @edit="editMultipleSeries"
    />

    <multi-select-bar
      v-model="selectedBooks"
      kind="books"
      :oneshots="selectedOneshots"
      @unselect-all="selectedBooks = []"
      @mark-read="markSelectedBooksRead"
      @mark-unread="markSelectedBooksUnread"
      @add-to-readlist="addToReadList"
      @edit="editMultipleBooks"
    />

    <v-container fluid class="pa-6">
      <!-- ========== 1. 继续阅读 (Continue Reading) ========== -->
      <template v-if="keepReadingBooks.length > 0">
        <section-header
          icon="mdi-book-open-page-variant"
          icon-color="#4CAF50"
          :title="$t('recommendation.keep_reading')"
          :subtitle="$t('recommendation.keep_reading_desc')"
          :count="keepReadingBooks.length"
        />
        <horizontal-scroller class="mb-8" :tick="keepReadingTick" @scroll-changed="(p) => scrollChanged(keepReadingLoader, p)">
          <template v-slot:prepend><span/></template>
          <template v-slot:content>
            <item-browser
              :items="keepReadingBooks"
              :item-context="[ItemContext.SHOW_SERIES]"
              nowrap
              :fixed-item-width="fixedCardWidth"
              :edit-function="isAdmin ? singleEditBook : undefined"
              :selected.sync="selectedBooks"
              :selectable="selectedSeries.length === 0"
            />
          </template>
        </horizontal-scroller>
      </template>

      <!-- ========== 2. 接下来阅读 (On Deck) ========== -->
      <template v-if="onDeckBooks.length > 0">
        <section-header
          icon="mdi-playlist-play"
          icon-color="#2196F3"
          :title="$t('recommendation.on_deck')"
          :subtitle="$t('recommendation.on_deck_desc')"
          :count="onDeckBooks.length"
        />
        <horizontal-scroller class="mb-8" :tick="onDeckTick" @scroll-changed="(p) => scrollChanged(onDeckLoader, p)">
          <template v-slot:prepend><span/></template>
          <template v-slot:content>
            <item-browser
              :items="onDeckBooks"
              :item-context="[ItemContext.SHOW_SERIES]"
              nowrap
              :fixed-item-width="fixedCardWidth"
              :edit-function="isAdmin ? singleEditBook : undefined"
              :selected.sync="selectedBooks"
              :selectable="selectedSeries.length === 0"
            />
          </template>
        </horizontal-scroller>
      </template>

      <!-- ========== 3. 猜你喜欢 (Because You Read - Genre based) ========== -->
      <template v-if="genreRecommendations.length > 0">
        <section-header
          icon="mdi-lightbulb-on-outline"
          icon-color="#FF9800"
          :title="$t('recommendation.because_you_read')"
          :subtitle="$t('recommendation.because_you_read_desc')"
        />
        <div class="mb-8">
          <div v-for="rec in genreRecommendations" :key="'byr-'+rec.genre" class="mb-5">
            <div class="d-flex align-center mb-2">
              <v-avatar size="20" color="#FF9800" class="mr-2">
                <v-icon dark style="font-size: 11px;">mdi-tag</v-icon>
              </v-avatar>
              <span class="text-body-2">{{ $t('recommendation.genre_label') }}</span>
              <v-chip small color="primary" class="mx-2" outlined>{{ rec.genre }}</v-chip>
              <v-divider class="mx-2"/>
              <span class="text-caption text--secondary">{{ rec.items.length }} {{ $t('recommendation.unread_series_unit') }}</span>
            </div>
            <horizontal-scroller>
              <template v-slot:prepend><span/></template>
              <template v-slot:content>
                <item-browser
                  :items="rec.items"
                  nowrap
                  :fixed-item-width="fixedCardWidthSmall"
                  :edit-function="isAdmin ? singleEditSeries : undefined"
                  :selected.sync="selectedSeries"
                  :selectable="selectedBooks.length === 0"
                />
              </template>
            </horizontal-scroller>
          </div>
        </div>
      </template>

      <!-- ========== 4. 快要读完了 (Almost Complete Series) ========== -->
      <template v-if="almostCompleteSeries.length > 0">
        <section-header
          icon="mdi-flag-checkered"
          icon-color="#9C27B0"
          :title="$t('recommendation.almost_complete')"
          :subtitle="$t('recommendation.almost_complete_desc')"
          :count="almostCompleteSeries.length"
        />
        <horizontal-scroller class="mb-8">
          <template v-slot:prepend><span/></template>
          <template v-slot:content>
            <div class="d-inline-flex flex-nowrap">
              <v-card v-for="s in almostCompleteSeries" :key="'ac-'+s.id"
                      class="flex-shrink-0 rounded-lg overflow-hidden mx-2 my-2 book-card"
                      flat outlined
                      width="240"
                      :to="{name: s.oneshot ? 'browse-oneshot' : 'browse-series', params: {seriesId: s.id}}"
              >
                <div class="d-flex" style="height: 110px;">
                  <v-img :src="seriesThumbnailUrl(s.id)" width="75" min-width="75" class="flex-shrink-0"/>
                  <div class="pa-3 d-flex flex-column justify-center flex-grow-1" style="min-width: 0;">
                    <div class="text-body-2 font-weight-bold text-truncate">{{ s.metadata.title }}</div>
                    <div class="text-caption text--secondary mt-1">{{ s.booksReadCount }}/{{ s.booksCount }} {{ $t('recommendation.books_unit') }}</div>
                    <v-progress-linear :value="readPercent(s)" height="6" rounded :color="readPercent(s) >= 90 ? '#4CAF50' : '#9C27B0'" class="mt-2"/>
                    <div class="text-caption mt-1" :style="{color: readPercent(s) >= 90 ? '#4CAF50' : '#9C27B0'}">
                      {{ $t('recommendation.remaining', {n: s.booksCount - s.booksReadCount}) }}
                    </div>
                  </div>
                </div>
              </v-card>
            </div>
          </template>
        </horizontal-scroller>
      </template>

      <!-- ========== 5. 许久未读 (Forgotten Reads) ========== -->
      <template v-if="forgottenBooks.length > 0">
        <section-header
          icon="mdi-clock-alert-outline"
          icon-color="#F44336"
          :title="$t('recommendation.forgotten_reads')"
          :subtitle="$t('recommendation.forgotten_reads_desc')"
          :count="forgottenBooks.length"
        />
        <horizontal-scroller class="mb-8" :tick="forgottenTick" @scroll-changed="(p) => scrollChanged(forgottenLoader, p)">
          <template v-slot:prepend><span/></template>
          <template v-slot:content>
            <item-browser
              :items="forgottenBooks"
              :item-context="[ItemContext.SHOW_SERIES, ItemContext.READ_DATE]"
              nowrap
              :fixed-item-width="fixedCardWidth"
              :edit-function="isAdmin ? singleEditBook : undefined"
              :selected.sync="selectedBooks"
              :selectable="selectedSeries.length === 0"
            />
          </template>
        </horizontal-scroller>
      </template>

      <!-- ========== 6. 新入库未读 (Newly Added, Unread) ========== -->
      <template v-if="newUnreadSeries.length > 0">
        <section-header
          icon="mdi-new-box"
          icon-color="#00BCD4"
          :title="$t('recommendation.new_unread')"
          :subtitle="$t('recommendation.new_unread_desc')"
          :count="newUnreadSeries.length"
        />
        <horizontal-scroller class="mb-8" :tick="newUnreadTick" @scroll-changed="(p) => scrollChanged(newUnreadLoader, p)">
          <template v-slot:prepend><span/></template>
          <template v-slot:content>
            <item-browser
              :items="newUnreadSeries"
              :item-context="[ItemContext.DATE_ADDED]"
              nowrap
              :fixed-item-width="fixedCardWidth"
              :edit-function="isAdmin ? singleEditSeries : undefined"
              :selected.sync="selectedSeries"
              :selectable="selectedBooks.length === 0"
            />
          </template>
        </horizontal-scroller>
      </template>

      <!-- ========== 7. 热门标签推荐 (By Genre) ========== -->
      <template v-if="allGenres.length > 0">
        <section-header
          icon="mdi-tag-multiple"
          icon-color="#E91E63"
          :title="$t('recommendation.favorite_genres')"
          :subtitle="$t('recommendation.favorite_genres_desc')"
        />
        <div class="mb-8">
          <div class="d-flex flex-wrap mb-3" style="gap: 8px;">
            <v-chip v-for="genre in allGenres.slice(0, 12)" :key="genre"
                    :color="selectedGenre === genre ? 'primary' : undefined"
                    :outlined="selectedGenre !== genre"
                    small
                    @click="selectGenre(genre)"
            >
              {{ genre }}
            </v-chip>
          </div>
          <horizontal-scroller v-if="genreFilteredSeries.length > 0" :tick="genreFilterTick">
            <template v-slot:prepend><span/></template>
            <template v-slot:content>
              <item-browser
                :items="genreFilteredSeries"
                nowrap
                :fixed-item-width="fixedCardWidth"
                :edit-function="isAdmin ? singleEditSeries : undefined"
                :selected.sync="selectedSeries"
                :selectable="selectedBooks.length === 0"
              />
            </template>
          </horizontal-scroller>
          <div v-else-if="genreLoading" class="text-center py-4">
            <v-progress-circular indeterminate size="24" width="2"/>
          </div>
        </div>
      </template>

      <!-- ========== 8. 同作者探索 (More from Creators) ========== -->
      <template v-if="authorRecommendations.length > 0">
        <section-header
          icon="mdi-account-star"
          icon-color="#673AB7"
          :title="$t('recommendation.favorite_authors')"
          :subtitle="$t('recommendation.favorite_authors_desc')"
        />
        <div class="mb-8">
          <div v-for="author in authorRecommendations" :key="'au-'+author.name" class="mb-5">
            <div class="d-flex align-center mb-2">
              <v-avatar size="28" color="#673AB7" class="mr-2">
                <span class="white--text text-caption font-weight-bold">{{ author.name.charAt(0) }}</span>
              </v-avatar>
              <div>
                <span class="text-body-2 font-weight-bold">{{ author.name }}</span>
                <span class="text-caption text--secondary ml-2">{{ author.items.length }} {{ $t('recommendation.series_unit') }}</span>
              </div>
            </div>
            <horizontal-scroller>
              <template v-slot:prepend><span/></template>
              <template v-slot:content>
                <item-browser
                  :items="author.items"
                  nowrap
                  :fixed-item-width="fixedCardWidthSmall"
                  :edit-function="isAdmin ? singleEditSeries : undefined"
                  :selected.sync="selectedSeries"
                  :selectable="selectedBooks.length === 0"
                />
              </template>
            </horizontal-scroller>
          </div>
        </div>
      </template>

      <!-- ========== 9. 完结可追 (Complete Series to Binge) ========== -->
      <template v-if="completeSeries.length > 0">
        <section-header
          icon="mdi-check-decagram"
          icon-color="#4CAF50"
          :title="$t('recommendation.complete_binge')"
          :subtitle="$t('recommendation.complete_binge_desc')"
          :count="completeSeries.length"
        />
        <horizontal-scroller class="mb-8" :tick="completeTick" @scroll-changed="(p) => scrollChanged(completeLoader, p)">
          <template v-slot:prepend><span/></template>
          <template v-slot:content>
            <item-browser
              :items="completeSeries"
              nowrap
              :fixed-item-width="fixedCardWidth"
              :edit-function="isAdmin ? singleEditSeries : undefined"
              :selected.sync="selectedSeries"
              :selectable="selectedBooks.length === 0"
            />
          </template>
        </horizontal-scroller>
      </template>

      <!-- ========== 10. 随机发现 (Random Discovery) ========== -->
      <template v-if="randomSeries.length > 0">
        <section-header
          icon="mdi-dice-multiple"
          icon-color="#FF5722"
          :title="$t('recommendation.random_discovery')"
          :subtitle="$t('recommendation.random_discovery_desc')"
        />
        <div class="d-flex align-center mb-3">
          <v-btn small outlined color="primary" @click="loadRandomSeries" :loading="randomLoading" class="mr-3">
            <v-icon small left>mdi-refresh</v-icon>
            {{ $t('recommendation.shuffle') }}
          </v-btn>
        </div>
        <horizontal-scroller class="mb-8" :tick="randomTick">
          <template v-slot:prepend><span/></template>
          <template v-slot:content>
            <item-browser
              :items="randomSeries"
              nowrap
              :fixed-item-width="fixedCardWidth"
              :edit-function="isAdmin ? singleEditSeries : undefined"
              :selected.sync="selectedSeries"
              :selectable="selectedBooks.length === 0"
            />
          </template>
        </horizontal-scroller>
      </template>

      <!-- Empty state -->
      <empty-state v-if="allEmpty && !loading"
                   :title="$t('common.nothing_to_show')"
                   icon="mdi-help-circle"
                   icon-color="secondary"
      />
    </v-container>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import MultiSelectBar from '@/components/bars/MultiSelectBar.vue'
import ToolbarSticky from '@/components/bars/ToolbarSticky.vue'
import LibraryActionsMenu from '@/components/menus/LibraryActionsMenu.vue'
import LibraryNavigation from '@/components/LibraryNavigation.vue'
import HorizontalScroller from '@/components/HorizontalScroller.vue'
import ItemBrowser from '@/components/ItemBrowser.vue'
import EmptyState from '@/components/EmptyState.vue'
import {BookDto} from '@/types/komga-books'
import {Oneshot, SeriesDto} from '@/types/komga-series'
import {ReadStatus} from '@/types/enum-books'
import {PageLoader} from '@/types/pageLoader'
import {ItemContext} from '@/types/items'
import {seriesThumbnailUrl} from '@/functions/urls'
import {LIBRARY_ROUTE} from '@/types/library'
import {LibraryDto} from '@/types/komga-libraries'
import {
  BookSearch,
  SearchConditionAllOfBook,
  SearchConditionAllOfSeries,
  SearchConditionAnyOfSeries,
  SearchConditionBook,
  SearchConditionComplete,
  SearchConditionGenre,
  SearchConditionLibraryId,
  SearchConditionReadStatus,
  SearchConditionSeriesId,
  SeriesSearch,
  SearchOperatorIs,
  SearchOperatorIsTrue,
} from '@/types/komga-search'
import {throttle} from 'lodash'
import SectionHeader from '@/components/SectionHeader.vue'

interface GenreRec {
  genre: string,
  items: SeriesDto[],
}

interface AuthorRec {
  name: string,
  items: SeriesDto[],
}

/**
 * Custom search condition for author matching by name+role.
 */
class SearchConditionAuthorMatch {
  author: { operator: string, value: { name: string, role: string } }

  constructor(name: string, role: string) {
    this.author = {operator: 'is', value: {name, role}}
  }
}

export default Vue.extend({
  name: 'RecommendationView',
  components: {SectionHeader, HorizontalScroller, ItemBrowser, EmptyState, ToolbarSticky, LibraryNavigation, LibraryActionsMenu, MultiSelectBar},
  props: {
    libraryId: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      ItemContext,
      loading: false,
      // 1. Keep Reading
      keepReadingLoader: undefined as PageLoader<BookDto> | undefined,
      // 2. On Deck
      onDeckLoader: undefined as PageLoader<BookDto> | undefined,
      // 3. Because You Read
      genreRecommendations: [] as GenreRec[],
      // 4. Almost Complete
      almostCompleteSeries: [] as SeriesDto[],
      // 5. Forgotten Reads
      forgottenLoader: undefined as PageLoader<BookDto> | undefined,
      // 6. New Unread
      newUnreadLoader: undefined as PageLoader<SeriesDto> | undefined,
      // 7. Genre filter
      allGenres: [] as string[],
      selectedGenre: '',
      genreFilteredSeries: [] as SeriesDto[],
      genreLoading: false,
      genreFilterTick: 0,
      // 8. Author recommendations
      authorRecommendations: [] as AuthorRec[],
      // 9. Complete series
      completeLoader: undefined as PageLoader<SeriesDto> | undefined,
      // 10. Random
      randomSeries: [] as SeriesDto[],
      randomLoading: false,
      randomTick: 0,
      // Selection
      selectedSeries: [] as SeriesDto[],
      selectedBooks: [] as BookDto[],
    }
  },
  mounted() {
    this.$store.commit('setLibraryRoute', {
      id: this.libraryId,
      route: LIBRARY_ROUTE.RECOMMENDED,
    })
    this.setupLoaders(this.libraryId)
    this.loadAll()
  },
  watch: {
    libraryId(val) {
      this.$store.commit('setLibraryRoute', {
        id: val,
        route: LIBRARY_ROUTE.RECOMMENDED,
      })
      this.setupLoaders(val)
      this.loadAll()
    },
    '$store.state.komgaLibraries.libraries': {
      handler(val) {
        if (val.length === 0) this.$router.push({name: 'welcome'})
        else this.reload()
      },
    },
  },
  computed: {
    library(): LibraryDto | undefined {
      return this.$store.getters.getLibraryById(this.libraryId)
    },
    requestLibraryIds(): string[] {
      return [this.libraryId]
    },
    isAdmin(): boolean {
      return this.$store.getters.meAdmin
    },
    fixedCardWidth(): number {
      return this.$vuetify.breakpoint.xs ? 120 : 150
    },
    fixedCardWidthSmall(): number {
      return this.$vuetify.breakpoint.xs ? 110 : 130
    },
    keepReadingBooks(): BookDto[] {
      return this.keepReadingLoader?.items || []
    },
    keepReadingTick(): number {
      return this.keepReadingLoader?.tick || 0
    },
    onDeckBooks(): BookDto[] {
      return this.onDeckLoader?.items || []
    },
    onDeckTick(): number {
      return this.onDeckLoader?.tick || 0
    },
    forgottenBooks(): BookDto[] {
      return this.forgottenLoader?.items || []
    },
    forgottenTick(): number {
      return this.forgottenLoader?.tick || 0
    },
    newUnreadSeries(): SeriesDto[] {
      return this.newUnreadLoader?.items || []
    },
    newUnreadTick(): number {
      return this.newUnreadLoader?.tick || 0
    },
    completeSeries(): SeriesDto[] {
      return this.completeLoader?.items || []
    },
    completeTick(): number {
      return this.completeLoader?.tick || 0
    },
    selectedOneshots(): boolean {
      return this.selectedBooks.every(b => b.oneshot)
    },
    allEmpty(): boolean {
      return this.keepReadingBooks.length === 0 &&
        this.onDeckBooks.length === 0 &&
        this.genreRecommendations.length === 0 &&
        this.almostCompleteSeries.length === 0 &&
        this.forgottenBooks.length === 0 &&
        this.newUnreadSeries.length === 0 &&
        this.allGenres.length === 0 &&
        this.authorRecommendations.length === 0 &&
        this.completeSeries.length === 0 &&
        this.randomSeries.length === 0
    },
  },
  methods: {
    seriesThumbnailUrl,

    readPercent(s: SeriesDto): number {
      return s.booksCount > 0 ? Math.round((s.booksReadCount / s.booksCount) * 100) : 0
    },

    async scrollChanged(loader: PageLoader<any> | undefined, percent: number) {
      if (loader && percent > 0.95) await loader.loadNext().catch(e => console.warn(e.message))
    },

    /** Build base book conditions scoped to this library */
    getBaseBookConditions(): SearchConditionBook[] {
      return [
        new SearchConditionAnyOfSeries([
          new SearchConditionLibraryId(new SearchOperatorIs(this.libraryId)),
        ]),
      ]
    },

    /** Build base series conditions scoped to this library */
    getBaseSeriesConditions(): any[] {
      return [
        new SearchConditionLibraryId(new SearchOperatorIs(this.libraryId)),
      ]
    },

    seriesChanged(event: SeriesSseDto) {
      if (event.libraryId === this.libraryId) {
        this.reload()
      }
    },
    bookChanged(event: BookSseDto) {
      if (event.libraryId === this.libraryId) {
        this.reload()
      }
    },
    readProgressChanged(event: ReadProgressSseDto) {
      if (this.keepReadingLoader?.items.some(b => b.id === event.bookId)) this.reload()
      else if (this.onDeckLoader?.items.some(b => b.id === event.bookId)) this.reload()
      else if (this.forgottenLoader?.items.some(b => b.id === event.bookId)) this.reload()
    },
    readProgressSeriesChanged(event: ReadProgressSeriesSseDto) {
      this.reload()
    },
    reload: throttle(function (this: any) {
      this.setupLoaders(this.libraryId)
      this.loadAll()
    }, 5000),

    setupLoaders(libraryId: string) {
      const baseBookConditions = this.getBaseBookConditions()
      const baseSeriesConditions = this.getBaseSeriesConditions()

      // 1. Keep Reading: IN_PROGRESS books sorted by last read date desc
      this.keepReadingLoader = new PageLoader<BookDto>(
        {sort: ['readProgress.readDate,desc']},
        (pageable: PageRequest) => this.$komgaBooks.getBooksList({
          condition: new SearchConditionAllOfBook([...baseBookConditions, new SearchConditionReadStatus(new SearchOperatorIs(ReadStatus.IN_PROGRESS))]),
        } as BookSearch, pageable),
      )

      // 2. On Deck (scoped to library)
      this.onDeckLoader = new PageLoader<BookDto>(
        {},
        (pageable: PageRequest) => this.$komgaBooks.getBooksOnDeck([libraryId], pageable),
      )

      // 5. Forgotten Reads: IN_PROGRESS books sorted by read date ASC (oldest first)
      this.forgottenLoader = new PageLoader<BookDto>(
        {sort: ['readProgress.readDate,asc']},
        (pageable: PageRequest) => this.$komgaBooks.getBooksList({
          condition: new SearchConditionAllOfBook([...baseBookConditions, new SearchConditionReadStatus(new SearchOperatorIs(ReadStatus.IN_PROGRESS))]),
        } as BookSearch, pageable),
      )

      // 6. New Unread series sorted by created date desc
      this.newUnreadLoader = new PageLoader<SeriesDto>(
        {sort: ['createdDate,desc']},
        (pageable: PageRequest) => this.$komgaSeries.getSeriesList({
          condition: new SearchConditionAllOfSeries([...baseSeriesConditions, new SearchConditionReadStatus(new SearchOperatorIs(ReadStatus.UNREAD))]),
        } as SeriesSearch, pageable),
      )

      // 9. Complete series to binge: complete + unread
      this.completeLoader = new PageLoader<SeriesDto>(
        {},
        (pageable: PageRequest) => this.$komgaSeries.getSeriesList({
          condition: new SearchConditionAllOfSeries([
            ...baseSeriesConditions,
            new SearchConditionComplete(new SearchOperatorIsTrue()),
            new SearchConditionReadStatus(new SearchOperatorIs(ReadStatus.UNREAD)),
          ]),
        } as SeriesSearch, pageable),
      )
    },

    async loadAll() {
      this.loading = true
      if (this.library != undefined) document.title = `Komga - ${this.library.name}`
      this.selectedSeries = []
      this.selectedBooks = []

      const promises: Promise<any>[] = []

      // PageLoader-based sections
      promises.push(this.keepReadingLoader!.loadNext().catch(e => console.warn(e.message)))
      promises.push(this.onDeckLoader!.loadNext().catch(e => console.warn(e.message)))
      promises.push(this.forgottenLoader!.loadNext().catch(e => console.warn(e.message)))
      promises.push(this.newUnreadLoader!.loadNext().catch(e => console.warn(e.message)))
      promises.push(this.completeLoader!.loadNext().catch(e => console.warn(e.message)))

      // Custom sections
      promises.push(this.loadAlmostComplete())
      promises.push(this.loadGenreRecommendations())
      promises.push(this.loadGenres())
      promises.push(this.loadAuthorRecommendations())
      promises.push(this.loadRandomSeries())

      await Promise.allSettled(promises)
      this.loading = false
    },

    // 3. Because You Read - genre-based recommendations (scoped to library)
    async loadGenreRecommendations() {
      try {
        const baseSeriesConditions = this.getBaseSeriesConditions()

        // Get recently read series from this library to extract genres
        const readSeriesPage = await this.$komgaSeries.getSeriesList({
          condition: new SearchConditionAllOfSeries([...baseSeriesConditions, new SearchConditionReadStatus(new SearchOperatorIs(ReadStatus.READ))]),
        } as SeriesSearch, {size: 20, sort: ['lastModified,desc']} as PageRequest)

        // Collect unique genres from read series
        const genreCounts = new Map<string, number>()
        for (const s of readSeriesPage.content) {
          for (const g of (s.metadata.genres || [])) {
            genreCounts.set(g, (genreCounts.get(g) || 0) + 1)
          }
        }

        // Sort by frequency, take top 3
        const topGenres = [...genreCounts.entries()]
          .sort((a, b) => b[1] - a[1])
          .slice(0, 3)
          .map(([g]) => g)

        // For each top genre, find unread series in this library
        const recs: GenreRec[] = []
        for (const genre of topGenres) {
          const unreadPage = await this.$komgaSeries.getSeriesList({
            condition: new SearchConditionAllOfSeries([
              ...baseSeriesConditions,
              new SearchConditionGenre(new SearchOperatorIs(genre)),
              new SearchConditionReadStatus(new SearchOperatorIs(ReadStatus.UNREAD)),
            ]),
          } as SeriesSearch, {size: 20} as PageRequest)

          if (unreadPage.content.length > 0) {
            recs.push({genre, items: unreadPage.content})
          }
        }
        this.genreRecommendations = recs
      } catch (e) {
        console.warn('Failed to load genre recommendations', e)
      }
    },

    // 4. Almost Complete Series (scoped to library)
    async loadAlmostComplete() {
      try {
        const baseSeriesConditions = this.getBaseSeriesConditions()
        const page = await this.$komgaSeries.getSeriesList({
          condition: new SearchConditionAllOfSeries([...baseSeriesConditions, new SearchConditionReadStatus(new SearchOperatorIs(ReadStatus.IN_PROGRESS))]),
        } as SeriesSearch, {size: 100} as PageRequest)

        // Filter: 70%+ read but not fully read
        this.almostCompleteSeries = page.content
          .filter(s => s.booksCount > 0 && (s.booksReadCount / s.booksCount) >= 0.7 && s.booksReadCount < s.booksCount)
          .sort((a, b) => (b.booksReadCount / b.booksCount) - (a.booksReadCount / a.booksCount))
      } catch (e) {
        console.warn('Failed to load almost complete series', e)
      }
    },

    // 7. Load all genres for filter (scoped to library)
    async loadGenres() {
      try {
        this.allGenres = await this.$komgaReferential.getGenres([this.libraryId])
        if (this.allGenres.length > 0) {
          this.selectedGenre = this.allGenres[0]
          await this.loadGenreFiltered()
        }
      } catch (e) {
        console.warn('Failed to load genres', e)
      }
    },

    async selectGenre(genre: string) {
      this.selectedGenre = genre
      await this.loadGenreFiltered()
    },

    async loadGenreFiltered() {
      this.genreLoading = true
      try {
        const baseSeriesConditions = this.getBaseSeriesConditions()
        const page = await this.$komgaSeries.getSeriesList({
          condition: new SearchConditionAllOfSeries([
            ...baseSeriesConditions,
            new SearchConditionGenre(new SearchOperatorIs(this.selectedGenre)),
            new SearchConditionReadStatus(new SearchOperatorIs(ReadStatus.UNREAD)),
          ]),
        } as SeriesSearch, {size: 20} as PageRequest)
        this.genreFilteredSeries = page.content
        this.genreFilterTick++
      } catch (e) {
        console.warn('Failed to load genre filtered series', e)
      } finally {
        this.genreLoading = false
      }
    },

    // 8. Author recommendations (scoped to library)
    async loadAuthorRecommendations() {
      try {
        const baseSeriesConditions = this.getBaseSeriesConditions()

        // Get recently read series from this library
        const readSeriesPage = await this.$komgaSeries.getSeriesList({
          condition: new SearchConditionAllOfSeries([...baseSeriesConditions, new SearchConditionReadStatus(new SearchOperatorIs(ReadStatus.READ))]),
        } as SeriesSearch, {size: 20, sort: ['lastModified,desc']} as PageRequest)

        // Collect unique authors from read series (writers and pencillers)
        const authorSet = new Map<string, { name: string, role: string }>()
        for (const s of readSeriesPage.content) {
          for (const a of (s.booksMetadata.authors || [])) {
            if (a.role === 'writer' || a.role === 'penciller') {
              const key = `${a.name}|${a.role}`
              if (!authorSet.has(key)) {
                authorSet.set(key, a)
              }
            }
          }
        }

        // Take top 3 unique authors
        const authors = [...authorSet.values()].slice(0, 3)

        const recs: AuthorRec[] = []
        for (const author of authors) {
          try {
            const page = await this.$komgaSeries.getSeriesList({
              condition: new SearchConditionAllOfSeries([
                ...this.getBaseSeriesConditions(),
                new SearchConditionAuthorMatch(author.name, author.role) as any,
              ]),
            } as SeriesSearch, {size: 20} as PageRequest)

            if (page.content.length > 0) {
              recs.push({name: author.name, items: page.content})
            }
          } catch (_) {
            // Skip this author if search fails
          }
        }
        this.authorRecommendations = recs
      } catch (e) {
        console.warn('Failed to load author recommendations', e)
      }
    },

    // 10. Random discovery (scoped to library)
    async loadRandomSeries() {
      this.randomLoading = true
      try {
        const baseSeriesConditions = this.getBaseSeriesConditions()

        // First get total count of unread series in this library
        const countPage = await this.$komgaSeries.getSeriesList({
          condition: new SearchConditionAllOfSeries([...baseSeriesConditions, new SearchConditionReadStatus(new SearchOperatorIs(ReadStatus.UNREAD))]),
        } as SeriesSearch, {size: 1} as PageRequest)

        if (countPage.totalElements > 0) {
          const totalPages = Math.max(1, Math.ceil(countPage.totalElements / 20))
          const randomPage = Math.floor(Math.random() * totalPages)

          const page = await this.$komgaSeries.getSeriesList({
            condition: new SearchConditionAllOfSeries([...baseSeriesConditions, new SearchConditionReadStatus(new SearchOperatorIs(ReadStatus.UNREAD))]),
          } as SeriesSearch, {size: 20, page: randomPage} as PageRequest)

          // Shuffle the results client-side
          this.randomSeries = this.shuffle(page.content).slice(0, 8)
          this.randomTick++
        }
      } catch (e) {
        console.warn('Failed to load random series', e)
      } finally {
        this.randomLoading = false
      }
    },

    shuffle<T>(array: T[]): T[] {
      const a = [...array]
      for (let i = a.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [a[i], a[j]] = [a[j], a[i]]
      }
      return a
    },

    // Selection action methods (same pattern as DashboardView)
    async singleEditSeries(series: SeriesDto) {
      if (series.oneshot) {
        let book = (await this.$komgaBooks.getBooksList({
          condition: new SearchConditionSeriesId(new SearchOperatorIs(series.id)),
        } as BookSearch)).content[0]
        this.$store.dispatch('dialogUpdateOneshots', {series: series, book: book})
      } else
        this.$store.dispatch('dialogUpdateSeries', series)
    },
    async singleEditBook(book: BookDto) {
      if (book.oneshot) {
        const series = (await this.$komgaSeries.getOneSeries(book.seriesId))
        this.$store.dispatch('dialogUpdateOneshots', {series: series, book: book})
      } else
        this.$store.dispatch('dialogUpdateBooks', book)
    },
    async markSelectedSeriesRead() {
      await Promise.all(this.selectedSeries.map(s =>
        this.$komgaSeries.markAsRead(s.id),
      ))
      this.selectedSeries = []
    },
    async markSelectedSeriesUnread() {
      await Promise.all(this.selectedSeries.map(s =>
        this.$komgaSeries.markAsUnread(s.id),
      ))
      this.selectedSeries = []
    },
    addToCollection() {
      this.$store.dispatch('dialogAddSeriesToCollection', this.selectedSeries.map(s => s.id))
      this.selectedSeries = []
    },
    async editMultipleSeries() {
      if (this.selectedSeries.every(s => s.oneshot)) {
        const books = await Promise.all(this.selectedSeries.map(s => this.$komgaBooks.getBooksList({
          condition: new SearchConditionSeriesId(new SearchOperatorIs(s.id)),
        } as BookSearch)))
        const oneshots = this.selectedSeries.map((s, index) => ({series: s, book: books[index].content[0]} as Oneshot))
        this.$store.dispatch('dialogUpdateOneshots', oneshots)
      } else
        this.$store.dispatch('dialogUpdateSeries', this.selectedSeries)
    },
    async editMultipleBooks() {
      if (this.selectedBooks.every(b => b.oneshot)) {
        const series = await Promise.all(this.selectedBooks.map(b => this.$komgaSeries.getOneSeries(b.seriesId)))
        const oneshots = this.selectedBooks.map((b, index) => ({series: series[index], book: b} as Oneshot))
        this.$store.dispatch('dialogUpdateOneshots', oneshots)
      } else
        this.$store.dispatch('dialogUpdateBooks', this.selectedBooks)
    },
    async markSelectedBooksRead() {
      await Promise.all(this.selectedBooks.map(b =>
        this.$komgaBooks.updateReadProgress(b.id, {completed: true}),
      ))
      this.selectedBooks = []
    },
    async markSelectedBooksUnread() {
      await Promise.all(this.selectedBooks.map(b =>
        this.$komgaBooks.deleteReadProgress(b.id),
      ))
      this.selectedBooks = []
    },
    addToReadList() {
      this.$store.dispatch('dialogAddBooksToReadList', this.selectedBooks.map(b => b.id))
      this.selectedBooks = []
    },
  },
})
</script>

<style scoped>
.book-card {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.book-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15) !important;
}
</style>
