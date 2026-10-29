import axios from 'axios'
import { message } from 'ant-design-vue'

/**
 * 电磁仿真接口（后台转发至 Python 仿真服务）。
 *
 * 注意：仿真由独立的 Python 服务计算（qigger/python-emulation），
 * 启发式优化算法（GA / PSO / SA / Tabu）耗时较长，故这里单独放宽超时时间，
 * 避免使用全局 request.ts 的 10s 超时。
 */
const emulationApiClient = axios.create({
  baseURL: 'http://localhost:8080/api/emulation',
  timeout: 180000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
})

emulationApiClient.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && res.code !== 0) {
      message.error(res.description || '仿真失败')
      return Promise.reject(new Error(res.description || '仿真失败'))
    }
    return res
  },
  (error) => {
    const errorMsg = error.response?.data?.description || error.message || '仿真请求失败'
    message.error(errorMsg)
    return Promise.reject(error)
  }
)

export interface EmulationResponse {
  code: number
  data: any
  description: string
}

/** 仿真类型定义 */
export interface EmulationOption {
  value: string
  label: string
  desc: string
}

export const emulationOptions: EmulationOption[] = [
  { value: 'doublet', label: '双极子天线方向图', desc: 'GA / PSO / SA / Tabu 算法，支持单/偶阵元' },
  { value: 'monopole', label: '单极子天线', desc: '旋转方向图仿真' },
  { value: 'ybfs', label: '入射波 / 反射', desc: '入射波反射仿真' },
  { value: 'qzs', label: '全折射', desc: '返回两个结果值' },
  { value: 'qfs', label: '全反射', desc: '全反射系数仿真' },
  { value: 'gs', label: '高斯波束', desc: '高斯波束仿真' },
  { value: 'zxb', label: '正弦 EH 场', desc: '正弦电场磁场仿真' },
  { value: 'emulation', label: '综合仿真', desc: '同一参数下 GA / PSO / SA / Tabu 对比' },
]

export const emulationApi = {
  /** 双极子天线方向图 */
  doublet: (params: { nu: string; value1: string; value2?: string; state: string }): Promise<EmulationResponse> =>
    emulationApiClient.get('/doublet', { params }),

  /** 单极子天线 */
  monopole: (params: { nu: string; value1: string; value2?: string }): Promise<EmulationResponse> =>
    emulationApiClient.get('/monopole', { params }),

  /** 综合仿真：同一参数下 GA / PSO / SA / Tabu 对比 */
  emulation: (params: { nu: string; value1: string; kind?: string }): Promise<EmulationResponse> =>
    emulationApiClient.get('/emulation', { params }),

  /** 入射波 / 反射 */
  ybfs: (params: { nu: string; value1: string; value2?: string }): Promise<EmulationResponse> =>
    emulationApiClient.get('/ybfs', { params }),

  /** 全折射 */
  qzs: (params: { nu: string; value1: string }): Promise<EmulationResponse> =>
    emulationApiClient.get('/qzs', { params }),

  /** 全反射 */
  qfs: (params: { nu: string }): Promise<EmulationResponse> =>
    emulationApiClient.get('/qfs', { params }),

  /** 高斯波束 */
  gs: (params: { nu: string; value1: string; value2?: string }): Promise<EmulationResponse> =>
    emulationApiClient.get('/gs', { params }),

  /** 正弦 EH 场 */
  zxb: (params: { value1: string; value2: string }): Promise<EmulationResponse> =>
    emulationApiClient.get('/zxb', { params }),
}

export default emulationApiClient
