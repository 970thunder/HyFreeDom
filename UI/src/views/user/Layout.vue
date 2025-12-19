<template>
	<div>
		<header class="app-header">
			<div class="app-header-inner container">
				<div class="brand">HyFreeDom</div>
				<div class="spacer"></div>
				<button class="mobile-menu-toggle" @click="toggleMobileMenu" v-if="isMobile">
					<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
						<line x1="3" y1="6" x2="21" y2="6"></line>
						<line x1="3" y1="12" x2="21" y2="12"></line>
						<line x1="3" y1="18" x2="21" y2="18"></line>
					</svg>
				</button>
			</div>
		</header>

		<!-- Mobile Sidebar Overlay -->
		<div class="mobile-overlay" :class="{ active: mobileMenuOpen }" @click="closeMobileMenu"></div>

		<!-- Mobile Sidebar -->
		<div class="mobile-sidebar" :class="{ active: mobileMenuOpen }">
			<div class="mobile-sidebar-header">
				<div class="brand">HyFreeDom</div>
				<button class="close-btn" @click="closeMobileMenu">
					<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
						<line x1="18" y1="6" x2="6" y2="18"></line>
						<line x1="6" y1="6" x2="18" y2="18"></line>
					</svg>
				</button>
			</div>
			<nav class="mobile-nav">
				<router-link to="/user/dashboard" @click="closeMobileMenu" class="nav-link">
					<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
						<rect x="3" y="3" width="7" height="7"></rect>
						<rect x="14" y="3" width="7" height="7"></rect>
						<rect x="14" y="14" width="7" height="7"></rect>
						<rect x="3" y="14" width="7" height="7"></rect>
					</svg>
					主页
				</router-link>
				<router-link to="/user/apply" @click="closeMobileMenu" class="nav-link">
					<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
						<path
							d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z">
						</path>
						<polyline points="3.27,6.96 12,12.01 20.73,6.96"></polyline>
						<line x1="12" y1="22.08" x2="12" y2="12"></line>
					</svg>
					申请域名
				</router-link>
				<a href="javascript:;" @click.prevent="handleRestrictedNav('/user/domains')" class="nav-link"
					:class="{ 'router-link-active': $route.path === '/user/domains' }">
					<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
						<circle cx="12" cy="12" r="10"></circle>
						<path d="M8 14s1.5 2 4 2 4-2 4-2"></path>
						<line x1="9" y1="9" x2="9.01" y2="9"></line>
						<line x1="15" y1="9" x2="15.01" y2="9"></line>
					</svg>
					我的域名
				</a>
				<a href="javascript:;" @click.prevent="handleRestrictedNav('/user/invite')" class="nav-link"
					:class="{ 'router-link-active': $route.path === '/user/invite' }">
					<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
						<path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
						<circle cx="8.5" cy="7" r="4"></circle>
						<line x1="20" y1="8" x2="20" y2="14"></line>
						<line x1="23" y1="11" x2="17" y2="11"></line>
					</svg>
					邀请
				</a>
				<router-link to="/user/recharge" @click="closeMobileMenu" class="nav-link">
					<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
						<line x1="12" y1="1" x2="12" y2="23"></line>
						<path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
					</svg>
					充值
				</router-link>
				<router-link to="/user/announcements" @click="closeMobileMenu" class="nav-link">
					<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
						<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
						<polyline points="14,2 14,8 20,8"></polyline>
						<line x1="16" y1="13" x2="8" y2="13"></line>
						<line x1="16" y1="17" x2="8" y2="17"></line>
						<polyline points="10,9 9,9 8,9"></polyline>
					</svg>
					公告
				</router-link>
				<router-link to="/user/github-tasks" @click="closeMobileMenu" class="nav-link">
					<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
						<path
							d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z">
						</path>
					</svg>
					GitHub任务
				</router-link>
				<router-link to="/user/profile" @click="closeMobileMenu" class="nav-link">
					<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
						<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
						<circle cx="12" cy="7" r="4"></circle>
					</svg>
					{{ userInfo.username || '用户信息' }}
				</router-link>
				<button @click="logout" class="nav-link logout-btn">
					<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
						<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
						<polyline points="16,17 21,12 16,7"></polyline>
						<line x1="21" y1="12" x2="9" y2="12"></line>
					</svg>
					退出登录
				</button>
			</nav>
			<div class="mobile-sidebar-footer">
			</div>
		</div>

		<div class="container" style="padding-top:16px;">
			<!-- Desktop Navigation -->
			<nav class="desktop-nav" v-if="!isMobile">
				<div class="nav-links">
					<router-link to="/user/dashboard" class="nav-link"
						:class="{ active: $route.path === '/user/dashboard' }">
						<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
							stroke-width="2">
							<rect x="3" y="3" width="7" height="7"></rect>
							<rect x="14" y="3" width="7" height="7"></rect>
							<rect x="14" y="14" width="7" height="7"></rect>
							<rect x="3" y="14" width="7" height="7"></rect>
						</svg>
						主页
					</router-link>
					<router-link to="/user/apply" class="nav-link" :class="{ active: $route.path === '/user/apply' }">
						<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
							stroke-width="2">
							<path
								d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z">
							</path>
							<polyline points="3.27,6.96 12,12.01 20.73,6.96"></polyline>
							<line x1="12" y1="22.08" x2="12" y2="12"></line>
						</svg>
						申请域名
					</router-link>
					<a href="javascript:;" @click.prevent="handleRestrictedNav('/user/domains')" class="nav-link"
						:class="{ active: $route.path === '/user/domains' }">
						<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
							stroke-width="2">
							<circle cx="12" cy="12" r="10"></circle>
							<path d="M8 14s1.5 2 4 2 4-2 4-2"></path>
							<line x1="9" y1="9" x2="9.01" y2="9"></line>
							<line x1="15" y1="9" x2="15.01" y2="9"></line>
						</svg>
						我的域名
					</a>
					<a href="javascript:;" @click.prevent="handleRestrictedNav('/user/invite')" class="nav-link"
						:class="{ active: $route.path === '/user/invite' }">
						<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
							stroke-width="2">
							<path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
							<circle cx="8.5" cy="7" r="4"></circle>
							<line x1="20" y1="8" x2="20" y2="14"></line>
							<line x1="23" y1="11" x2="17" y2="11"></line>
						</svg>
						邀请
					</a>
					<router-link to="/user/recharge" class="nav-link"
						:class="{ active: $route.path === '/user/recharge' }">
						<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
							stroke-width="2">
							<line x1="12" y1="1" x2="12" y2="23"></line>
							<path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
						</svg>
						充值
					</router-link>
					<router-link to="/user/announcements" class="nav-link"
						:class="{ active: $route.path === '/user/announcements' }">
						<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
							stroke-width="2">
							<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
							<polyline points="14,2 14,8 20,8"></polyline>
							<line x1="16" y1="13" x2="8" y2="13"></line>
							<line x1="16" y1="17" x2="8" y2="17"></line>
							<polyline points="10,9 9,9 8,9"></polyline>
						</svg>
						公告
					</router-link>
					<router-link to="/user/github-tasks" class="nav-link"
						:class="{ active: $route.path === '/user/github-tasks' }">
						<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
							stroke-width="2">
							<path
								d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z">
							</path>
						</svg>
						GitHub任务
					</router-link>
					<router-link to="/user/profile" class="nav-link"
						:class="{ active: $route.path === '/user/profile' }">
						<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
							stroke-width="2">
							<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
							<circle cx="12" cy="7" r="4"></circle>
						</svg>
						{{ userInfo.username || '用户信息' }}
					</router-link>
				</div>
				<div class="nav-actions">
					<button @click="logout" class="nav-link logout-btn">
						<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
							stroke-width="2">
							<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
							<polyline points="16,17 21,12 16,7"></polyline>
							<line x1="21" y1="12" x2="9" y2="12"></line>
						</svg>
						退出登录
					</button>
				</div>
			</nav>

			<div class="page">
				<router-view />
			</div>
		</div>
		<footer class="footer container">
			<span>© 2025 HyFreeDom</span>
			<a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer" class="beian-link">
				桂ICP备2024034221号-2
			</a>
		</footer>

		<!-- 公告弹窗 -->
		<AnnouncementModal v-model:visible="showAnnouncementModal" @confirm="handleAnnouncementConfirm" />

		<FeaturedSitesSidebar />
	</div>
