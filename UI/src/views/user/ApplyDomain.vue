<template>
	<div class="apply-domain-container" v-loading="isLoading">
		<!-- SEO组件 -->
		<SEOHead pageName="applyDomain" />
		<div class="card">
			<div class="card-header">
				<h3>申请二级域名</h3>
				<button class="btn outline" @click="refreshZones">刷新域名列表</button>
			</div>

			<div class="form">
				<!-- 主域名和子域名前缀在同一行 -->
				<div class="grid cols-2">
					<div class="input-row">
						<label class="label">选择主域名（可分发）</label>
						<select class="select" v-model="formData.zoneId" @change="onZoneChange">
							<option value="">请选择主域名</option>
							<option v-for="zone in availableZones" :key="zone.id" :value="zone.id">
								{{ zone.name }} {{ zone.enabled ? '(可用)' : '(不可用)' }}
							</option>
						</select>
					</div>

					<div class="input-row">
						<label class="label">子域名前缀</label>
						<div class="prefix-input-group">
							<input class="input" v-model="formData.prefix" placeholder="例如：app"
								@input="checkAvailability">
							<span class="domain-suffix" v-if="selectedZone">.{{ selectedZone.name }}</span>
						</div>
						<div class="availability-status" v-if="availabilityStatus">
							<span class="status" :class="availabilityStatus.available ? 'available' : 'unavailable'">
								{{ availabilityStatus.available ? '✓ 可用' : '✗ 不可用' }}
							</span>
							<span class="reason" v-if="availabilityStatus.reason">{{ availabilityStatus.reason }}</span>
						</div>
					</div>
				</div>

				<!-- 记录类型、记录值、TTL和备注在同一行 -->
				<div class="grid cols-4">
					<div class="input-row">
						<label class="label">记录类型</label>
						<select class="select" v-model="formData.type" @change="onTypeChange">
							<option value="A">A (IPv4)</option>
							<option value="AAAA">AAAA (IPv6)</option>
							<option value="CNAME">CNAME</option>
							<option value="TXT">TXT</option>
						</select>
					</div>
					<div class="input-row">
						<label class="label">记录值</label>
						<input class="input" v-model="formData.value" :placeholder="getValuePlaceholder()">
					</div>
					<div class="input-row">
						<label class="label">TTL</label>
						<input class="input" v-model.number="formData.ttl" type="number"
							:placeholder="`默认 ${defaultTtl}`">
					</div>
					<div class="input-row">
						<label class="label">备注</label>
						<textarea class="textarea compact" v-model="formData.remark" placeholder="用途说明，方便管理"></textarea>
					</div>
				</div>

				<div class="cost-info">
					<div class="cost-item">
						<span class="label">消耗积分：</span>
						<span class="value">{{ calculateCost() }} 积分</span>
					</div>
					<div class="cost-item">
						<span class="label">当前余额：</span>
						<span class="value">{{ userBalance }} 积分</span>
					</div>
					<div class="cost-item" v-if="userBalance < calculateCost()">
						<span class="warning">⚠️ 积分不足，请先充值</span>
					</div>
				</div>

				<div class="form-actions">
					<button class="btn primary" @click="submitApplication" :disabled="!canSubmit || isSubmitting">
						{{ isSubmitting ? '提交中...' : `提交申请（扣除 ${calculateCost()} 积分）` }}
					</button>
					<router-link class="btn outline" to="/user/dashboard">返回</router-link>
				</div>
			</div>
		</div>
	</div>
</template>
<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import { apiGet, apiPost } from '@/utils/api.js'
import { ElMessage } from 'element-plus'
import SEOHead from '@/components/SEOHead.vue'

const router = useRouter()
const authStore = useAuthStore()

// 响应式数据
const isLoading = ref(false)
const isSubmitting = ref(false)
const isVerified = ref(false)
const availableZones = ref([])
const userBalance = ref(0)
const defaultTtl = ref(120)
const baseCost = ref(10) // 根据API文档，默认应该是10分，不是5分

