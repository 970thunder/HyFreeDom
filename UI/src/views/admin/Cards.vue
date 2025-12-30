<template>
	<main class="main container">
		<div class="card">
			<div class="card-header">
				<h3>生成卡密</h3>
				<div class="header-actions">
					<button class="btn outline" @click="loadCards" :disabled="isLoading">
						{{ isLoading ? '加载中...' : '刷新' }}
					</button>
				</div>
			</div>

			<div class="form">
				<div class="grid cols-3">
					<div class="input-row">
						<label class="label">面值（积分）</label>
						<input class="input" v-model="form.points" placeholder="例如：10" type="number" min="1"
							max="9999" />
					</div>
					<div class="input-row">
						<label class="label">数量</label>
						<input class="input" v-model="form.count" placeholder="例如：100" type="number" min="1" max="1000"
							:disabled="!!form.customCode" />
					</div>
					<div class="input-row">
						<label class="label">有效期（天）</label>
						<input class="input" v-model="form.validDays" placeholder="例如：30" type="number" min="0"
							max="3650" />
					</div>
					<div class="input-row">
						<label class="label">自定义卡密（可选）</label>
						<input class="input" v-model="form.customCode" placeholder="留空则随机生成"
							@input="onCustomCodeInput" />
					</div>
					<div class="input-row">
						<label class="label">使用次数（可选）</label>
						<input class="input" v-model="form.usageLimit" placeholder="留空则不限次数" type="number" min="1" />
					</div>
				</div>
				<div class="row">
					<button class="btn primary" @click="generateCards" :disabled="isLoading">
						{{ isLoading ? '生成中...' : '生成' }}
					</button>
					<button class="btn outline" @click="exportCsv" :disabled="cards.length === 0">
						导出 CSV
					</button>
				</div>
			</div>
		</div>

		<div class="card" style="margin-top:16px;">
			<div class="card-header">
				<h3>卡密列表</h3>
				<div class="header-actions">
					<div class="filter-group">
						<select class="select" style="max-width:160px;" v-model="filters.status" @change="loadCards">
							<option value="">全部状态</option>
							<option value="ACTIVE">未使用</option>
							<option value="USED">已使用</option>
							<option value="EXPIRED">已过期</option>
						</select>
					</div>
					<button class="btn danger" @click="batchDelete" :disabled="selectedCards.length === 0 || isLoading">
						批量删除 ({{ selectedCards.length }})
					</button>
				</div>
			</div>

			<div class="table-container" v-loading="isLoading">
				<table class="table">
					<thead>
						<tr>
							<th>
								<input type="checkbox" v-model="selectAll" @change="toggleSelectAll" />
							</th>
							<th>卡密</th>
							<th>面值</th>
							<th>使用次数</th>
							<th>状态</th>
							<th>生成时间</th>
							<th>过期时间</th>
							<th>使用者</th>
							<th>使用时间</th>
							<th>操作</th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="card in cards" :key="card.id">
							<td>
								<input type="checkbox" v-model="selectedCards" :value="card.id" />
							</td>
							<td class="card-code">{{ card.code }}</td>
							<td>{{ card.points }}</td>
							<td>
								<span v-if="card.usageLimit === -1">
									{{ card.usedCount || 0 }} / ∞
								</span>
								<span v-else>
									{{ card.usedCount || 0 }} / {{ card.usageLimit || 1 }}
								</span>
							</td>
							<td>
								<span class="badge" :class="getStatusClass(card.status)">
									{{ getStatusText(card.status) }}
								</span>
							</td>
							<td>{{ formatDate(card.createdAt) }}</td>
							<td>{{ formatDate(card.expiredAt) || '永不过期' }}</td>
							<td>{{ card.usedByUsername || '-' }}</td>
							<td>{{ formatDate(card.usedAt) || '-' }}</td>
							<td>
								<button class="btn danger small" @click="deleteCard(card.id)" :disabled="isLoading">
									删除
								</button>
							</td>
						</tr>
					</tbody>
				</table>

				<div v-if="!isLoading && cards.length === 0" class="empty-state">
					<p>暂无卡密数据</p>
				</div>
			</div>

			<!-- 分页组件 -->
			<div class="pagination-container" v-if="total > 0">
				<el-pagination v-model:current-page="filters.page" v-model:page-size="filters.size"
					:page-sizes="[10, 20, 50, 100]" :total="total" layout="total, sizes, prev, pager, next, jumper"
					@size-change="handleSizeChange" @current-change="handleCurrentChange" />
			</div>
		</div>
	</main>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiGet, apiPost } from '@/utils/api.js'
import { useAuthStore } from '@/stores/auth.js'

// 认证store
const authStore = useAuthStore()

// 响应式数据
const isLoading = ref(false)
const cards = ref([])
const total = ref(0)
const selectedCards = ref([])
const selectAll = ref(false)

