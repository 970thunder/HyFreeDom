<template>
	<div class="apply-domain-container" v-loading="isLoading">
		<!-- SEO组件 -->
		<SEOHead pageName="applyDomain" />
		<div class="card">
			<div class="card-header">
				<h3>域名申请</h3>
				<button class="btn outline" @click="refreshZones">刷新域名列表</button>
			</div>

			<!-- 选项卡导航 -->
			<div class="tabs">
				<div class="tab-item" :class="{ active: activeTab === 'standard' }" @click="switchTab('standard')">
					普通域名申请
				</div>
				<div class="tab-item" :class="{ active: activeTab === 'exclusive' }" @click="switchTab('exclusive')">
					专属域名申请
					<span class="hot-tag">最新热门</span>
				</div>
			</div>

			<!-- 普通域名申请表单 -->
			<div class="form" v-if="activeTab === 'standard'">
				<!-- 主域名和子域名前缀在同一行 -->
				<div class="grid cols-2">
					<div class="input-row">
						<label class="label">选择主域名（可分发）</label>
						<select class="select" v-model="formData.zoneId" @change="onZoneChange">
							<option value="">请选择主域名</option>
							<option v-for="zone in standardZones" :key="zone.id" :value="zone.id">
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

			<!-- 专属域名申请表单 (NS托管) -->
			<div class="form" v-if="activeTab === 'exclusive'">
				<!-- 步骤条 -->
				<div class="steps">
					<div class="step" :class="{ active: exclusiveStep >= 1, completed: exclusiveStep > 1 }">
						<div class="step-number">1</div>
						<div class="step-title">授权校验</div>
					</div>
					<div class="step-line" :class="{ active: exclusiveStep > 1 }"></div>
					<div class="step" :class="{ active: exclusiveStep >= 2 }">
						<div class="step-number">2</div>
						<div class="step-title">NS设置</div>
					</div>
				</div>

				<!-- 步骤1: 授权校验 -->
				<div v-if="exclusiveStep === 1" class="step-content">
					<div class="grid cols-2">
						<div class="input-row">
							<div class="label-with-tooltip">
								<label class="label">选择主域名</label>
								<div class="tooltip-wrapper">
									<span class="tooltip-icon">?</span>
									<div class="tooltip-text">
										应广大需求希望能开放免费专属域名，自己管理DNS记录，站长购入一个新域名试运营，如果网站还有资金，未来还会继续运营下去😭</div>
								</div>
							</div>
							<select class="select" v-model="exclusiveData.zoneId" @change="onExclusiveZoneChange">
								<option value="">请选择主域名</option>
								<option v-for="zone in exclusiveZones" :key="zone.id" :value="zone.id">
									{{ zone.name }}
								</option>
							</select>
						</div>

						<div class="input-row">
							<label class="label">专属域名前缀 (申请目标)</label>
							<div class="prefix-input-group">
								<input class="input" v-model="exclusiveData.prefix" placeholder="例如：mybrand"
									@input="checkExclusiveAvailability">
								<span class="domain-suffix" v-if="exclusiveSelectedZone">.{{ exclusiveSelectedZone.name
								}}</span>
							</div>
							<div class="availability-status" v-if="exclusiveAvailability">
								<span class="status"
									:class="exclusiveAvailability.available ? 'available' : 'unavailable'">
									{{ exclusiveAvailability.available ? '✓ 可用' : '✗ 不可用' }}
								</span>
								<span class="reason" v-if="exclusiveAvailability.reason">{{ exclusiveAvailability.reason
								}}</span>
							</div>
						</div>
					</div>

					<div class="verification-section" v-if="exclusiveData.zoneId && exclusiveData.prefix">
						<div class="alert info">
							<i class="icon">ℹ️</i>
							<span>专属域名申请消耗30积分，请设置以下TXT记录以进行授权校验（通常由服务商提供，如阿里云、腾讯云等）：</span>
						</div>

						<div class="grid cols-3">
							<div class="input-row">
								<label class="label">记录类型</label>
								<input class="input" value="TXT" disabled>
							</div>
							<div class="input-row">
								<label class="label">主机记录 (用于校验)</label>
								<input class="input" v-model="exclusiveData.txtHost" placeholder="例如：_dnsauth">
							</div>
							<div class="input-row">
								<label class="label">记录值 (用于校验)</label>
								<input class="input" v-model="exclusiveData.txtValue" placeholder="请输入校验用的TXT记录值">
							</div>
						</div>
					</div>

					<div class="form-actions">
						<button class="btn primary" @click="verifyAndNext" :disabled="!canVerify || isSubmitting">
							{{ isSubmitting ? '提交中...' : '提交TXT记录并下一步' }}
						</button>
					</div>
				</div>

				<!-- 步骤2: NS设置 -->
				<div v-if="exclusiveStep === 2" class="step-content">
					<div class="alert success">
						<i class="icon">✓</i>
						<span>授权校验通过！请设置您的NS记录以完成托管。</span>
					</div>

					<div class="grid cols-1">
						<div class="input-row">
							<label class="label">NS记录 1</label>
							<input class="input" v-model="exclusiveData.ns1" placeholder="例如：ns1.cloudflare.com">
						</div>
						<div class="input-row">
							<label class="label">NS记录 2</label>
							<input class="input" v-model="exclusiveData.ns2" placeholder="例如：ns2.cloudflare.com">
						</div>
					</div>

					<div class="cost-info">
						<div class="cost-item">
							<span class="label">专属域名费用：</span>
							<span class="value">20 积分 (总计30积分)</span>
						</div>
						<div class="cost-item">
							<span class="label">当前余额：</span>
							<span class="value">{{ userBalance }} 积分</span>
						</div>
						<div class="cost-item" v-if="userBalance < 20">
							<span class="warning">⚠️ 积分不足，请先充值</span>
						</div>
					</div>

					<div class="form-actions">
						<button class="btn outline" @click="handlePreviousStep">上一步</button>
						<button class="btn primary" @click="submitExclusive"
							:disabled="!canSubmitExclusive || isSubmitting">
							{{ isSubmitting ? '提交中...' : '确认开通（再扣除 20 积分）' }}
						</button>
					</div>
				</div>
			</div>
		</div>
	</div>
