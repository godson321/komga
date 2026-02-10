import {AxiosInstance} from 'axios'

const API_TASKS = '/api/v1/tasks'

export interface TaskCountDto {
  count: number
  countByType: { [key: string]: number }
}

export default class KomgaTasksService {
  private http: AxiosInstance

  constructor(http: AxiosInstance) {
    this.http = http
  }

  async getTaskCount(): Promise<TaskCountDto> {
    const response = await this.http.get(`${API_TASKS}/count`)
    return response.data
  }

  async deleteAllTasks(): Promise<number> {
    try {
      return (await this.http.delete(API_TASKS)).data
    } catch (e) {
      let msg = 'An error occurred while trying to delete all tasks'
      if (e.response?.data?.message) {
        msg += `: ${e.response?.data?.message}`
      }
      throw new Error(msg)
    }
  }
}