// 生成表单
const form = reactive({
	points: '',
	count: '',
	validDays: '',
	customCode: '',
	usageLimit: ''
})

// 监听自定义卡密输入
const onCustomCodeInput = () => {
	if (form.customCode) {
		form.count = 1
	}
}

// 过滤器
const filters = reactive({
	status: '',
	page: 1,
	size: 20
})

// 加载卡密列表
const loadCards = async () => {
	try {
		isLoading.value = true

		const params = new URLSearchParams()
		if (filters.status) {
			params.append('status', filters.status)
		}
		params.append('page', filters.page)
		params.append('size', filters.size)

		const response = await apiGet(`/api/admin/cards?${params.toString()}`, { token: authStore.adminToken })
		cards.value = response.data?.list || []
		total.value = response.data?.total || 0
	} catch (error) {
		ElMessage.error('加载卡密列表失败: ' + error.message)
	} finally {
		isLoading.value = false
	}
}

// 生成卡密
const generateCards = async () => {
	try {
		// 表单验证
		if (!form.points) {
			ElMessage.error('请填写面值')
			return
		}

		if (!form.customCode && !form.count) {
			ElMessage.error('请填写数量')
			return
		}

		if (form.points < 1 || form.points > 9999) {
			ElMessage.error('面值必须在 1-9999 之间')
			return
		}

		if (!form.customCode && (form.count < 1 || form.count > 1000)) {
			ElMessage.error('数量必须在 1-1000 之间')
			return
		}

		if (form.validDays < 0 || form.validDays > 3650) {
			ElMessage.error('有效期必须在 0-3650 天之间')
			return
		}

		const confirmMsg = form.customCode
			? `确定要生成自定义卡密 "${form.customCode}" (面值 ${form.points} 积分${form.usageLimit ? ', 可用 ' + form.usageLimit + ' 次' : ', 不限次数'}) 吗？`
			: `确定要生成 ${form.count} 张面值为 ${form.points} 积分的卡密${form.usageLimit && form.usageLimit != 1 ? ' (每张可用 ' + form.usageLimit + ' 次)' : ''}吗？`;

		await ElMessageBox.confirm(
			confirmMsg,
			'确认生成',
			{
				confirmButtonText: '确定',
				cancelButtonText: '取消',
				type: 'warning'
			}
		)

		isLoading.value = true

		const data = {
			count: form.customCode ? 1 : parseInt(form.count),
			points: parseInt(form.points),
			validDays: form.validDays ? parseInt(form.validDays) : null,
			customCode: form.customCode || null,
			usageLimit: form.usageLimit ? parseInt(form.usageLimit) : null
		}

		const response = await apiPost('/api/admin/cards/generate', data, { token: authStore.adminToken })
		ElMessage.success(`成功生成 ${response.data.count} 张卡密`)

		// 清空表单
		form.points = ''
		form.count = ''
		form.validDays = ''
		form.customCode = ''
		form.usageLimit = ''

		await loadCards()
	} catch (error) {
		if (error !== 'cancel') {
			ElMessage.error('生成卡密失败: ' + error.message)
		}
	} finally {
		isLoading.value = false
	}
}

// 导出CSV
const exportCsv = () => {
	try {
		if (cards.value.length === 0) {
			ElMessage.warning('没有数据可导出')
			return
		}

		// 构建CSV内容
		const headers = ['卡密', '面值', '使用次数', '限制次数', '状态', '生成时间', '过期时间', '使用者', '使用时间']
		const csvContent = [
			headers.join(','),
			...cards.value.map(card => [
				card.code,
				card.points,
				card.usedCount || 0,
				card.usageLimit === -1 ? '不限' : (card.usageLimit || 1),
				getStatusText(card.status),
				formatDate(card.createdAt),
				formatDate(card.expiredAt) || '永不过期',
				card.usedByUsername || '-',
				formatDate(card.usedAt) || '-'
			].join(','))
		].join('\n')

		// 创建下载链接
		const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
		const link = document.createElement('a')
		const url = URL.createObjectURL(blob)
		link.setAttribute('href', url)
		link.setAttribute('download', `卡密列表_${new Date().toISOString().split('T')[0]}.csv`)
		link.style.visibility = 'hidden'
		document.body.appendChild(link)
		link.click()
		document.body.removeChild(link)

		ElMessage.success('CSV文件导出成功')
	} catch (error) {
		ElMessage.error('导出失败: ' + error.message)
	}
}

// 删除单个卡密
const deleteCard = async (cardId) => {
	try {
		await ElMessageBox.confirm('确定要删除这张卡密吗？', '确认删除', {
			confirmButtonText: '确定',
			cancelButtonText: '取消',
			type: 'warning'
		})

		await fetch(`/api/admin/cards/${cardId}`, {
			method: 'DELETE',
			headers: {
				'Authorization': `Bearer ${authStore.adminToken}`,
				'Content-Type': 'application/json'
			}
		})

		ElMessage.success('卡密删除成功')
		await loadCards()
	} catch (error) {
		if (error !== 'cancel') {
			ElMessage.error('删除失败: ' + error.message)
		}
	}
}

