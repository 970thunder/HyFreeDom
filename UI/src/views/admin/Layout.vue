<template>
	<div class="admin-layout">
		<header class="app-header">
			<div class="app-header-inner container">
				<div class="brand">HyFreeDom</div>
				<nav class="nav">
					<el-menu mode="horizontal" :router="true" :default-active="$route.path" :ellipsis="false"
						class="nav-menu">
						<el-menu-item index="/admin/dashboard">仪表板</el-menu-item>
						<el-menu-item index="/admin/cf-accounts">CF账户</el-menu-item>
						<el-menu-item index="/admin/zones">域名同步</el-menu-item>
						<el-menu-item index="/admin/dns-records">DNS记录</el-menu-item>
						<el-menu-item index="/admin/users">用户管理</el-menu-item>
						<el-menu-item index="/admin/invites">邀请管理</el-menu-item>
						<el-menu-item index="/admin/cards">卡密管理</el-menu-item>
						<el-menu-item index="/admin/github-tasks">GitHub任务</el-menu-item>
						<el-menu-item index="/admin/settings">系统设置</el-menu-item>
					</el-menu>
				</nav>
				<div class="header-actions">
					<el-button text @click="logout">退出登录</el-button>
				</div>
			</div>
		</header>
		<main class="main container">
			<router-view />
		</main>
		<footer class="footer container">
			<span>© 2025 HyFreeDom</span>
			<a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer" class="beian-link">
				桂ICP备2024034221号-2
			</a>
		</footer>
	</div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import { ElMessageBox, ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

const isCollapsed = ref(false)

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

		// 调用后端注销接口
		await authStore.logoutAdmin()

		// 跳转到管理员登录页面
		router.push('/admin/login')
	} catch (error) {
		if (error !== 'cancel') {
			console.error('退出登录失败:', error)
		}
	}
}

onMounted(() => {
	// 监听token过期事件
	const handleTokenExpired = () => {
		// 调试日志已移除
		// 显示提示信息
		ElMessage.warning('登录已过期，请重新登录')
		// 跳转到管理员登录页
		router.push('/admin/login')
	}

	window.addEventListener('token-expired', handleTokenExpired)

	// 保存事件监听器的引用，以便在组件卸载时移除
	window._adminTokenExpiredHandler = handleTokenExpired
})

onUnmounted(() => {
	// 移除token过期事件监听器
	if (window._adminTokenExpiredHandler) {
		window.removeEventListener('token-expired', window._adminTokenExpiredHandler)
		delete window._adminTokenExpiredHandler
	}
})
</script>

<style scoped>
.admin-layout {
	min-height: 100vh;
	background: #f8fafc;
}

.app-header {
	background: #0f172a;
	color: #fff;
	padding: 10px 0;
	position: sticky;
	top: 0;
	z-index: 1000;
	box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.app-header-inner {
	display: flex;
	align-items: center;
	gap: 12px;
	max-width: 1200px;
	margin: 0 auto;
	padding: 0 16px;
	min-height: 56px;
}

.brand {
	font-weight: 700;
	font-size: 18px;
	letter-spacing: 0.4px;
	color: #fff;
	flex-shrink: 0;
	min-width: 140px;
}

.nav {
	flex: 1;
	display: flex;
	justify-content: center;
	min-width: 0;
	overflow: hidden;
}

.nav-menu {
	background: transparent;
	border: none;
	display: flex;
	flex-wrap: nowrap;
	overflow-x: auto;
	overflow-y: hidden;
	scrollbar-width: none;
	-ms-overflow-style: none;
}

.nav-menu::-webkit-scrollbar {
	display: none;
}

.nav-menu .el-menu-item {
	color: #e2e8f0;
	border-bottom: none;
	padding: 6px 10px;
	height: auto;
	line-height: 1;
	border-radius: 8px;
	white-space: nowrap;
	flex-shrink: 0;
	min-width: fit-content;
	font-size: 14px;
}

.nav-menu .el-menu-item:hover,
.nav-menu .el-menu-item.is-active {
	color: #fff;
	background: #1e293b;
	border-bottom-color: transparent;
}

/* 移除 element-plus 横向菜单默认下划线 */
:deep(.el-menu--horizontal>.el-menu-item.is-active) {
	border-bottom: 0 !important;
}

:deep(.el-menu--horizontal>.el-menu-item) {
	border-bottom: 0 !important;
}

.header-actions {
	display: flex;
	align-items: center;
	gap: 12px;
	flex-shrink: 0;
	min-width: 120px;
}

/* 顶部右侧“切换到用户端”按钮颜色 */
.header-actions :deep(.el-button) {
	color: #e2e8f0;
}

.main {
	padding: 24px 16px;
	min-height: calc(100vh - 120px);
}

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
	max-width: 1300px;
	margin: 0 auto;
}

/* 响应式设计 */
@media (max-width: 1024px) {
	.nav-menu .el-menu-item {
		padding: 0 8px;
		font-size: 13px;
	}

	.brand {
		font-size: 16px;
		min-width: 120px;
	}

	.header-actions {
		min-width: 100px;
	}
}

@media (max-width: 768px) {
	.app-header-inner {
		flex-direction: column;
		gap: 8px;
		padding: 8px 16px;
		min-height: auto;
	}

	.brand {
		order: 1;
		align-self: flex-start;
	}

	.nav {
		order: 2;
		width: 100%;
		justify-content: flex-start;
	}

	.nav-menu {
		width: 100%;
		justify-content: flex-start;
	}

	.nav-menu .el-menu-item {
		padding: 0 8px;
		font-size: 12px;
	}

	.header-actions {
		order: 3;
		align-self: flex-end;
		margin-top: 8px;
	}
}

@media (max-width: 480px) {
	.main {
		padding: 16px 8px;
	}

	.brand {
		font-size: 14px;
		min-width: 100px;
	}

	.nav-menu .el-menu-item {
		padding: 0 6px;
		font-size: 11px;
	}

	.header-actions .el-button {
		font-size: 12px;
		padding: 4px 8px;
	}
}
</style>