// 表单数据
const formData = ref({
	zoneId: '',
	prefix: '',
	type: 'A',
	value: '',
	ttl: 120,
	remark: ''
})

// 可用性检查
const availabilityStatus = ref(null)
const checkTimeout = ref(null)

// 计算属性
const selectedZone = computed(() => {
	return availableZones.value.find(zone => zone.id == formData.value.zoneId)
})

const canSubmit = computed(() => {
	return formData.value.zoneId &&
		formData.value.prefix.trim() &&
		formData.value.value.trim() &&
		availabilityStatus.value?.available &&
		userBalance.value >= calculateCost()
})

// 加载可用域名
const loadZones = async () => {
	try {
		if (!authStore.token) {
			console.error('用户token不存在')
			ElMessage.error('请先登录')
			return
		}

		const response = await apiGet('/api/zones', { token: authStore.token })
		if (response.data) {
			availableZones.value = response.data.filter(zone => zone.enabled)
		}
	} catch (error) {
		console.error('加载域名列表失败:', error)
		ElMessage.error('加载域名列表失败: ' + (error.message || '未知错误'))
	}
}

// 加载用户积分
const loadUserBalance = async () => {
	try {
		if (!authStore.token) {
			console.error('用户token不存在')
			return
		}

		const response = await apiGet('/api/user/points', { token: authStore.token })
		if (response.data) {
			userBalance.value = response.data.balance || 0
		}
	} catch (error) {
		console.error('加载用户积分失败:', error)
		ElMessage.error('加载用户积分失败: ' + (error.message || '未知错误'))
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
		if (!authStore.token) {
			console.error('用户token不存在')
			// 使用默认值
			defaultTtl.value = 120
			baseCost.value = 10
			return
		}

		const response = await apiGet('/api/user/settings', { token: authStore.token })
		if (response.data) {
			// 从后端获取实际的系统设置
			defaultTtl.value = parseInt(response.data.default_ttl) || 120
			baseCost.value = parseInt(response.data.domain_cost_points) || 10
			// 调试日志已移除
		}
	} catch (error) {
		console.error('加载系统设置失败:', error)
		// 如果接口失败，使用默认值
		defaultTtl.value = 120
		baseCost.value = 10
		// 调试日志已移除
	}
}

// 检查域名可用性
const checkAvailability = async () => {
	if (!formData.value.prefix.trim() || !selectedZone.value) {
		availabilityStatus.value = null
		return
	}

	// 防抖处理
	if (checkTimeout.value) {
		clearTimeout(checkTimeout.value)
	}

	checkTimeout.value = setTimeout(async () => {
		try {
			if (!authStore.token) {
				console.error('用户token不存在')
				availabilityStatus.value = {
					available: false,
					reason: '请先登录'
				}
				return
			}

			const fullDomain = `${formData.value.prefix.trim()}.${selectedZone.value.name}`
			const response = await apiGet('/api/domains/search', {
				token: authStore.token,
				params: { prefix: formData.value.prefix.trim() }
			})

			if (response.data) {
				const domainInfo = response.data.find(d => d.domain === fullDomain)
				availabilityStatus.value = {
					available: domainInfo?.available || false,
					reason: domainInfo?.reason || null
				}
			}
		} catch (error) {
			console.error('检查域名可用性失败:', error)
			availabilityStatus.value = {
				available: false,
				reason: '检查失败: ' + (error.message || '未知错误')
			}
		}
	}, 500)
}

// 域名选择变化
const onZoneChange = () => {
	formData.value.prefix = ''
	availabilityStatus.value = null
	if (selectedZone.value) {
		formData.value.ttl = defaultTtl.value
	}
}

// 记录类型变化
const onTypeChange = () => {
	formData.value.value = ''
}