</template>
<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import { apiGet, apiPost, apiDelete } from '@/utils/api.js'
import { ElMessage, ElMessageBox } from 'element-plus'
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
const activeTab = ref('standard')
const exclusiveStep = ref(1)
const exclusiveData = ref({
	zoneId: '',
	prefix: '',
	txtHost: '',
	txtValue: '',
	ns1: '',
	ns2: ''
})
const exclusiveAvailability = ref(null)
const createdTxtDomainId = ref(null)

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

const standardZones = computed(() => {
	// 普通域名申请不显示 vivo50.today
	return availableZones.value.filter(zone => zone.name !== 'vivo50.today')
})

const exclusiveZones = computed(() => {
	// 专属域名只允许选择 vivo50.today
	return availableZones.value.filter(zone => zone.name === 'vivo50.today')
})

const exclusiveSelectedZone = computed(() => {
	return availableZones.value.find(zone => zone.id == exclusiveData.value.zoneId)
})

const canSubmit = computed(() => {
	return formData.value.zoneId &&
		formData.value.prefix.trim() &&
		formData.value.value.trim() &&
		availabilityStatus.value?.available &&
		userBalance.value >= calculateCost()
})

const canVerify = computed(() => {
	return exclusiveData.value.zoneId &&
		exclusiveData.value.prefix.trim() &&
		exclusiveData.value.txtHost.trim() &&
		exclusiveData.value.txtValue.trim() &&
		exclusiveAvailability.value?.available
})

const canSubmitExclusive = computed(() => {
	return exclusiveData.value.ns1.trim() &&
		exclusiveData.value.ns2.trim() &&
		userBalance.value >= 20
})

// 清理TXT记录
const cleanupTxtRecord = async () => {
	if (!createdTxtDomainId.value) return

	try {
		await apiDelete(`/api/user/domains/${createdTxtDomainId.value}`, { token: authStore.token })
		createdTxtDomainId.value = null
	} catch (error) {
		console.error('清理TXT记录失败:', error)
	}
}

// 切换Tab
const switchTab = async (tab) => {
	// 如果离开专属域名申请且在第二步，清理TXT记录
	if (activeTab.value === 'exclusive' && tab !== 'exclusive' && exclusiveStep.value === 2) {
		await cleanupTxtRecord()
	}

	activeTab.value = tab
	if (tab === 'exclusive') {
		exclusiveStep.value = 1
		exclusiveData.value = {
			zoneId: '',
			prefix: '',
			txtHost: '',
			txtValue: '',
			ns1: '',
			ns2: ''
		}
		exclusiveAvailability.value = null
		createdTxtDomainId.value = null
	}
}

// 专属域名 - 区域选择变化
const onExclusiveZoneChange = () => {
	exclusiveData.value.prefix = ''
	exclusiveAvailability.value = null
}