// 批量删除卡密
const batchDelete = async () => {
	try {
		if (selectedCards.value.length === 0) {
			ElMessage.warning('请选择要删除的卡密')
			return
		}

		await ElMessageBox.confirm(
			`确定要删除选中的 ${selectedCards.value.length} 张卡密吗？`,
			'确认批量删除',
			{
				confirmButtonText: '确定',
				cancelButtonText: '取消',
				type: 'warning'
			}
		)

		await fetch('/api/admin/cards/batch', {
			method: 'DELETE',
			headers: {
				'Authorization': `Bearer ${authStore.adminToken}`,
				'Content-Type': 'application/json'
			},
			body: JSON.stringify({ ids: selectedCards.value })
		})

		ElMessage.success('批量删除成功')
		selectedCards.value = []
		selectAll.value = false
		await loadCards()
	} catch (error) {
		if (error !== 'cancel') {
			ElMessage.error('批量删除失败: ' + error.message)
		}
	}
}

// 全选/取消全选
const toggleSelectAll = () => {
	if (selectAll.value) {
		selectedCards.value = cards.value.map(card => card.id)
	} else {
		selectedCards.value = []
	}
}

// 分页处理
const handleSizeChange = (newSize) => {
	filters.size = newSize
	filters.page = 1
	loadCards()
}

const handleCurrentChange = (newPage) => {
	filters.page = newPage
	loadCards()
}

// 获取状态样式类
const getStatusClass = (status) => {
	switch (status) {
		case 'ACTIVE':
			return 'success'
		case 'USED':
			return 'warning'
		case 'EXPIRED':
			return 'danger'
		default:
			return ''
	}
}

// 获取状态文本
const getStatusText = (status) => {
	switch (status) {
		case 'ACTIVE':
			return '未使用'
		case 'USED':
			return '已使用'
		case 'EXPIRED':
			return '已过期'
		default:
			return status
	}
}

// 格式化日期
const formatDate = (dateString) => {
	if (!dateString) return '-'
	return new Date(dateString).toLocaleString('zh-CN')
}

// 组件挂载时初始化
onMounted(() => {
	loadCards()
})
</script>

<style scoped>
/* Inlined minimal styles (do not depend on external prototype) */
.card {
	background: #fff;
	border: 1px solid #e2e8f0;
	border-radius: 12px;
	padding: 16px;
	box-shadow: 0 1px 2px rgba(0, 0, 0, .04);
}

.grid {
	display: grid;
	gap: 16px;
}

.grid.cols-3 {
	grid-template-columns: repeat(3, minmax(0, 1fr));
}

.form {
	display: grid;
	gap: 12px;
	max-width: 640px;
}

.input-row {
	display: grid;
	gap: 8px;
}

.label {
	font-size: 12px;
	color: #475569;
}

.input {
	width: 100%;
	padding: 10px 12px;
	border: 1px solid #cbd5e1;
	border-radius: 10px;
}

.input:focus {
	border-color: #6366f1;
	box-shadow: 0 0 0 3px rgba(99, 102, 241, .15);
	outline: none;
}

.row {
	display: flex;
	gap: 8px;
	align-items: center;
	flex-wrap: wrap;
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

.table {
	width: 100%;
	border-collapse: collapse;
	border: 1px solid #e2e8f0;
	border-radius: 12px;
	overflow: hidden;
}

.table th,
.table td {
	text-align: left;
	padding: 12px 10px;
	border-bottom: 1px solid #e2e8f0;
	font-size: 14px;
}

.table th {
	background: #f1f5f9;
	color: #0f172a;
}

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

.badge.success {
	background: #ecfdf5;
	color: #065f46;
	border-color: #a7f3d0;
}

.badge.warning {
	background: #fef3c7;
	color: #92400e;
	border-color: #fbbf24;
}

.badge.danger {
	background: #fee2e2;
	color: #991b1b;
	border-color: #f87171;
}

.card-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 16px;
}

.header-actions {
	display: flex;
	gap: 8px;
}

.filter-group {
	display: flex;
	gap: 12px;
	align-items: center;
}

.table-container {
	position: relative;
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

.card-code {
	font-family: monospace;
	font-size: 13px;
	background: #f1f5f9;
	padding: 2px 6px;
	border-radius: 4px;
}

/* 响应式：窄屏优化 */
@media (max-width: 960px) {
	.grid.cols-3 {
		grid-template-columns: 1fr;
	}
}

@media (max-width: 768px) {
	.card {
		padding: 12px;
	}

	.form {
		max-width: 100%;
	}

	.table th,
	.table td {
		padding: 10px 8px;
		font-size: 13px;
	}
}
</style>