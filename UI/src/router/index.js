import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'

const routes = [
    { path: '/', component: () => import('../views/Landing.vue') },
    // User
    { path: '/user/login', component: () => import('../views/user/Login.vue') },
    { path: '/user/forgot', component: () => import('../views/user/Forgot.vue') },
    { path: '/user/register', component: () => import('../views/user/Register.vue') },
    {
        path: '/user', component: () => import('../views/user/Layout.vue'), children: [
            { path: 'dashboard', component: () => import('../views/user/Dashboard.vue') },
            { path: 'apply', component: () => import('../views/user/ApplyDomain.vue') },
            { path: 'domains', component: () => import('../views/user/MyDomains.vue') },
            { path: 'invite', component: () => import('../views/user/Invite.vue') },
            { path: 'recharge', component: () => import('../views/user/Recharge.vue') },
            { path: 'profile', component: () => import('../views/user/Profile.vue') },
            { path: 'announcements', component: () => import('../views/user/Announcements.vue') },
            { path: 'github-tasks', component: () => import('../views/user/GithubTasks.vue') },
        ]
    },
    // 法律条款页面
    { path: '/legal/user-agreement', component: () => import('../views/legal/UserAgreement.vue') },
    { path: '/legal/privacy-policy', component: () => import('../views/legal/PrivacyPolicy.vue') },
    // Admin login (separate)
    { path: '/admin/login', component: () => import('../views/admin/AdminLogin.vue') },
    // Admin
    {
        path: '/admin', component: () => import('../views/admin/Layout.vue'), children: [
            { path: 'dashboard', component: () => import('../views/admin/Dashboard.vue') },
            { path: 'cf-accounts', component: () => import('../views/admin/CfAccounts.vue') },
            { path: 'zones', component: () => import('../views/admin/Zones.vue') },
            { path: 'dns-records', component: () => import('../views/admin/DnsRecords.vue') },
            { path: 'users', component: () => import('../views/admin/Users.vue') },
            { path: 'invites', component: () => import('../views/admin/Invites.vue') },
            { path: 'cards', component: () => import('../views/admin/Cards.vue') },
            { path: 'points-logs', component: () => import('../views/admin/PointsLogs.vue') },
            { path: 'featured-sites', component: () => import('../views/admin/FeaturedSites.vue') },
            { path: 'settings', component: () => import('../views/admin/Settings.vue') },
            { path: 'github-tasks', component: () => import('../views/admin/GithubTasks.vue') },
        ]
    },
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
    const authStore = useAuthStore()

    // 只在首次访问时加载状态，避免从登录页跳转时重新加载导致状态丢失
    if (from.path === '/' && !authStore.isLoggedIn && !authStore.isAdminLoggedIn) {
        authStore.loadFromStorage()
    }

    // 调试日志已移除

    // 管理员路由需要认证
    if (to.path.startsWith('/admin') && to.path !== '/admin/login') {
        if (!authStore.isAdminLoggedIn || !authStore.adminToken) {
            // 调试日志已移除
            next('/admin/login')
            return
        }
    }

    // 用户路由需要认证
    if (to.path.startsWith('/user') && to.path !== '/user/login' && to.path !== '/user/register' && to.path !== '/user/forgot') {
        if (!authStore.isLoggedIn || !authStore.token) {
            // 调试日志已移除
            next('/user/login')
            return
        }
    }

    // 已登录用户访问登录页，重定向到对应首页
    if (to.path === '/admin/login' && authStore.isAdminLoggedIn) {
        next('/admin/dashboard')
        return
    }

    if (to.path === '/user/login' && authStore.isLoggedIn) {
        next('/user/dashboard')
        return
    }

    next()
})

// 路由错误处理
router.onError((error) => {
    console.error('路由错误:', error)

    // 如果是token过期相关的错误，跳转到登录页
    if (error.message && (error.message.includes('Token已过期') || error.message.includes('Token无效'))) {
        const authStore = useAuthStore()
        if (window.location.pathname.startsWith('/user')) {
            authStore.handleTokenExpired()
        } else if (window.location.pathname.startsWith('/admin')) {
            authStore.handleAdminTokenExpired()
        }
    }
})

export default router
