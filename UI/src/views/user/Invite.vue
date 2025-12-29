<template>
	<div class="invite-container" v-loading="isLoading">
		<div class="card">
			<div class="card-header">
				<h3>我的邀请码</h3>
				<button class="btn primary" @click="generateInviteCode" :disabled="isGenerating">
					{{ isGenerating ? '生成中...' : '生成/重置' }}
				</button>
			</div>

			<div v-if="inviteInfo.hasCode" class="invite-section">
				<div class="invite-code-display">
					<div class="code-input-group">
						<input class="input code-input" :value="inviteInfo.code" readonly>
					</div>
					<button class="btn outline" @click="copyInviteCode">复制</button>
					<button class="btn primary" @click="copyInviteLink">复制邀请链接</button>
				</div>

				<div class="invite-stats">
					<div class="stat-item">
						<span class="label">使用次数：</span>
						<span class="value">{{ inviteInfo.usedCount || 0 }} / {{ inviteInfo.maxUses || '∞' }}</span>
					</div>
					<div class="stat-item">
						<span class="label">状态：</span>
						<span class="badge" :class="getStatusClass(inviteInfo.status)">{{ inviteInfo.status }}</span>
					</div>
					<div class="stat-item" v-if="inviteInfo.expiredAt">
						<span class="label">过期时间：</span>
						<span class="value" :class="getExpiredTimeClass(inviteInfo.expiredAt)">{{
							formatExpiredTime(inviteInfo.expiredAt) }}</span>
					</div>
				</div>
			</div>

			<div v-else class="no-invite-code">
				<p>您还没有邀请码，点击上方按钮生成一个</p>
			</div>

			<p class="invite-tip">新用户使用你的邀请码注册，你与对方均可获得{{ systemSettings.inviterPoints }}点积分。</p>
		</div>

		<div class="card" style="margin-top:16px;">
			<div class="card-header">
				<h3>邀请明细</h3>
				<button class="btn outline" @click="refreshInviteDetails">刷新</button>
			</div>

			<div v-if="inviteDetails.length === 0 && !isLoading" class="empty-state">
				<svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor" style="color: #9ca3af;">
					<path
						d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
				</svg>
				<p>暂无邀请记录</p>
			</div>

			<table class="table" v-else>
				<thead>
					<tr>
						<th>用户</th>
						<th>时间</th>
						<th>奖励积分</th>
					</tr>
				</thead>
				<tbody>
					<tr v-for="detail in inviteDetails" :key="detail.id">
						<td data-label="用户">{{ detail.username || '未知用户' }}</td>
						<td data-label="时间">{{ formatTime(detail.createdAt) }}</td>
						<td data-label="奖励积分">+{{ detail.points || 0 }}</td>
					</tr>
				</tbody>
			</table>
		</div>

		<div class="card" style="margin-top:16px;">
			<div class="card-header" style="margin-bottom: 0;">
				<h3>{{ myInviter.hasInviter ? '邀请人' : '补填邀请码' }}</h3>
			</div>
			<div class="invite-bind" v-if="!myInviter.hasInviter">
				<div class="form"
					style="margin-top: 12px; display: flex; gap: 8px; flex-wrap: wrap; align-items: center;">
					<input class="input" v-model="bindCode" placeholder="请输入邀请人邀请码" style="max-width: 260px;">
					<button class="btn primary" @click="handleBindInvite" :disabled="binding">
						{{ binding ? '绑定中...' : '绑定并领取积分' }}
					</button>
				</div>
				<p class="invite-tip">补填后将为邀请人与您各发放{{ systemSettings.inviterPoints }} / {{ systemSettings.inviteePoints
				}}积分。</p>
			</div>
			<div class="invite-bind" v-else>
				<p>已绑定邀请人：<b>{{ myInviter.inviterUsername || ('#' + myInviter.inviterId) }}</b></p>
			</div>
		</div>
	</div>
</template>
<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth.js'
import { apiGet, apiPost } from '@/utils/api.js'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()

// 响应式数据
const isLoading = ref(false)
const isGenerating = ref(false)
const inviteInfo = ref({
	hasCode: false,
	code: '',
	maxUses: 0,
	usedCount: 0,
	status: '',
	expiredAt: null
})
const inviteDetails = ref([])
const myInviter = ref({ hasInviter: false, inviterId: null, inviterUsername: '' })
const bindCode = ref('')
const binding = ref(false)

// 系统设置
const systemSettings = ref({
	inviterPoints: 3,  // 邀请者获得积分
	inviteePoints: 3   // 被邀请者获得积分
})