</template>

<script setup>
import FeaturedSitesSidebar from '@/components/FeaturedSitesSidebar.vue'
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import { apiGet } from '@/utils/api.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import AnnouncementModal from '@/components/AnnouncementModal.vue'

const router = useRouter()
const authStore = useAuthStore()

const isMobile = ref(false)
const mobileMenuOpen = ref(false)

// 用户信息
const userInfo = ref({
	username: '',
	email: '',
	createdAt: '',
	status: '',
	isVerified: false
})

// 公告弹窗
const showAnnouncementModal = ref(false)

// 限制访问的导航处理
const handleRestrictedNav = (path) => {
	if (!userInfo.value.isVerified) {
		ElMessage.warning('请先完成实名认证')
		router.push('/user/profile')
		if (isMobile.value) {
			closeMobileMenu()
		}
	} else {
		router.push(path)
		if (isMobile.value) {
			closeMobileMenu()
		}
	}
}

const checkMobile = () => {
	isMobile.value = window.innerWidth <= 768
}

// 加载用户信息
const loadUserInfo = async () => {
	try {
		if (!authStore.token) {
			console.error('用户token不存在')
			return
		}

		const response = await apiGet('/api/user/info', { token: authStore.token })
		if (response.data) {
			userInfo.value = response.data
			// 调试日志已移除
		}
	} catch (error) {
		console.error('加载用户信息失败:', error)
		// 如果接口失败，使用默认值
		userInfo.value = {
			username: '用户信息',
			email: '',
			createdAt: '',
			status: ''
		}
	}
}

