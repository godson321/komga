import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import KomgaTasksService from '@/services/komga-tasks.service'
import {Module} from 'vuex'

const POLL_INTERVAL = 30_000 // 30 seconds

const vuexModule: Module<any, any> = {
  state: {
    taskCount: 0,
    taskCountByType: {} as { [key: string]: number },
  },
  mutations: {
    setTaskCount(state, {count, countByType}: { count: number, countByType: { [key: string]: number } }) {
      state.taskCount = count
      state.taskCountByType = countByType
    },
  },
}

export default {
  install(
    Vue: typeof _Vue,
    {http, store}: { http: AxiosInstance, store: any }) {
    const tasksService = new KomgaTasksService(http)
    Vue.prototype.$komgaTasks = tasksService

    store.registerModule('komgaTasks', vuexModule)

    // Poll task queue every 30 seconds (admin only)
    let pollInterval: number | null = null

    const pollTaskQueue = async () => {
      if (store.getters.meAdmin) {
        try {
          const data = await tasksService.getTaskCount()
          store.commit('setTaskCount', data)
        } catch (e) {
          // Ignore errors during polling
        }
      }
    }

    // Watch authentication state to start/stop polling
    store.watch(
      (state: any, getters: any) => getters.authenticated,
      (authenticated: boolean) => {
        if (authenticated) {
          pollTaskQueue() // Poll immediately on login
          pollInterval = window.setInterval(pollTaskQueue, POLL_INTERVAL)
        } else if (pollInterval) {
          clearInterval(pollInterval)
          pollInterval = null
        }
      },
    )
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $komgaTasks: KomgaTasksService;
  }
}
