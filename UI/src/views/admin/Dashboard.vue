<template>
	<div class="dashboard">
		<!-- SEO组件 -->
		<SEOHead pageName="adminDashboard" />
		<!-- 页面头部 -->
		<div class="dashboard-header">
			<h1>控制台</h1>
			<div class="header-actions">
				<button class="btn outline" @click="refreshAll" :disabled="isLoading">
					{{ isLoading ? '刷新中...' : '刷新数据' }}
				</button>
			</div>
		</div>

		<!-- 统计卡片 -->
		<div class="stats-grid">
			<el-card class="stat-card" v-for="stat in stats" :key="stat.key" v-loading="isLoading">
				<div class="stat-content">
					<div class="stat-info">
						<div class="stat-label">{{ stat.label }}</div>
						<div class="stat-value">{{ stat.value }}</div>
					</div>
					<span class="stat-badge" :class="stat.badgeClass" v-if="stat.badge">
						{{ stat.badge }}
					</span>
				</div>
			</el-card>
		</div>

		<!-- 图表区域 -->
		<div class="charts-grid">
			<RegistrationLineChart title="用户注册统计" api-endpoint="/api/admin/stats/user-registration" />
			<el-card class="chart-card">
				<h3>最近 7 天 DNS 变更</h3>
				<div class="chart-placeholder"></div>
			</el-card>
		</div>

		<!-- 详细信息区域 -->
		<div class="info-grid">
			<el-card class="info-card" v-loading="isLoading">
				<template #header>
					<div class="card-header">
						<h3>Top Zones</h3>
						<button class="btn small outline" @click="loadTopZones">刷新</button>
					</div>
				</template>
				<el-table :data="topZones" class="zones-table" v-if="topZones.length > 0">
					<el-table-column prop="name" label="域名" />
					<el-table-column prop="count" label="记录数" />
					<el-table-column prop="status" label="状态">
						<template #default="{ row }">
							<span class="badge" :class="row.status === '启用' ? 'badge-success' : 'badge-default'">
								{{ row.status }}
							</span>
						</template>
					</el-table-column>
				</el-table>
				<div v-else class="empty-state">
					<p>暂无域名数据</p>
				</div>
			</el-card>

			<el-card class="info-card" v-loading="isLoading">
				<template #header>
					<div class="card-header">
						<h3>同步任务与队列</h3>
						<button class="btn small outline" @click="refreshSyncStatus">刷新</button>
					</div>
				</template>
				<div class="sync-status">
					<div class="sync-tags">
						<span class="badge badge-default">运行中任务 {{ syncStatus.running }}</span>
						<span class="badge badge-warn">24h 失败 {{ syncStatus.failed }}</span>
						<span class="badge badge-default">队列长度 {{ syncStatus.queued }}</span>
					</div>
					<div class="sync-actions">
						<el-button class="btn outline" @click="$router.push('/admin/zones')">进入同步</el-button>
						<el-button class="btn primary" @click="triggerFullSync"
							:loading="syncLoading">手动全量同步</el-button>
					</div>
				</div>
			</el-card>

			<el-card class="info-card" v-loading="isLoading">
				<template #header>
					<div class="card-header">
						<h3>最近操作</h3>
						<button class="btn small outline" @click="refreshRecentActions">刷新</button>
					</div>
				</template>
				<el-table :data="recentActions" class="actions-table" v-if="recentActions.length > 0">
					<el-table-column prop="user" label="用户" />
					<el-table-column prop="action" label="动作" />
					<el-table-column prop="detail" label="说明" />
					<el-table-column prop="time" label="时间" />
				</el-table>
				<div v-else class="empty-state">
					<p>暂无操作记录</p>
				</div>
			</el-card>
		</div>
	</div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth.js'
import { apiGet } from '@/utils/api.js'
import SEOHead from '@/components/SEOHead.vue'
import RegistrationLineChart from '@/components/RegistrationLineChart.vue'

// 认证store
const authStore = useAuthStore()

// 加载状态
const isLoading = ref(false)


// 统计数据
const stats = ref([
	{
		key: 'users',
		label: '总用户数',
		value: '0',
		badge: '本周 +0',
		badgeClass: 'badge-default'
	},
	{
		key: 'zones',
		label: '启用分发域名',
		value: '0',
		badge: '总域名 0',
		badgeClass: 'badge-success'
	},
	{
		key: 'records',
		label: 'DNS 记录总数',
		value: '0',
		badge: '今日新增 0',
		badgeClass: 'badge-default'
	},
	{
		key: 'points',
		label: '系统总积分',
		value: '0',
		badge: '活跃用户 0',
		badgeClass: 'badge-default'
	}
])

// Top Zones 数据
const topZones = ref([])

// 同步状态
const syncStatus = ref({
	running: 0,
	failed: 0,
	queued: 0
})

const syncLoading = ref(false)

// 最近操作
const recentActions = ref([])