// 专属域名 - 检查可用性
const checkExclusiveAvailability = async () => {
	if (!exclusiveData.value.prefix.trim() || !exclusiveSelectedZone.value) {
		exclusiveAvailability.value = null
		return
	}

	if (checkTimeout.value) {
		clearTimeout(checkTimeout.value)
	}

	checkTimeout.value = setTimeout(async () => {
		try {
			const fullDomain = `${exclusiveData.value.prefix.trim()}.${exclusiveSelectedZone.value.name}`
			const response = await apiGet('/api/domains/search', {
				token: authStore.token,
				params: { prefix: exclusiveData.value.prefix.trim() }
			})

			if (response.data) {
				const domainInfo = response.data.find(d => d.domain === fullDomain)
				exclusiveAvailability.value = {
					available: domainInfo?.available || false,
					reason: domainInfo?.reason || null
				}
			}
		} catch (error) {
			console.error('检查域名可用性失败:', error)
			exclusiveAvailability.value = {
				available: false,
				reason: '检查失败: ' + (error.message || '未知错误')
			}
		}
	}, 500)
}

// 专属域名 - 返回上一步
const handlePreviousStep = async () => {
	await cleanupTxtRecord()
	exclusiveStep.value = 1
}

// 专属域名 - 校验并下一步 (创建TXT记录)
const verifyAndNext = async () => {
	if (!canVerify.value) return

	if (!authStore.token) {
		ElMessage.error('请先登录')
		return
	}

	try {
		await ElMessageBox.confirm(
			'第一步将扣除 10 积分用于创建校验记录，后续第二步将扣除 20 积分。如果中途撤销 TXT 记录，仅退还一半积分（5 积分）。请确认是否继续？',
			'积分扣除提示',
			{
				confirmButtonText: '确认继续',
				cancelButtonText: '我再想想',
				type: 'warning',
			}
		)

		isSubmitting.value = true
		// 调用API创建TXT记录
		const response = await apiPost('/api/user/domains/apply', {
			zoneId: exclusiveData.value.zoneId,
			prefix: exclusiveData.value.txtHost.trim(),
			type: 'TXT',
			value: exclusiveData.value.txtValue.trim(),
			ttl: 120, // TXT验证记录TTL通常较短
			remark: `专属域名验证 (申请: ${exclusiveData.value.prefix})`
		}, { token: authStore.token })

		if (response.code === 0) {
			// 保存创建的记录ID，以便后续清理
			if (response.data && response.data.id) {
				createdTxtDomainId.value = response.data.id
			}

			ElMessage.success('TXT记录创建成功，请前往服务商进行验证')
			exclusiveStep.value = 2
			// 刷新积分显示
			await loadUserBalance()
		} else {
			ElMessage.error(response.message || 'TXT记录创建失败')
		}
	} catch (error) {
		if (error !== 'cancel') {
			ElMessage.error(error.message || 'TXT记录创建失败')
		}
	} finally {
		isSubmitting.value = false
	}
}

// 专属域名 - 提交申请
const submitExclusive = async () => {
	if (!canSubmitExclusive.value) return
	if (!authStore.token) {
		ElMessage.error('请先登录')
		return
	}

	isSubmitting.value = true
	try {
		// 构造NS记录值
		const nsValue = `${exclusiveData.value.ns1.trim()} ${exclusiveData.value.ns2.trim()}`

		const response = await apiPost('/api/user/domains/apply', {
			zoneId: exclusiveData.value.zoneId,
			prefix: exclusiveData.value.prefix.trim(),
			type: 'NS',
			value: nsValue, // 发送合并的NS记录
			ttl: 86400, // NS记录通常TTL较长
			remark: `专属域名托管 (授权TXT: ${exclusiveData.value.txtValue})`
		}, { token: authStore.token })

		if (response.code === 0) {
			ElMessage.success('专属域名开通成功')
			createdTxtDomainId.value = null // 成功提交，清除ID避免被清理
			switchTab('standard') // 重置并返回
			await loadUserBalance()
		} else {
			ElMessage.error(response.message || '申请失败')
		}
	} catch (error) {
		ElMessage.error(error.message || '申请失败')
	} finally {
		isSubmitting.value = false
	}
}

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
		case 'A': return '例如IP地址：1.2.3.4'
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

// 页面可见性变化处理
const handleVisibilityChange = () => {
	if (document.visibilityState === 'visible') {
		initData()
	}
}