// 加载系统设置
const loadSystemSettings = async () => {
	try {
		if (!authStore.token) {
			console.error('用户token不存在')
			// 使用默认值
			systemSettings.value = {
				inviterPoints: 3,
				inviteePoints: 3
			}
			return
		}

		const response = await apiGet('/api/user/settings', { token: authStore.token })
		if (response.data) {
			// 从后端获取实际的系统设置
			systemSettings.value = {
				inviterPoints: parseInt(response.data.inviter_points) || 3,
				inviteePoints: parseInt(response.data.invitee_points) || 3
			}
			// 调试日志已移除
		}
	} catch (error) {
		console.error('加载系统设置失败:', error)
		// 如果接口失败，使用默认值
		systemSettings.value = {
			inviterPoints: 3,
			inviteePoints: 3
		}
		// 调试日志已移除
	}
}

// 加载邀请码信息
const loadInviteCode = async () => {
	try {
		const response = await apiGet('/api/user/invite/mycode', { token: authStore.token })
		if (response.data) {
			inviteInfo.value = response.data
		}
	} catch (error) {
		console.error('加载邀请码失败:', error)
		ElMessage.error('加载邀请码失败')
	}
}

// 生成/重置邀请码
const generateInviteCode = async () => {
	isGenerating.value = true
	try {
		const response = await apiPost('/api/user/invite/generate', {
			maxUses: 10,
			validDays: 30
		}, { token: authStore.token })

		if (response.code === 200) {
			ElMessage.success('邀请码生成成功')
			// 重新加载邀请码信息
			await loadInviteCode()
		} else {
			ElMessage.error(response.message || '生成失败')
		}
	} catch (error) {
		ElMessage.error('生成邀请码失败')
		console.error('生成邀请码失败:', error)
	} finally {
		isGenerating.value = false
	}
}

// 复制邀请码
const copyInviteCode = async () => {
	try {
		await navigator.clipboard.writeText(inviteInfo.value.code)
		ElMessage.success('邀请码已复制到剪贴板')
	} catch (error) {
		// 降级方案
		const textArea = document.createElement('textarea')
		textArea.value = inviteInfo.value.code
		document.body.appendChild(textArea)
		textArea.select()
		document.execCommand('copy')
		document.body.removeChild(textArea)
		ElMessage.success('邀请码已复制到剪贴板')
	}
}

// 复制邀请链接
const copyInviteLink = async () => {
	const inviteLink = `${window.location.origin}/user/register?invite=${inviteInfo.value.code}`
	try {
		await navigator.clipboard.writeText(inviteLink)
		ElMessage.success('邀请链接已复制到剪贴板')
	} catch (error) {
		// 降级方案
		const textArea = document.createElement('textarea')
		textArea.value = inviteLink
		document.body.appendChild(textArea)
		textArea.select()
		document.execCommand('copy')
		document.body.removeChild(textArea)
		ElMessage.success('邀请链接已复制到剪贴板')
	}
}

// 加载邀请明细
const loadInviteDetails = async () => {
	try {
		if (!authStore.token) {
			console.error('用户token不存在')
			return
		}

		const response = await apiGet('/api/user/invite/details', { token: authStore.token })
		if (response.data) {
			inviteDetails.value = response.data.list || []
			// 调试日志已移除
		}
	} catch (error) {
		console.error('加载邀请明细失败:', error)
		ElMessage.error('加载邀请明细失败')
		// 清空数据
		inviteDetails.value = []
	}
}

// 加载邀请人信息
const loadMyInviter = async () => {
	try {
		const resp = await apiGet('/api/user/invite/my-inviter', { token: authStore.token })
		if (resp.data) {
			myInviter.value = resp.data
		}
	} catch (e) {
		console.error('加载邀请人信息失败:', e)
	}
}

// 绑定邀请码
const handleBindInvite = async () => {
	if (!bindCode.value.trim()) {
		ElMessage.warning('请输入邀请码')
		return
	}
	binding.value = true
	try {
		const resp = await apiPost('/api/user/invite/bind', { inviteCode: bindCode.value.trim() }, { token: authStore.token })
    if (resp.code === 200) {
      ElMessage.success('绑定成功，积分已发放')
      bindCode.value = ''
			await loadMyInviter()
		} else {
			ElMessage.error(resp.message || '绑定失败')
		}
	} catch (e) {
		ElMessage.error(e.message || '绑定失败')
	} finally {
		binding.value = false
	}
}

// 刷新邀请明细
const refreshInviteDetails = () => {
	loadInviteDetails()
}

// 格式化时间（相对时间，用于显示多久前）
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

