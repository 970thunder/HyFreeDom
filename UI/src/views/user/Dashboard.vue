<template>
	<div class="dashboard-container" v-loading="isLoading">
		<div class="grid cols-3">
			<div class="card stat">
				<div>
					<div class="label">我的积分</div>
					<div class="value">{{ userStats.balance || 0 }}</div>
				</div>
				<span class="badge">本周 +{{ userStats.weeklyChange || 0 }}</span>
			</div>
			<div class="card stat">
				<div>
					<div class="label">已申请域名</div>
					<div class="value">{{ userStats.domainCount || 0 }}</div>
				</div>
				<span class="badge">上限 {{ userStats.maxDomains || 5 }}</span>
			</div>
			<div class="card stat">
				<div>
					<div class="label">我的邀请码</div>
					<div class="value">{{ inviteCode || '未生成' }}</div>
				</div>
				<router-link class="btn outline" to="/user/invite">管理</router-link>
			</div>
		</div>

		<div class="grid cols-2" style="margin-top:16px;">
			<div class="card">
				<h3>快速申请</h3>
				<div class="form">
					<div class="row">
						<select class="select" v-model="quickApply.zoneId" style="max-width: 260px;">
							<option value="">选择域名（已启用）</option>
							<option v-for="zone in availableZones" :key="zone.id" :value="zone.id">
								{{ zone.name }}
							</option>
						</select>
						<input class="input" v-model="quickApply.prefix" style="max-width: 200px;"
							placeholder="前缀，如：app">
						<input class="input" v-model="quickApply.value" style="max-width: 240px;"
							placeholder="记录值，如：1.2.3.4">
						<button class="btn primary" @click="handleQuickApply" :disabled="isApplying">
							{{ isApplying ? '申请中...' : `申请（-${domainCost}积分）` }}
						</button>
					</div>
				</div>
			</div>
			<div class="card">
				<h3>最近申请</h3>
				<div v-if="recentDomains.length === 0" class="empty-state">
					<p>暂无申请记录</p>
				</div>
				<table class="table" v-else>
					<thead>
						<tr>
							<th>域名</th>
							<th>记录</th>
							<th>时间</th>
							<th>状态</th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="domain in recentDomains" :key="domain.id">
							<td data-label="域名">{{ domain.fullDomain }}</td>
							<td data-label="记录">{{ getRecordType(domain) }} → {{ getRecordValue(domain) }}</td>
							<td data-label="时间">{{ formatTime(domain.createdAt) }}</td>
							<td data-label="状态">
								<span class="badge" :class="getStatusClass(domain.status)">{{ domain.status }}</span>
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>
	</div>
</template>
<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import { apiGet, apiPost } from '@/utils/api.js'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

// 响应式数据
const isLoading = ref(false)
const isApplying = ref(false)
const isVerified = ref(false)
const userStats = ref({
	balance: 0,
	weeklyChange: 0,
	domainCount: 0,
	maxDomains: 5
})
const availableZones = ref([])
const recentDomains = ref([])
const inviteCode = ref('')
const baseCost = ref(10) // 基础积分消耗

// 快速申请表单
const quickApply = ref({
	zoneId: '',
	prefix: '',
	value: ''
})

// 计算选中区域
const selectedZone = computed(() => {
	return availableZones.value.find(zone => zone.id == quickApply.value.zoneId)
})

// 计算消耗积分 - 按照ApplyDomain.vue的逻辑
const domainCost = computed(() => {
	if (!selectedZone.value) return baseCost.value

	const domain = selectedZone.value.name.toLowerCase()
	let multiplier = 1.0

	// 根据域名后缀计算倍数
	if (domain.endsWith('.cn') || domain.endsWith('.com')) {
		multiplier = 2.0  // .cn / .com：2.0倍
	} else if (domain.endsWith('.top')) {
		multiplier = 1.5  // .top：1.5倍
	} else {
		multiplier = 1.0  // 其它：1.0倍
	}

	return Math.floor(baseCost.value * multiplier)
})

// 计算属性
const canApply = computed(() => {
	return quickApply.value.zoneId &&
		quickApply.value.prefix.trim() &&
		quickApply.value.value.trim()
})

// 加载用户统计数据
const loadUserStats = async () => {
	try {
		const response = await apiGet('/api/user/points', { token: authStore.token })
		if (response.data) {
			userStats.value.balance = response.data.balance || 0
			// 计算本周变化（简化处理）
			userStats.value.weeklyChange = Math.max(0, Math.floor(userStats.value.balance / 10))
		}
	} catch (error) {
		console.error('加载积分统计失败:', error)
	}
}

// 加载域名统计
const loadDomainStats = async () => {
	try {
		const response = await apiGet('/api/user/domains', {
			token: authStore.token,
			params: { page: 1, size: 1 }
		})
		if (response.data) {
			userStats.value.domainCount = response.data.total || 0
		}
	} catch (error) {
		console.error('加载域名统计失败:', error)
	}
}

