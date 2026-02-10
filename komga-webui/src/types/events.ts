import {Route} from 'vue-router'

// Internal events (used by eventHub for internal communication, not SSE)
export const ERROR = 'error'
export const NOTIFICATION = 'notification'

export interface ErrorEvent {
  message: string,
}

export interface NotificationEvent {
  message: string,
  text2?: string,
  goTo?: {
    text: string,
    click: () => Promise<Route>
  }
}