// 格式化过期时间（绝对时间，用于显示具体日期时间）
const formatExpiredTime = (timeStr) => {
	if (!timeStr) return '永不过期'
	const date = new Date(timeStr)
	const now = new Date()

	// 检查是否已过期
	if (date < now) {
		return `已过期 (${date.toLocaleString()})`
	}

	// 计算剩余时间
	const diff = date - now
	const days = Math.floor(diff / (1000 * 60 * 60 * 24))
	const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
	const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))

	if (days > 0) {
		return `${days}天${hours}小时后过期 (${date.toLocaleString()})`
	} else if (hours > 0) {
		return `${hours}小时${minutes}分钟后过期 (${date.toLocaleString()})`
	} else if (minutes > 0) {
		return `${minutes}分钟后过期 (${date.toLocaleString()})`
	} else {
		return `即将过期 (${date.toLocaleString()})`
	}
}

// 获取状态样式类
const getStatusClass = (status) => {
	switch (status) {
		case 'ACTIVE': return 'success'
		case 'EXPIRED': return 'danger'
		case 'DISABLED': return 'warning'
		default: return ''
	}
}

// 获取过期时间样式类
const getExpiredTimeClass = (timeStr) => {
	if (!timeStr) return ''
	const date = new Date(timeStr)
	const now = new Date()

	if (date < now) {
		return 'expired'
	}

	// 计算剩余时间
	const diff = date - now
	const days = Math.floor(diff / (1000 * 60 * 60 * 24))
	const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))

	if (days <= 1) {
		return 'expiring-soon'
	} else if (days <= 7) {
		return 'expiring-week'
	}

	return ''
}

// 初始化数据
const initData = async () => {
	isLoading.value = true
	try {
		await Promise.all([
			loadSystemSettings(),
			loadInviteCode(),
			loadInviteDetails(),
			loadMyInviter()
		])
	} catch (error) {
		console.error('初始化数据失败:', error)
	} finally {
		isLoading.value = false
	}
}

// 页面可见性变化处理
const handleVisibilityChange = () => {
	if (document.visibilityState === 'visible') {
		initData()
	}
}

// 监听用户信息变化
watch(() => authStore.user, (newUser) => {
	if (newUser) {
		// 如果用户信息更新（例如实名认证状态变化），重新加载数据
		initData()
	}
}, { deep: true })

onMounted(() => {
	initData()
	document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
	document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>
<style scoped>
.invite-container {
	padding: 20px;
}

.card-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 20px;
}

.invite-section {
	margin-bottom: 20px;
}

.invite-code-display {
	display: flex;
	gap: 12px;
	align-items: center;
	margin-bottom: 16px;
	flex-wrap: wrap;
}

.code-input-group {
	display: flex;
	gap: 8px;
	align-items: center;
}

.code-input {
	font-family: 'Courier New', monospace;
	font-weight: 600;
	letter-spacing: 1px;
	text-align: center;
}

.invite-stats {
	display: flex;
	gap: 24px;
	flex-wrap: wrap;
	margin-bottom: 16px;
}

.stat-item {
	display: flex;
	align-items: center;
	gap: 8px;
}

.stat-item .label {
	font-size: 14px;
	color: #64748b;
}

.stat-item .value {
	font-size: 14px;
	font-weight: 500;
	color: #151717;
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

/* 过期时间样式 */
.value.expired {
	color: #dc2626;
	font-weight: 600;
}

.value.expiring-soon {
	color: #f59e0b;
	font-weight: 600;
}

.value.expiring-week {
	color: #f97316;
	font-weight: 500;
}

.no-invite-code {
	text-align: center;
	padding: 40px 20px;
	color: #64748b;
}

.no-invite-code p {
	margin: 0;
	font-size: 14px;
}

.invite-tip {
	font-size: 14px;
	color: #64748b;
	margin: 0;
	line-height: 1.5;
}

.empty-state {
	text-align: center;
	padding: 60px 20px;
	color: #64748b;
}

.empty-state svg {
	margin-bottom: 16px;
}

.empty-state p {
	margin: 0;
	font-size: 16px;
}

.btn:disabled {
	background-color: #9ca3af;
	cursor: not-allowed;
}

.btn:disabled:hover {
	background-color: #9ca3af;
}

/* 响应式设计 */
@media (max-width: 768px) {
	.invite-code-display {
		flex-direction: column;
		align-items: stretch;
	}

	.code-input-group {
		width: 100%;
	}

	.code-input {
		flex: 1;
	}

	.invite-stats {
		flex-direction: column;
		gap: 12px;
	}
}
</style>