onMounted(() => {
	initData()
	document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
	document.removeEventListener('visibilitychange', handleVisibilityChange)
	if (createdTxtDomainId.value) {
		cleanupTxtRecord()
	}
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

.tabs {
	display: flex;
	gap: 20px;
	border-bottom: 1px solid #e5e7eb;
	margin-bottom: 24px;
}

.tab-item {
	padding: 12px 4px;
	cursor: pointer;
	color: #64748b;
	font-weight: 500;
	border-bottom: 2px solid transparent;
	transition: all 0.2s;
	position: relative;
}

.tab-item:hover {
	color: #2d79f3;
}

.tab-item.active {
	color: #2d79f3;
	border-bottom-color: #2d79f3;
}

.hot-tag {
	position: absolute;
	top: 0;
	right: -24px;
	background: linear-gradient(135deg, #ff7e5f 0%, #feb47b 100%);
	color: white;
	font-size: 10px;
	padding: 1px 6px;
	border-radius: 8px 8px 8px 0;
	transform: scale(0.85);
	font-weight: bold;
	box-shadow: 0 2px 4px rgba(255, 126, 95, 0.3);
	animation: pulse 2s infinite;
}

@keyframes pulse {
	0% {
		transform: scale(0.85);
	}

	50% {
		transform: scale(0.95);
	}

	100% {
		transform: scale(0.85);
	}
}

.label-with-tooltip {
	display: flex;
	align-items: center;
	gap: 6px;
}

.label-with-tooltip .label {
	margin-bottom: 0;
}

.tooltip-wrapper {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: center;
	cursor: help;
}

.tooltip-icon {
	width: 16px;
	height: 16px;
	background-color: #94a3b8;
	color: white;
	border-radius: 50%;
	font-size: 12px;
	display: flex;
	align-items: center;
	justify-content: center;
	font-weight: bold;
	transition: background-color 0.2s;
}

.tooltip-wrapper:hover .tooltip-icon {
	background-color: #2d79f3;
}

.tooltip-text {
	position: absolute;
	bottom: 100%;
	left: 100%;
	transform: translateX(10px) translateY(50%);
	background-color: #1e293b;
	color: white;
	padding: 8px 12px;
	border-radius: 6px;
	font-size: 12px;
	line-height: 1.5;
	width: 280px;
	text-align: left;
	visibility: hidden;
	opacity: 0;
	transition: all 0.2s;
	z-index: 1000;
	pointer-events: none;
	box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.tooltip-text::after {
	content: '';
	position: absolute;
	top: 50%;
	right: 100%;
	transform: translateY(-50%);
	border-width: 5px;
	border-style: solid;
	border-color: transparent #1e293b transparent transparent;
}

.tooltip-wrapper:hover .tooltip-text {
	visibility: visible;
	opacity: 1;
	transform: translateX(10px) translateY(-50%);
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

.grid.cols-3 {
	grid-template-columns: 1fr 1fr 1fr;
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

/* Tabs */
.tabs {
	display: flex;
	gap: 20px;
	border-bottom: 1px solid #e5e7eb;
	margin-bottom: 24px;
}

.tab-item {
	padding: 12px 4px;
	color: #64748b;
	font-weight: 500;
	cursor: pointer;
	border-bottom: 2px solid transparent;
	transition: all 0.2s;
}

.tab-item:hover {
	color: #151717;
}

.tab-item.active {
	color: #2d79f3;
	border-bottom-color: #2d79f3;
}

/* Steps */
.steps {
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 32px;
	gap: 12px;
}

.step {
	display: flex;
	align-items: center;
	gap: 8px;
	opacity: 0.5;
	transition: all 0.3s;
}

.step.active {
	opacity: 1;
}

.step-number {
	width: 28px;
	height: 28px;
	border-radius: 50%;
	background-color: #e5e7eb;
	color: #6b7280;
	display: flex;
	align-items: center;
	justify-content: center;
	font-weight: 600;
	font-size: 14px;
	transition: all 0.3s;
}

.step.active .step-number {
	background-color: #2d79f3;
	color: white;
}

.step.completed .step-number {
	background-color: #10b981;
	color: white;
}

.step-title {
	font-weight: 500;
	color: #374151;
}

.step-line {
	flex: 1;
	height: 2px;
	background-color: #e5e7eb;
	max-width: 100px;
	transition: all 0.3s;
}

.step-line.active {
	background-color: #2d79f3;
}

.step-content {
	display: flex;
	flex-direction: column;
	gap: 24px;
	animation: slideIn 0.3s ease;
}

.verification-section {
	background-color: #f8fafc;
	border-radius: 8px;
	padding: 20px;
	border: 1px solid #e2e8f0;
	display: flex;
	flex-direction: column;
	gap: 16px;
}

.alert {
	display: flex;
	align-items: center;
	gap: 12px;
	padding: 12px 16px;
	border-radius: 8px;
	font-size: 14px;
}

.alert.info {
	background-color: #eff6ff;
	color: #1e40af;
	border: 1px solid #dbeafe;
}

.alert.success {
	background-color: #ecfdf5;
	color: #047857;
	border: 1px solid #d1fae5;
}

.icon {
	font-style: normal;
}

@keyframes slideIn {
	from {
		opacity: 0;
		transform: translateY(10px);
	}

	to {
		opacity: 1;
		transform: translateY(0);
	}
}
</style>