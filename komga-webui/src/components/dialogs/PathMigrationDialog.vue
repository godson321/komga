<template>
  <v-dialog v-model="modal" max-width="600">
    <v-card>
      <v-card-title>{{ $t('dialog.migrate_path.title') }}</v-card-title>
      <v-card-text>
        <v-container>
          <v-row>
            <v-col>
              <p>{{ $t('dialog.migrate_path.description') }}</p>
            </v-col>
          </v-row>
          <v-row>
            <v-col>
              <v-text-field
                v-model="oldPathPrefix"
                :label="$t('dialog.migrate_path.old_path')"
                :placeholder="'W:\\14 漫画\\未完结'"
                outlined
                dense
                :disabled="loading"
              />
            </v-col>
          </v-row>
          <v-row>
            <v-col>
              <v-text-field
                v-model="newPathPrefix"
                :label="$t('dialog.migrate_path.new_path')"
                :placeholder="'Z:\\14 漫画\\未完结'"
                outlined
                dense
                :disabled="loading"
              />
            </v-col>
          </v-row>
          <v-row v-if="result">
            <v-col>
              <v-alert type="success" dense>
                {{ $t('dialog.migrate_path.result', {
                  series: result.seriesUpdated,
                  books: result.booksUpdated,
                  sidecars: result.sidecarsUpdated
                }) }}
              </v-alert>
            </v-col>
          </v-row>
        </v-container>
      </v-card-text>
      <v-card-actions>
        <v-spacer/>
        <v-btn text @click="modal = false" :disabled="loading">{{ $t('common.cancel') }}</v-btn>
        <v-btn
          color="primary"
          @click="migrate"
          :disabled="!canMigrate"
          :loading="loading"
        >
          {{ $t('dialog.migrate_path.button_confirm') }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script lang="ts">
import Vue from 'vue'
import {LibraryDto} from '@/types/komga-libraries'
import {PathMigrationResultDto} from '@/services/komga-libraries.service'

export default Vue.extend({
  name: 'PathMigrationDialog',
  props: {
    value: Boolean,
    library: {
      type: Object as () => LibraryDto,
      required: true,
    },
  },
  data: () => ({
    oldPathPrefix: '',
    newPathPrefix: '',
    loading: false,
    result: null as PathMigrationResultDto | null,
  }),
  computed: {
    modal: {
      get(): boolean {
        return this.value
      },
      set(val: boolean) {
        this.$emit('input', val)
      },
    },
    canMigrate(): boolean {
      return this.oldPathPrefix.trim() !== '' && this.newPathPrefix.trim() !== '' && !this.loading
    },
  },
  watch: {
    value(val) {
      if (val) {
        this.oldPathPrefix = ''
        this.newPathPrefix = ''
        this.result = null
      }
    },
  },
  methods: {
    async migrate() {
      this.loading = true
      this.result = null
      try {
        this.result = await this.$komgaLibraries.migratePath(
          this.library,
          this.oldPathPrefix,
          this.newPathPrefix,
        )
        this.$emit('migrated', this.result)
      } catch (e) {
        this.$eventHub.$emit('showSnackbar', {
          message: e.message,
          color: 'error',
        })
      } finally {
        this.loading = false
      }
    },
  },
})
</script>