// 获取记录值占位符
const getValuePlaceholder = () => {
	switch (formData.value.type) {
		case 'A': return '例如：1.2.3.4'
		case 'AAAA': return '例如：2001:db8::1'
		case 'CNAME': return '例如：target.example.com'
		case 'TXT': return '例如：v=spf1 include:_spf.google.com ~all'
		default: return '请输入记录值'
	}
}

// 计算消耗积分 - 按照API文档规则
const calculateCost = () => {
	if (!selectedZone.value) return baseCost.value

	const domain = selectedZone.value.name.toLowerCase()
	let multiplier = 1.0

	// 根据API文档规则计算倍数
	if (domain.endsWith('.cn') || domain.endsWith('.com')) {
		multiplier = 2.0  // .cn / .com：2.0倍
	} else if (domain.endsWith('.top')) {
		multiplier = 1.5  // .top：1.5倍
	} else {
		multiplier = 1.0  // 其它：1.0倍
	}

	const cost = Math.floor(baseCost.value * multiplier)
	// 调试日志已移除

	return cost
}

// 提交申请
const submitApplication = async () => {
	if (!canSubmit.value) {
		ElMessage.warning('请填写完整的申请信息')
		return
	}

	if (!authStore.token) {
		ElMessage.error('请先登录')
		return
	}

	if (!isVerified.value) {
		ElMessage.warning('请先完成实名认证')
		router.push('/user/profile')
		return
	}

	isSubmitting.value = true
	try {
		const response = await apiPost('/api/user/domains/apply', {
			zoneId: formData.value.zoneId,
			prefix: formData.value.prefix.trim(),
			type: formData.value.type,
			value: formData.value.value.trim(),
			ttl: formData.value.ttl || defaultTtl.value,
			remark: formData.value.remark.trim()
		}, { token: authStore.token })

		if (response.code === 0) {
			ElMessage.success('域名申请成功')
			// 清空表单
			formData.value = {
				zoneId: '',
				prefix: '',
				type: 'A',
				value: '',
				ttl: defaultTtl.value,
				remark: ''
			}
			availabilityStatus.value = null
			// 重新加载积分
			await loadUserBalance()
		} else {
			ElMessage.error(response.message || '申请失败')
		}
	} catch (error) {
		ElMessage.error(error.message || '申请失败')
		console.error('申请域名失败:', error)
	} finally {
		isSubmitting.value = false
	}
}

// 刷新域名列表
const refreshZones = async () => {
	await loadZones()
	ElMessage.success('域名列表已刷新')
}

// 监听前缀变化，自动检查可用性
watch(() => formData.value.prefix, () => {
	checkAvailability()
})

// 初始化数据
const initData = async () => {
	isLoading.value = true
	try {
		// 检查用户是否已登录
		if (!authStore.isLoggedIn || !authStore.token) {
			ElMessage.error('请先登录')
			// 可以在这里跳转到登录页
			// router.push('/user/login')
			return
		}

		await Promise.all([
			loadZones(),
			loadUserBalance(),
			loadSystemSettings(),
			loadUserInfo()
		])
	} catch (error) {
		console.error('初始化数据失败:', error)
		ElMessage.error('初始化数据失败: ' + (error.message || '未知错误'))
	} finally {
		isLoading.value = false
	}
}

onMounted(() => {
	initData()
})
</script>
<style scoped>
.apply-domain-container {
	padding: 20px;
	min-height: 100vh;
	display: flex;
	flex-direction: column;
}

.card {
	background: rgba(247, 250, 250, 0.685);
	flex: 1;
	max-width: 1200px;
	margin: 0 auto;
	width: 100%;
	transition: all 0.3s ease;
}

.card-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 20px;
}

.form {
	display: flex;
	flex-direction: column;
	gap: 24px;
	max-width: 100%;
	padding: 0 8px;
}

.grid {
	display: grid;
	gap: 20px;
	align-items: start;
}

.grid.cols-2 {
	grid-template-columns: 1fr 1fr;
}

.grid.cols-4 {
	grid-template-columns: 1fr 1.5fr 0.8fr 1.2fr;
}

