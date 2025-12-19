/**
 * 环境配置管理
 * 统一管理所有环境变量和配置
 */

// 获取环境变量，提供默认值
const getEnvVar = (key, defaultValue = '') => {
    return import.meta.env[key] || defaultValue
}

// 应用配置
export const APP_CONFIG = {
    // 应用标题
    TITLE: getEnvVar('VITE_APP_TITLE', 'HyFreeDom'),

    // 当前环境
    ENV: getEnvVar('VITE_APP_ENV', 'development'),

    // 是否为开发环境
    IS_DEV: getEnvVar('VITE_APP_ENV', 'development') === 'development',

    // 是否为生产环境
    IS_PROD: getEnvVar('VITE_APP_ENV', 'development') === 'production',
}

// API 配置
export const API_CONFIG = {
    // API 基础 URL - 开发环境使用相对路径利用 Vite 代理
    BASE_URL: APP_CONFIG.IS_DEV ? '' : getEnvVar('VITE_API_BASE_URL', 'http://localhost:8080'),

    // API 超时时间（毫秒）
    TIMEOUT: 10000,

    // 请求头配置
    HEADERS: {
        'Content-Type': 'application/json',
    }
}

// 存储配置
export const STORAGE_CONFIG = {
    // Token 存储键名
    TOKEN_KEY: 'admin_token',
    USER_TOKEN_KEY: 'user_token',

    // 用户信息存储键名
    ADMIN_ROLE_KEY: 'admin_role',
    ADMIN_USERNAME_KEY: 'admin_username',
    USER_INFO_KEY: 'user_info',

    // 其他存储键名
    REMEMBER_ME_KEY: 'remember_me',
    REMEMBERED_USERNAME_KEY: 'remembered_username',
    THEME_KEY: 'theme',
}

// 路由配置
export const ROUTE_CONFIG = {
    // 默认重定向路径
    DEFAULT_REDIRECT: '/user/login',

    // 管理员默认路径
    ADMIN_DEFAULT_REDIRECT: '/admin/dashboard',

    // 用户默认路径
    USER_DEFAULT_REDIRECT: '/user/dashboard',
}

// 错误码配置
export const ERROR_CODES = {
    SUCCESS: 200,
    PARAM_ERROR: 40001,
    AUTH_ERROR: 40101,
    PERMISSION_ERROR: 40301,
    CONFLICT_ERROR: 40901,
    RATE_LIMIT_ERROR: 42901,
    SERVER_ERROR: 50000,
}

// 错误消息配置
export const ERROR_MESSAGES = {
    [ERROR_CODES.PARAM_ERROR]: '参数校验失败',
    [ERROR_CODES.AUTH_ERROR]: '认证失败，请重新登录',
    [ERROR_CODES.PERMISSION_ERROR]: '权限不足',
    [ERROR_CODES.CONFLICT_ERROR]: '资源冲突',
    [ERROR_CODES.RATE_LIMIT_ERROR]: '请求过于频繁，请稍后再试',
    [ERROR_CODES.SERVER_ERROR]: '服务器内部错误',
    NETWORK_ERROR: '网络错误，请检查网络连接',
    UNKNOWN_ERROR: '未知错误，请稍后重试',
}

// 开发环境调试配置
export const DEBUG_CONFIG = {
    // 是否启用调试日志
    ENABLE_LOG: APP_CONFIG.IS_DEV,

    // 是否显示 API 请求详情
    SHOW_API_DETAILS: APP_CONFIG.IS_DEV,

    // 是否启用 Vue DevTools
    ENABLE_DEVTOOLS: APP_CONFIG.IS_DEV,
}

// 导出所有配置
export default {
    API_CONFIG,
    APP_CONFIG,
    STORAGE_CONFIG,
    ROUTE_CONFIG,
    ERROR_CODES,
    ERROR_MESSAGES,
    DEBUG_CONFIG,
}
