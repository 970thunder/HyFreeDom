<template>
	<main class="main container">
		<div class="card">
			<div class="card-header">
				<h3>积分流水日志</h3>
				<div class="header-actions">
					<button class="btn outline" @click="loadTransactions" :disabled="isLoading">
						{{ isLoading ? '加载中...' : '刷新' }}
					</button>
				</div>
			</div>

			<div class="filters" style="margin-bottom:12px;">
				<div class="filter-group">
					<input class="input" style="max-width:240px;" placeholder="搜索用户名/邮箱" v-model="filters.keyword"
						@input="onSearchInput" />
					<select class="select" style="max-width:200px;" v-model="filters.type" @change="loadTransactions">
						<option value="">所有类型</option>
						<option value="REGISTER_GIFT">注册赠送</option>
						<option value="REAL_NAME_GIFT">实名认证赠送</option>
						<option value="INVITE_REWARD">邀请奖励</option>
						<option value="INVITE_CODE">使用邀请码</option>
						<option value="DOMAIN_REGISTER">注册域名</option>
						<option value="DOMAIN_RENEW">域名续费</option>
						<option value="ADMIN_ADJUST">管理员调整</option>
						<option value="CARD_REDEEM">卡密兑换</option>
						<option value="GITHUB_STAR_REWARD">GitHub Star奖励</option>
						<option value="DOMAIN_REFUND">域名退款</option>
					</select>
				</div>
			</div>

			<div class="table-container" v-loading="isLoading">
				<table class="table">
					<thead>
						<tr>
							<th>ID</th>
							<th>用户</th>
							<th>类型</th>
							<th>变动数量</th>
							<th>变动后余额</th>
							<th>备注</th>
							<th>时间</th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="item in transactions" :key="item.id">
							<td>{{ item.id }}</td>
							<td>
								<div class="user-info">
									<span class="username">{{ item.username || '未知用户' }}</span>
									<span class="email" v-if="item.email">({{ item.email }})</span>
								</div>
							</td>
							<td>
								<span class="badge" :class="getTypeClass(item.type)">
									{{ getTypeLabel(item.type) }}
								</span>
							</td>
							<td :class="item.changeAmount > 0 ? 'text-success' : 'text-danger'">
								{{ item.changeAmount > 0 ? '+' : '' }}{{ item.changeAmount }}
							</td>
							<td>{{ item.balanceAfter }}</td>
							<td>{{ item.remark }}</td>
							<td>{{ formatDate(item.createdAt) }}</td>
						</tr>
					</tbody>
				</table>

				<div v-if="!isLoading && transactions.length === 0" class="empty-state">
					<p>暂无数据</p>
				</div>

				<!-- 分页 -->
				<el-pagination v-if="!isLoading && total > 0" class="pagination" background
					layout="prev, pager, next, sizes, total" :total="total" :page-size="filters.size"
					:current-page="filters.page" :page-sizes="[10, 20, 30, 50]" @current-change="onPageChange"
					@size-change="onSizeChange" />
			</div>
		</div>
	</main>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { apiGet } from '@/utils/api.js'
import { useAuthStore } from '@/stores/auth.js'

const authStore = useAuthStore()
const isLoading = ref(false)
const transactions = ref([])
const total = ref(0)

const filters = reactive({
	keyword: '',
	type: '',
	page: 1,
	size: 20
})

let searchTimeout = null

// 加载流水数据
const loadTransactions = async () => {
	try {
		isLoading.value = true

		const params = new URLSearchParams()
		if (filters.keyword) {
			params.append('keyword', filters.keyword)
		}
		if (filters.type) {
			params.append('type', filters.type)
		}
		params.append('page', filters.page)
		params.append('size', filters.size)

		const response = await apiGet(`/api/admin/points/transactions?${params.toString()}`, { token: authStore.adminToken })
		transactions.value = response.data?.list || []
		total.value = response.data?.total || 0
	} catch (error) {
		ElMessage.error('加载数据失败: ' + error.message)
	} finally {
		isLoading.value = false
	}
}

// 搜索防抖
const onSearchInput = () => {
	if (searchTimeout) {
		clearTimeout(searchTimeout)
	}
	searchTimeout = setTimeout(() => {
		filters.page = 1
		loadTransactions()
	}, 500)
}

const onPageChange = (page) => {
	filters.page = page
	loadTransactions()
}

const onSizeChange = (size) => {
	filters.size = size
	filters.page = 1
	loadTransactions()
}

// 工具函数
const formatDate = (dateStr) => {
	if (!dateStr) return '-'
	return new Date(dateStr).toLocaleString()
}

const getTypeLabel = (type) => {
	const map = {
		'REGISTER_GIFT': '注册赠送',
		'REAL_NAME_GIFT': '实名认证赠送',
		'INVITE_REWARD': '邀请奖励',
		'INVITE_CODE': '使用邀请码',
		'DOMAIN_REGISTER': '注册域名',
		'DOMAIN_RENEW': '域名续费',
		'ADMIN_ADJUST': '管理员调整',
		'CARD_REDEEM': '卡密兑换',
		'GITHUB_STAR_REWARD': 'GitHub Star奖励',
		'DOMAIN_REFUND': '域名退款'
	}
	return map[type] || type
}

const getTypeClass = (type) => {
	if (['REGISTER_GIFT', 'REAL_NAME_GIFT', 'INVITE_REWARD', 'CARD_REDEEM', 'GITHUB_STAR_REWARD'].includes(type)) {
		return 'success'
	}
	if (['DOMAIN_REGISTER', 'DOMAIN_RENEW'].includes(type)) {
		return 'warning' // 消费
	}
	if (['ADMIN_ADJUST', 'DOMAIN_REFUND'].includes(type)) {
		return 'info'
	}
	return ''
}

onMounted(() => {
	loadTransactions()
})
</script>

<style scoped>
.user-info {
	display: flex;
	flex-direction: column;
}

.user-info .email {
	font-size: 0.85em;
	color: #666;
}

.text-success {
	color: var(--success-color, #67c23a);
	font-weight: bold;
}

.text-danger {
	color: var(--danger-color, #f56c6c);
	font-weight: bold;
}
</style>