.input-row {
	display: flex;
	flex-direction: column;
	gap: 8px;
}

.label {
	font-size: 14px;
	font-weight: 500;
	color: #374151;
	white-space: nowrap;
}

.prefix-input-group {
	display: flex;
	align-items: center;
	gap: 8px;
}

.domain-suffix {
	font-size: 14px;
	color: #64748b;
	font-weight: 500;
	white-space: nowrap;
}

.availability-status {
	margin-top: 4px;
	display: flex;
	align-items: center;
	gap: 8px;
}

.status {
	font-size: 12px;
	font-weight: 500;
	padding: 2px 6px;
	border-radius: 4px;
}

.status.available {
	background-color: #dcfce7;
	color: #166534;
}

.status.unavailable {
	background-color: #fee2e2;
	color: #dc2626;
}

.reason {
	font-size: 12px;
	color: #64748b;
}

.textarea.compact {
	min-height: 60px;
	max-height: 60px;
	resize: vertical;
}

.cost-info {
	background-color: #f8fafc;
	border: 1px solid #e2e8f0;
	border-radius: 8px;
	padding: 16px;
	display: flex;
	flex-direction: column;
	gap: 8px;
}

.cost-item {
	display: flex;
	justify-content: space-between;
	align-items: center;
}

.cost-item .label {
	font-size: 14px;
	color: #64748b;
}

.cost-item .value {
	font-size: 14px;
	font-weight: 600;
	color: #151717;
}

.cost-item .warning {
	font-size: 14px;
	color: #dc2626;
	font-weight: 500;
}

.form-actions {
	display: flex;
	gap: 12px;
	justify-content: flex-end;
	margin-top: 20px;
}

.btn:disabled {
	background-color: #9ca3af;
	cursor: not-allowed;
}

.btn:disabled:hover {
	background-color: #9ca3af;
}

/* 响应式设计 */
@media (min-width: 1400px) {
	.apply-domain-container {
		padding: 40px 60px;
	}

	.card {
		padding: 32px;
	}

	.form {
		gap: 32px;
	}

	.grid {
		gap: 24px;
	}
}

@media (min-width: 1200px) and (max-width: 1399px) {

	.card {
		padding: 28px;
	}

	.form {
		gap: 28px;
	}
}

@media (min-width: 992px) and (max-width: 1199px) {
	.apply-domain-container {
		padding: 24px 32px;
	}

	.card {
		padding: 24px;
	}
}

@media (max-width: 1024px) {
	.grid.cols-4 {
		grid-template-columns: 1fr 1fr;
		gap: 16px;
	}

	.grid.cols-2 {
		gap: 16px;
	}
}

@media (max-width: 768px) {
	.apply-domain-container {
		padding: 16px;
	}

	.card {
		padding: 20px;
	}

	.form {
		gap: 20px;
		padding: 0;
	}

	.grid.cols-2,
	.grid.cols-4 {
		grid-template-columns: 1fr;
		gap: 16px;
	}

	.form-actions {
		flex-direction: column;
	}

	.prefix-input-group {
		flex-direction: column;
		align-items: stretch;
	}

	.domain-suffix {
		text-align: center;
		padding: 8px;
		background-color: #f3f4f6;
		border-radius: 4px;
	}

	.cost-info {
		padding: 12px;
	}

	.cost-item {
		flex-direction: column;
		align-items: flex-start;
		gap: 4px;
	}

	.textarea.compact {
		min-height: 80px;
		max-height: 120px;
	}
}

@media (max-width: 480px) {
	.apply-domain-container {
		padding: 12px;
	}

	.card {
		padding: 16px;
	}

	.form {
		gap: 16px;
	}

	.grid {
		gap: 12px;
	}

	.card-header {
		flex-direction: column;
		gap: 12px;
		align-items: stretch;
	}

	.form-actions {
		flex-direction: column;
		gap: 8px;
	}
}
</style>