// 加载可用域名
const loadAvailableZones = async () => {
	try {
		const response = await apiGet('/api/zones', { token: authStore.token })
		if (response.data) {
			availableZones.value = response.data.filter(zone => zone.enabled)
		}
	} catch (error) {
		console.error('加载可用域名失败:', error)
	}
}

// 加载最近申请的域名
const loadRecentDomains = async () => {
	try {
		const response = await apiGet('/api/user/domains', {
			token: authStore.token,
			params: { page: 1, size: 5 }
		})
		if (response.data && response.data.list) {
			recentDomains.value = response.data.list
		}
	} catch (error) {
		console.error('加载最近域名失败:', error)
	}
}

// 加载邀请码
const loadInviteCode = async () => {
	try {
		const response = await apiGet('/api/user/invite/mycode', { token: authStore.token })
		if (response.data && response.data.hasCode) {
			inviteCode.value = response.data.code
		}
	} catch (error) {
		console.error('加载邀请码失败:', error)
	}
}

// 加载用户信息
const loadUserInfo = async () => {
	try {
		const response = await apiGet('/api/user/info', { token: authStore.token })
		if (response.data) {
			isVerified.value = response.data.isVerified
		}
	} catch (error) {
		console.error('加载用户信息失败:', error)
	}
}

// 加载系统设置
const loadSystemSettings = async () => {
	try {
		const response = await apiGet('/api/user/settings', { token: authStore.token })
		if (response.data) {
			baseCost.value = parseInt(response.data.domain_cost_points) || 10
			userStats.value.maxDomains = parseInt(response.data.max_domains_per_user) || 5
		}
	} catch (error) {
		console.error('加载系统设置失败:', error)
		// 如果接口失败，使用默认值
		baseCost.value = 10
		userStats.value.maxDomains = 5
	}
}

// 处理快速申请
const handleQuickApply = async () => {
	if (!canApply.value) {
		ElMessage.warning('请填写完整的申请信息')
		return
	}

	if (!isVerified.value) {
		ElMessage.warning('请先完成实名认证')
		router.push('/user/profile')
		return
	}

	isApplying.value = true
	try {
		const response = await apiPost('/api/user/domains/apply', {
			zoneId: quickApply.value.zoneId,
			prefix: quickApply.value.prefix.trim(),
			type: 'A',
			value: quickApply.value.value.trim(),
			remark: '快速申请'
		}, { token: authStore.token })

		if (response.code === 0) {
			ElMessage.success('申请成功')
			// 清空表单
			quickApply.value = { zoneId: '', prefix: '', value: '' }
			// 重新加载数据
			await Promise.all([
				loadUserStats(),
				loadDomainStats(),
				loadRecentDomains()
			])
		} else {
			ElMessage.error(response.message || '申请失败')
		}
	} catch (error) {
		ElMessage.error(error.message || '申请失败')
		console.error('申请域名失败:', error)
	} finally {
		isApplying.value = false
	}
}

// 格式化时间
const formatTime = (timeStr) => {
	if (!timeStr) return '未知'
	const date = new Date(timeStr)
	const now = new Date()
	const diff = now - date

	if (diff < 60000) return '刚刚'
	if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
	if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
	return date.toLocaleDateString()
}

// 获取状态样式类
const getStatusClass = (status) => {
	switch (status) {
		case 'ACTIVE': return 'success'
		case 'PENDING': return 'warning'
		case 'FAILED': return 'danger'
		default: return ''
	}
}

// 获取记录类型（暂时显示默认值，需要根据实际API调整）
const getRecordType = (domain) => {
	// 这里可以根据domain的其他字段来判断记录类型
	// 暂时返回默认值，后续需要从API获取
	return 'A'
}

// 获取记录值（暂时显示默认值，需要根据实际API调整）
const getRecordValue = (domain) => {
	// 这里可以根据domain的其他字段来获取记录值
	// 暂时返回默认值，后续需要从API获取
	return '1.2.3.4'
}

// 初始化数据
const initData = async () => {
	isLoading.value = true
	try {
		await Promise.all([
			loadUserStats(),
			loadDomainStats(),
			loadAvailableZones(),
			loadRecentDomains(),
			loadInviteCode(),
			loadSystemSettings(),
			loadUserInfo()
		])
	} catch (error) {
		console.error('初始化数据失败:', error)
	} finally {
		isLoading.value = false
	}
}

onMounted(() => {
	initData()
})
</script>
<style scoped>
.dashboard-container {
	padding: 20px;
}

.empty-state {
	text-align: center;
	padding: 40px 20px;
	color: #64748b;
}

.empty-state p {
	margin: 0;
	font-size: 14px;
}

.badge {
	padding: 4px 8px;
	border-radius: 4px;
	font-size: 12px;
	font-weight: 500;
}

.badge.success {
	background-color: #dcfce7;
	color: #166534;
}

.badge.warning {
	background-color: #fef3c7;
	color: #92400e;
}

.badge.danger {
	background-color: #fee2e2;
	color: #dc2626;
}

.btn:disabled {
	background-color: #9ca3af;
	cursor: not-allowed;
}

.btn:disabled:hover {
	background-color: #9ca3af;
}
</style>
