import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

/** 统一响应结构 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

/** 创建 Axios 实例 */
const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 120000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ===== 请求拦截器：自动附加 JWT Token =====
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// ===== 响应拦截器：统一错误处理 =====
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    // 业务成功
    if (res.code === 200) {
      return response
    }

    // Token 过期 / 未认证
    if (res.code === 401 || res.code === 1005 || res.code === 1006) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
      return Promise.reject(new Error(res.message))
    }

    // 其他业务错误
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(new Error(res.message))
  },
  (error) => {
    // 网络层错误
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        ElMessage.error('未登录或登录已过期')
        router.push('/login')
      } else if (status === 403) {
        ElMessage.error('无访问权限')
      } else if (status === 404) {
        ElMessage.error('请求的资源不存在')
      } else if (status >= 500) {
        ElMessage.error('服务器内部错误，请稍后重试')
      } else {
        ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络连接')
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  },
)

/**
 * 封装 GET 请求
 */
export function get<T = unknown>(url: string, params?: object, config?: AxiosRequestConfig) {
  return service.get<ApiResponse<T>>(url, { params, ...config }).then((res) => res.data)
}

/**
 * 封装 POST 请求
 */
export function post<T = unknown>(url: string, data?: object, config?: AxiosRequestConfig) {
  return service.post<ApiResponse<T>>(url, data, config).then((res) => res.data)
}

/**
 * 封装 PUT 请求
 */
export function put<T = unknown>(url: string, data?: object, config?: AxiosRequestConfig) {
  return service.put<ApiResponse<T>>(url, data, config).then((res) => res.data)
}

/**
 * 封装 DELETE 请求
 */
export function del<T = unknown>(url: string, params?: object, config?: AxiosRequestConfig) {
  return service.delete<ApiResponse<T>>(url, { params, ...config }).then((res) => res.data)
}

/**
 * 封装文件上传请求
 */
export function upload<T = unknown>(url: string, formData: FormData, config?: AxiosRequestConfig) {
  return service
    .post<ApiResponse<T>>(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      ...config,
    })
    .then((res) => res.data)
}

export default service