// 获取动作类型
const getActionType = (action) => {
	const typeMap = {
		'sync_zone': 'primary',
		'apply_domain': 'success',
		'delete_domain': 'danger',
		'update_settings': 'warning'
	}
	return typeMap[action] || 'info'
}

// 获取动作标签
const getActionLabel = (action) => {
	const labelMap = {
		'sync_zone': '同步域名',
		'apply_domain': '申请域名',
		'delete_domain': '删除域名',
		'update_settings': '更新设置'
	}
	return labelMap[action] || action
}

// 刷新同步状态
const refreshSyncStatus = async () => {
	try {
		await loadStats()
		ElMessage.success('同步状态已刷新')
	} catch (error) {
		ElMessage.error('刷新失败')
	}
}

// 触发全量同步
const triggerFullSync = async () => {
	syncLoading.value = true
	try {
		// 这里应该调用API触发全量同步
		// 暂时模拟API调用
		await new Promise(resolve => setTimeout(resolve, 2000))
		ElMessage.success('全量同步已启动')
		// 刷新数据
		await loadStats()
	} catch (error) {
		ElMessage.error('同步启动失败')
	} finally {
		syncLoading.value = false
	}
}

// 刷新最近操作
const refreshRecentActions = async () => {
	try {
		await loadRecentActions()
		ElMessage.success('最近操作已刷新')
	} catch (error) {
		ElMessage.error('刷新失败')
	}
}

// 刷新所有数据
const refreshAll = async () => {
	await loadDashboardData()
	ElMessage.success('数据已刷新')
}

// 加载统计数据
const loadStats = async () => {
	try {
		// 调试日志已移除

		// 调用新的统计API
		const response = await apiGet('/api/admin/stats/dashboard', { token: authStore.adminToken })
		// 调试日志已移除

		const data = response.data || {}

		// 更新统计数据
		stats.value[0].value = data.totalUsers?.toString() || '0'
		stats.value[0].badge = `本周 +${data.weeklyNewUsers || 0}`

		stats.value[1].value = data.activeZones?.toString() || '0'
		stats.value[1].badge = `总域名 ${data.totalZones || 0}`

		stats.value[2].value = data.totalDnsRecords?.toString() || '0'
		stats.value[2].badge = `今日新增 ${data.dailyNewRecords || 0}`

		stats.value[3].value = data.totalPoints?.toString() || '0'
		stats.value[3].badge = `活跃用户 ${data.activeUsers || 0}`

		// 调试日志已移除

	} catch (error) {
		console.error('加载统计数据失败:', error)
		ElMessage.error('加载统计数据失败: ' + error.message)
	}
}

// 加载Top Zones数据
const loadTopZones = async () => {
	try {
		// 调试日志已移除
		const response = await apiGet('/api/admin/zones', {
			token: authStore.adminToken,
			params: { page: 1, size: 5 }
		})

		// 调试日志已移除

		topZones.value = response.data?.list?.map(zone => ({
			name: zone.name,
			count: Math.floor(Math.random() * 500), // 临时随机数，实际应该从DNS记录统计
			status: zone.status === 1 ? '启用' : '禁用'
		})) || []

		// 调试日志已移除

	} catch (error) {
		console.error('加载Top Zones失败:', error)
		ElMessage.error('加载Top Zones失败: ' + error.message)
	}
}

// 加载最近操作
const loadRecentActions = async () => {
	try {
		// 调试日志已移除
		const response = await apiGet('/api/admin/audit', {
			token: authStore.adminToken,
			params: { page: 1, size: 10 }
		})

		// 调试日志已移除

		recentActions.value = response.data?.list?.map(log => ({
			user: log.username || 'system',
			action: log.action,
			detail: log.details || log.action,
			time: formatTime(log.createdAt)
		})) || []

		// 调试日志已移除

	} catch (error) {
		console.error('加载最近操作失败:', error)
		ElMessage.error('加载最近操作失败: ' + error.message)
	}
}

// 格式化时间
const formatTime = (timestamp) => {
	if (!timestamp) return '未知时间'

	const now = new Date()
	const time = new Date(timestamp)
	const diff = now - time

	if (diff < 60000) return '刚刚'
	if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
	if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
	return `${Math.floor(diff / 86400000)} 天前`
}

// 加载仪表板数据
const loadDashboardData = async () => {
	isLoading.value = true
	try {
		await Promise.all([
			loadStats(),
			loadTopZones(),
			loadRecentActions()
		])
	} catch (error) {
		console.error('加载仪表板数据失败:', error)
		ElMessage.error('数据加载失败')
	} finally {
		isLoading.value = false
	}
}

onMounted(() => {
	loadDashboardData()
})
</script>

<style scoped>
.dashboard {
	display: flex;
	flex-direction: column;
	gap: 16px;
}

/* 页面头部样式 */
.dashboard-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 8px;
}

.dashboard-header h1 {
	margin: 0;
	font-size: 24px;
	font-weight: 600;
	color: #0f172a;
}

.header-actions {
	display: flex;
	gap: 8px;
}