// 检查是否需要显示公告弹窗
const checkAnnouncementModal = () => {
	// 检查是否已登录
	if (!authStore.isLoggedIn || !authStore.token) {
		return
	}

	// 获取今天的日期
	const today = new Date().toDateString()

	// 检查今天是否已经显示过公告
	const lastShownDate = localStorage.getItem('announcement_last_shown')

	// 如果今天还没有显示过，则显示公告弹窗
	if (lastShownDate !== today) {
		// 延迟显示，确保页面完全加载
		setTimeout(() => {
			showAnnouncementModal.value = true
		}, 1000)
	}
}

// 处理公告确认
const handleAnnouncementConfirm = () => {
	// 记录今天已经显示过公告
	const today = new Date().toDateString()
	localStorage.setItem('announcement_last_shown', today)
	// 调试日志已移除
}

const toggleMobileMenu = () => {
	mobileMenuOpen.value = !mobileMenuOpen.value
}

const closeMobileMenu = () => {
	mobileMenuOpen.value = false
}

// 退出登录
const logout = async () => {
	try {
		await ElMessageBox.confirm(
			'确定要退出登录吗？',
			'确认退出',
			{
				confirmButtonText: '确定',
				cancelButtonText: '取消',
				type: 'warning'
			}
		)

		// 调用用户登出
		authStore.logout()
		ElMessage.success('已退出登录')

		// 关闭移动端菜单
		closeMobileMenu()

		// 跳转到登录页
		router.push('/user/login')
	} catch (error) {
		// 用户取消退出
		if (error !== 'cancel') {
			console.error('退出登录失败:', error)
		}
	}
}

// 监听用户信息变化
watch(() => authStore.user, (newUser) => {
	if (newUser) {
		userInfo.value = { ...userInfo.value, ...newUser }
	}
}, { deep: true })

onMounted(() => {
	checkMobile()
	window.addEventListener('resize', checkMobile)

	// 加载用户信息
	loadUserInfo()

	// 检查是否需要显示公告弹窗
	checkAnnouncementModal()

	// 监听路由变化，确保每次进入用户页面都检查公告
	let unwatchRoute = router.afterEach((to) => {
		if (to.path.startsWith('/user') && to.path !== '/user/login' && to.path !== '/user/register') {
			// 延迟检查，确保页面切换完成
			setTimeout(() => {
				checkAnnouncementModal()
			}, 500)
		}
	})

	// 监听token过期事件
	const handleTokenExpired = () => {
		// 调试日志已移除
		// 显示提示信息
		ElMessage.warning('登录已过期，请重新登录')
		// 跳转到登录页
		router.push('/user/login')
	}

	window.addEventListener('token-expired', handleTokenExpired)

	// 保存事件监听器的引用，以便在组件卸载时移除
	window._tokenExpiredHandler = handleTokenExpired
})

onUnmounted(() => {
	window.removeEventListener('resize', checkMobile)
	// 移除token过期事件监听器
	if (window._tokenExpiredHandler) {
		window.removeEventListener('token-expired', window._tokenExpiredHandler)
		delete window._tokenExpiredHandler
	}
	// 清理路由监听器
	if (unwatchRoute) {
		unwatchRoute()
	}
})
</script>

<style scoped>
.footer {
	padding: 16px;
	color: #64748b;
	font-size: 14px;
	text-align: center;
	border-top: 1px solid #e2e8f0;
	display: flex;
	justify-content: space-between;
	align-items: center;
}

.beian-link {
	color: #64748b;
	text-decoration: none;
	transition: color 0.15s ease;
}

.beian-link:hover {
	color: #0f172a;
	text-decoration: underline;
}

.container {
	max-width: 1200px;
	margin: 0 auto;
}

/* Mobile Menu Toggle */
.mobile-menu-toggle {
	background: none;
	border: none;
	color: #fff;
	cursor: pointer;
	padding: 8px;
	border-radius: 6px;
	display: none;
	transition: background-color 0.15s ease;
	will-change: background-color;
}

