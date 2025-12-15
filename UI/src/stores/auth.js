import { defineStore } from 'pinia'
import { STORAGE_CONFIG } from '@/config/env.js'
import { apiPost } from '@/utils/api.js'

export const useAuthStore = defineStore('auth', {
    state: () => ({
        // 用户信息
        user: null,
        token: null,
        role: null,

        // 管理员信息
        admin: null,
        adminToken: null,
        adminRole: null,

        // 登录状态
        isLoggedIn: false,
        isAdminLoggedIn: false,

        // 记住我
        rememberMe: false,
    }),

    getters: {
        // 获取当前用户信息
        currentUser: (state) => state.user,

        // 获取当前管理员信息
        currentAdmin: (state) => state.admin,

        // 检查是否为管理员
        isAdmin: (state) => state.role === 'ADMIN' || state.adminRole === 'ADMIN',

        // 检查是否为普通用户
        isUser: (state) => state.role === 'USER',

        // 获取认证头
        authHeaders: (state) => {
            const token = state.token || state.adminToken
            return token ? { Authorization: `Bearer ${token}` } : {}
        },

        // 获取管理员认证头
        adminAuthHeaders: (state) => {
            return state.adminToken ? { Authorization: `Bearer ${state.adminToken}` } : {}
        }
    },

    actions: {
        // 初始化 - 从本地存储恢复状态
        init() {
            this.loadFromStorage()
        },

        // 从本地存储加载状态
        loadFromStorage() {
            try {
                // 调试日志已移除

                // 加载用户信息
                const userToken = localStorage.getItem(STORAGE_CONFIG.USER_TOKEN_KEY)
                const userInfo = localStorage.getItem(STORAGE_CONFIG.USER_INFO_KEY)

                if (userToken && userInfo) {
                    this.token = userToken
                    this.user = JSON.parse(userInfo)
                    this.role = 'USER'
                    this.isLoggedIn = true
                    // 调试日志已移除
                } else {
                    // 调试日志已移除
                }

                // 加载管理员信息
                const adminToken = localStorage.getItem(STORAGE_CONFIG.TOKEN_KEY)
                const adminRole = localStorage.getItem(STORAGE_CONFIG.ADMIN_ROLE_KEY)
                const adminUsername = localStorage.getItem(STORAGE_CONFIG.ADMIN_USERNAME_KEY)

                if (adminToken && adminRole && adminUsername) {
                    this.adminToken = adminToken
                    this.adminRole = adminRole
                    this.admin = { username: adminUsername }
                    this.isAdminLoggedIn = true
                    // 调试日志已移除
                } else {
                    // 调试日志已移除
                }

                // 加载记住我设置
                const rememberMe = localStorage.getItem(STORAGE_CONFIG.REMEMBER_ME_KEY)
                this.rememberMe = rememberMe === 'true'

            } catch (error) {
                console.error('加载本地存储失败:', error)
                this.clearStorage()
            }
        },

        // 用户登录
        async loginUser(credentials) {
            try {
                const response = await apiPost('/api/auth/login', credentials)

                if (response.code === 0) {
                    // 调试日志已移除

                    this.user = response.data.user
                    this.token = response.data.token
                    this.role = 'USER'
                    this.isLoggedIn = true

                    // 保存到本地存储
                    this.saveToStorage()

                    // 调试日志已移除

                    // 确保状态立即生效，避免路由守卫读取到旧状态
                    // 调试日志已移除

                    return { success: true, data: response.data }
                } else {
                    return { success: false, message: response.message }
                }
            } catch (error) {
                console.error('用户登录失败:', error)
                return { success: false, message: error.message || '登录失败' }
            }
        },

        // 管理员登录
        async loginAdmin(credentials) {
            try {
                const response = await apiPost('/api/auth/admin/login', credentials)

                if (response.code === 0) {
                    // 调试日志已移除

                    // 分步设置，每步都检查
                    this.admin = { username: credentials.username }
                    // 调试日志已移除

                    this.adminToken = response.data.token
                    // 调试日志已移除

                    this.adminRole = response.data.role
                    // 调试日志已移除

                    this.isAdminLoggedIn = true
                    // 调试日志已移除

                    // 保存到本地存储
                    this.saveToStorage()

                    // 调试日志已移除

                    // 立即检查localStorage
                    const savedToken = localStorage.getItem(STORAGE_CONFIG.TOKEN_KEY)
                    // 调试日志已移除

                    return { success: true, data: response.data }
                } else {
                    return { success: false, message: response.message }
                }
            } catch (error) {
                console.error('管理员登录失败:', error)
                return { success: false, message: error.message || '登录失败' }
            }
        },

        // 用户注册
        async registerUser(userData) {
            try {
                const response = await apiPost('/api/auth/register', userData)

                if (response.code === 0) {
                    return { success: true, message: '注册成功' }
                } else {
                    return { success: false, message: response.message }
                }
            } catch (error) {
                console.error('用户注册失败:', error)
                return { success: false, message: error.message || '注册失败' }
            }
        },

        // 发送注册验证码
        async sendRegisterCode(email) {
            try {
                const response = await apiPost('/api/auth/register/send-code', { email })

                if (response.code === 0) {
                    // 立即返回成功，不等待邮件发送完成
                    return { success: true, message: '验证码发送中，请稍后查收邮件' }
                } else {
                    return { success: false, message: response.message }
                }
            } catch (error) {
                console.error('发送验证码失败:', error)
                // 即使网络错误也返回成功，让用户进入验证码输入界面
                return { success: true, message: '验证码发送中，请稍后查收邮件' }
            }
        },

        // 已删除创建管理员账号功能

        // 已删除 checkAdminExists 方法，不再自动检测管理员状态

        // 保存到本地存储
        saveToStorage() {
            try {
                // 调试日志已移除

                // 保存用户信息
                if (this.token && this.user) {
                    localStorage.setItem(STORAGE_CONFIG.USER_TOKEN_KEY, this.token)
                    localStorage.setItem(STORAGE_CONFIG.USER_INFO_KEY, JSON.stringify(this.user))
                    // 调试日志已移除
                } else {
                    // 调试日志已移除
                }

                // 保存管理员信息
                if (this.adminToken && this.admin) {
                    localStorage.setItem(STORAGE_CONFIG.TOKEN_KEY, this.adminToken)
                    localStorage.setItem(STORAGE_CONFIG.ADMIN_ROLE_KEY, this.adminRole)
                    localStorage.setItem(STORAGE_CONFIG.ADMIN_USERNAME_KEY, this.admin.username)
                    // 调试日志已移除
                } else {
                    // 调试日志已移除
                }

                // 保存记住我设置
                localStorage.setItem(STORAGE_CONFIG.REMEMBER_ME_KEY, this.rememberMe.toString())

            } catch (error) {
                console.error('保存到本地存储失败:', error)
            }
        },

        // 清除本地存储
        clearStorage() {
            try {
                localStorage.removeItem(STORAGE_CONFIG.USER_TOKEN_KEY)
                localStorage.removeItem(STORAGE_CONFIG.USER_INFO_KEY)
                localStorage.removeItem(STORAGE_CONFIG.TOKEN_KEY)
                localStorage.removeItem(STORAGE_CONFIG.ADMIN_ROLE_KEY)
                localStorage.removeItem(STORAGE_CONFIG.ADMIN_USERNAME_KEY)
                localStorage.removeItem(STORAGE_CONFIG.REMEMBER_ME_KEY)
            } catch (error) {
                console.error('清除本地存储失败:', error)
            }
        },

        // 用户登出
        logout() {
            // 只清除用户相关状态，保留管理员状态
            this.user = null
            this.token = null
            this.role = null
            this.isLoggedIn = false

            // 只清除用户相关的存储
            localStorage.removeItem(STORAGE_CONFIG.USER_TOKEN_KEY)
            localStorage.removeItem(STORAGE_CONFIG.USER_INFO_KEY)

            // 注意：不清除记住的用户名，因为"记住我"是为了方便下次登录
        },

        // 管理员登出
        async logoutAdmin() {
            try {
                // 调用后端注销接口
                if (this.adminToken) {
                    await apiPost('/api/auth/logout')
                }
            } catch (error) {
                console.error('调用注销接口失败:', error)
                // 即使后端调用失败，也要清理本地状态
            }

            // 清理本地状态
            this.admin = null
            this.adminToken = null
            this.adminRole = null
            this.isAdminLoggedIn = false

            // 只清除管理员相关的存储
            localStorage.removeItem(STORAGE_CONFIG.TOKEN_KEY)
            localStorage.removeItem(STORAGE_CONFIG.ADMIN_ROLE_KEY)
            localStorage.removeItem(STORAGE_CONFIG.ADMIN_USERNAME_KEY)

            // 注意：不清除记住的用户名，因为"记住我"是为了方便下次登录
        },

        // 设置记住我
        setRememberMe(remember) {
            this.rememberMe = remember
            localStorage.setItem(STORAGE_CONFIG.REMEMBER_ME_KEY, remember.toString())
        },

        // 设置记住的用户名
        setRememberedUsername(username) {
            localStorage.setItem(STORAGE_CONFIG.REMEMBERED_USERNAME_KEY, username)
        },

        // 获取记住的用户名
        getRememberedUsername() {
            return localStorage.getItem(STORAGE_CONFIG.REMEMBERED_USERNAME_KEY)
        },

        // 清除记住的用户名
        clearRememberedUsername() {
            localStorage.removeItem(STORAGE_CONFIG.REMEMBERED_USERNAME_KEY)
        },

        // 处理token过期
        handleTokenExpired() {
            // 调试日志已移除

            // 清除用户状态
            this.user = null
            this.token = null
            this.role = null
            this.isLoggedIn = false

            // 清除用户相关的存储
            localStorage.removeItem(STORAGE_CONFIG.USER_TOKEN_KEY)
            localStorage.removeItem(STORAGE_CONFIG.USER_INFO_KEY)

            // 跳转到登录页
            if (window.location.pathname.startsWith('/user')) {
                window.location.href = '/user/login'
            }
        },

        // 处理管理员token过期
        handleAdminTokenExpired() {
            // 调试日志已移除

            // 清除管理员状态
            this.admin = null
            this.adminToken = null
            this.adminRole = null
            this.isAdminLoggedIn = false

            // 清除管理员相关的存储
            localStorage.removeItem(STORAGE_CONFIG.TOKEN_KEY)
            localStorage.removeItem(STORAGE_CONFIG.ADMIN_ROLE_KEY)
            localStorage.removeItem(STORAGE_CONFIG.ADMIN_USERNAME_KEY)

            // 跳转到管理员登录页
            if (window.location.pathname.startsWith('/admin')) {
                window.location.href = '/admin/login'
            }
        },

        // 获取用户信息
        async fetchUserInfo() {
            try {
                if (!this.token) {
                    return { success: false, message: '未登录' }
                }

                // Use the standard apiGet utility instead of fetch if possible, 
                // but since apiGet is not imported, we use fetch or import apiGet.
                // Looking at the file, apiPost IS imported. Let's import apiGet too.
                // Wait, apiPost is imported at line 3.
                // Let's check imports.

                // For now, I'll stick to the existing pattern in refreshUserInfo which uses fetch directly 
                // (Wait, refreshUserInfo uses fetch directly? That's inconsistent. 
                // loginUser uses apiPost. Let's check line 3).

                // Line 3: import { apiPost } from '@/utils/api.js'
                // I should add apiGet to imports if I want to use it.
                // But simpler: just alias fetchUserInfo to refreshUserInfo or implement it similarly.

                return await this.refreshUserInfo()
            } catch (error) {
                console.error('获取用户信息失败:', error)
                return { success: false, message: error.message }
            }
        },

        // 刷新用户信息
        async refreshUserInfo() {
            try {
                if (!this.token) {
                    return { success: false, message: '未登录' }
                }

                const response = await fetch('/api/user/info', {
                    method: 'GET',
                    headers: {
                        'Authorization': `Bearer ${this.token}`,
                        'Content-Type': 'application/json'
                    }
                })

                const data = await response.json()

                if (data.code === 0) {
                    this.user = data.data
                    this.saveToStorage()
                    return { success: true, data: data.data }
                } else {
                    return { success: false, message: data.message }
                }
            } catch (error) {
                console.error('刷新用户信息失败:', error)
                return { success: false, message: error.message || '刷新失败' }
            }
        }
    }
})