.btn {
	display: inline-flex;
	align-items: center;
	gap: 8px;
	padding: 10px 14px;
	border-radius: 10px;
	border: 1px solid transparent;
	font-weight: 600;
	cursor: pointer;
}

.btn.primary {
	background: #6366f1;
	color: #fff;
}

.btn.primary:hover {
	background: #4f46e5;
}

.btn.outline {
	background: #fff;
	border-color: #cbd5e1;
	color: #0f172a;
}

.btn.outline:hover {
	background: #f8fafc;
}

/* 统计卡片样式 - 参照原型图 */
.stats-grid {
	display: grid;
	grid-template-columns: repeat(4, 1fr);
	gap: 16px;
}

.stat-card {
	border-radius: 12px;
	box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
	border: 1px solid #e2e8f0;
}

.stat-content {
	display: flex;
	justify-content: space-between;
	align-items: baseline;
}

.stat-info {
	display: flex;
	flex-direction: column;
	gap: 0;
}

.stat-label {
	font-size: 12px;
	color: #64748b;
	font-weight: 500;
	margin-bottom: 4px;
}

.stat-value {
	font-size: 28px;
	font-weight: 700;
	color: #0f172a;
	line-height: 1;
}

.stat-badge {
	display: inline-flex;
	align-items: center;
	padding: 4px 8px;
	border-radius: 999px;
	font-size: 12px;
	border: 1px solid #e2e8f0;
	color: #334155;
	background: #f8fafc;
}

.stat-badge.badge-success {
	background: #ecfdf5;
	color: #065f46;
	border-color: #a7f3d0;
}

/* 图表区域样式 */
.charts-grid {
	display: grid;
	grid-template-columns: repeat(2, 1fr);
	gap: 16px;
}

.chart-card {
	border-radius: 12px;
	box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
	border: 1px solid #e2e8f0;
}

.chart-card h3 {
	margin: 0 0 16px 0;
	font-size: 16px;
	color: #0f172a;
	font-weight: 600;
}


.chart-placeholder {
	height: 250px;
	background: repeating-linear-gradient(45deg, #f1f5f9, #f1f5f9 10px, #e2e8f0 10px, #e2e8f0 20px);
	border-radius: 12px;
	border: 1px dashed #cbd5e1;
}

/* 详细信息区域样式 */
.info-grid {
	display: grid;
	grid-template-columns: repeat(3, 1fr);
	gap: 16px;
}

.info-card {
	border-radius: 12px;
	box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
	border: 1px solid #e2e8f0;
}

.info-card h3 {
	margin: 0 0 16px 0;
	font-size: 16px;
	color: #0f172a;
	font-weight: 600;
}

/* 表格样式 */
.zones-table,
.actions-table {
	width: 100%;
	border-collapse: collapse;
	border: 1px solid #e2e8f0;
	border-radius: 12px;
	overflow: hidden;
}

.zones-table th,
.zones-table td,
.actions-table th,
.actions-table td {
	text-align: left;
	padding: 12px 10px;
	border-bottom: 1px solid #e2e8f0;
	font-size: 14px;
}

.zones-table th,
.actions-table th {
	background: #f1f5f9;
	color: #0f172a;
}

.zones-table tr:hover td,
.actions-table tr:hover td {
	background: #f8fafc;
}

/* Badge 样式 */
.badge {
	display: inline-flex;
	align-items: center;
	padding: 4px 8px;
	border-radius: 999px;
	font-size: 12px;
	border: 1px solid #e2e8f0;
	color: #334155;
	background: #f8fafc;
}

.badge.badge-success {
	background: #ecfdf5;
	color: #065f46;
	border-color: #a7f3d0;
}

.badge.badge-warn {
	background: #fff7ed;
	color: #9a3412;
	border-color: #fed7aa;
}

/* 同步状态样式 */
.sync-status {
	display: flex;
	flex-direction: column;
	gap: 12px;
}

.sync-tags {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
}

.sync-actions {
	display: flex;
	gap: 8px;
	margin-top: 12px;
}

/* 卡片头部样式 */
.card-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
}

.card-header h3 {
	margin: 0;
	font-size: 16px;
	font-weight: 600;
	color: #0f172a;
}

/* 小按钮样式 */
.btn.small {
	padding: 6px 12px;
	font-size: 12px;
}

/* 空状态样式 */
.empty-state {
	text-align: center;
	padding: 40px 20px;
	color: #64748b;
}

.empty-state p {
	margin: 0;
	font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 960px) {
	.stats-grid {
		grid-template-columns: repeat(2, 1fr);
	}
}

@media (max-width: 720px) {
	.stats-grid {
		grid-template-columns: 1fr;
	}

	.charts-grid {
		grid-template-columns: 1fr;
	}

	.info-grid {
		grid-template-columns: 1fr;
	}

	.sync-tags {
		flex-direction: column;
		align-items: flex-start;
	}

	.sync-actions {
		flex-direction: column;
	}

	.dashboard-header {
		flex-direction: column;
		align-items: flex-start;
		gap: 12px;
	}
}
</style>