.mobile-menu-toggle:hover {
	background: rgba(255, 255, 255, 0.1);
}

/* Desktop Navigation */
.desktop-nav {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 20px;
	padding: 12px 16px;
	background: rgba(255, 255, 255, 0.6);
	backdrop-filter: blur(10px);
	-webkit-backdrop-filter: blur(10px);
	border: 1px solid rgba(255, 255, 255, 0.3);
	border-radius: 16px;
	box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
	will-change: transform;
}

.nav-links {
	display: flex;
	gap: 4px;
}

.nav-actions {
	display: flex;
	gap: 8px;
}

.desktop-nav .nav-link {
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 10px 16px;
	border-radius: 12px;
	text-decoration: none;
	color: #64748b;
	font-weight: 500;
	transition: transform 0.2s ease, background-color 0.2s ease, color 0.2s ease;
	background: rgba(255, 255, 255, 0.4);
	border: 1px solid rgba(255, 255, 255, 0.3);
	backdrop-filter: blur(8px);
	-webkit-backdrop-filter: blur(8px);
	will-change: transform;
}

.desktop-nav .nav-link:hover {
	background: rgba(255, 255, 255, 0.6);
	color: #0f172a;
	transform: translateY(-1px);
}

.desktop-nav .nav-link.active {
	background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
	color: #fff;
	border-color: rgba(255, 255, 255, 0.3);
	box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
}

/* Mobile Sidebar */
.mobile-overlay {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(0, 0, 0, 0.5);
	z-index: 998;
	opacity: 0;
	visibility: hidden;
	transition: opacity 0.2s ease, visibility 0.2s ease;
	will-change: opacity;
}

.mobile-overlay.active {
	opacity: 1;
	visibility: visible;
}

.mobile-sidebar {
	position: fixed;
	top: 0;
	left: 0;
	width: 280px;
	height: 100vh;
	background: #fff;
	z-index: 999;
	transform: translateX(-100%);
	transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
	display: flex;
	flex-direction: column;
	box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
	will-change: transform;
}

.mobile-sidebar.active {
	transform: translateX(0);
}

.mobile-sidebar-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 16px 20px;
	border-bottom: 1px solid #e2e8f0;
	background: #0f172a;
	color: #fff;
}

.mobile-sidebar-header .brand {
	font-weight: 700;
	font-size: 18px;
}

.close-btn {
	background: none;
	border: none;
	color: #fff;
	cursor: pointer;
	padding: 4px;
	border-radius: 4px;
	transition: background-color 0.15s ease;
	will-change: background-color;
}

.close-btn:hover {
	background: rgba(255, 255, 255, 0.1);
}

.mobile-nav {
	flex: 1;
	padding: 20px 0;
	overflow-y: auto;
	-webkit-overflow-scrolling: touch;
	scroll-behavior: smooth;
	contain: layout style paint;
}

.mobile-nav .nav-link {
	display: flex;
	align-items: center;
	gap: 12px;
	padding: 12px 20px;
	color: #64748b;
	text-decoration: none;
	font-weight: 500;
	transition: background-color 0.15s ease, color 0.15s ease, border-left-color 0.15s ease;
	border-left: 3px solid transparent;
	will-change: background-color;
}

.mobile-nav .nav-link:hover,
.mobile-nav .nav-link.router-link-active {
	background: #f8fafc;
	color: #0f172a;
	border-left-color: #6366f1;
}

.mobile-nav .nav-link svg {
	flex-shrink: 0;
}

.logout-btn {
	background: none;
	border: none;
	width: 100%;
	text-align: left;
	cursor: pointer;
}

.logout-btn:hover {
	background: #f8fafc;
	color: #0f172a;
	border-left-color: #ef4444;
}

.mobile-sidebar-footer {
	padding: 20px;
	border-top: 1px solid #e2e8f0;
}

/* Responsive Design */
@media (max-width: 768px) {
	.mobile-menu-toggle {
		display: block;
	}

	.desktop-nav {
		display: none;
	}

	.container {
		padding: 0 12px;
	}

	.page {
		padding: 12px 0;
	}
}

@media (max-width: 480px) {
	.mobile-sidebar {
		width: 100%;
	}

	.container {
		padding: 0 8px;
	}
}

/* Performance optimizations for reduced motion */
@media (prefers-reduced-motion: reduce) {

	.mobile-sidebar,
	.mobile-overlay,
	.desktop-nav .nav-link,
	.mobile-nav .nav-link,
	.mobile-menu-toggle,
	.close-btn {
		transition: none !important;
		animation: none !important;
	}

	.mobile-sidebar {
		transform: translateX(-100%);
	}

	.mobile-sidebar.active {
		transform: translateX(0);
	}
}
</style>
