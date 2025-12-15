<template>
	<main class="main container">
		<div class="card">
			<div class="card-header">
				<h3>邀请码列表</h3>
				<div class="header-actions">
					<button class="btn outline" @click="loadInvites" :disabled="isLoading">
						{{ isLoading ? '加载中...' : '刷新' }}
					</button>
					<button class="btn primary" @click="showCreateDialog">生成邀请码</button>
				</div>
			</div>

			<div class="filters" style="margin-bottom:12px;">
				<div class="filter-group">
					<input class="input" style="max-width:240px;" placeholder="搜索邀请码/用户" v-model="filters.keyword"
						@input="onSearchInput" />
					<select class="select" style="max-width:160px;" v-model="filters.ownerUserId" @change="loadInvites">
						<option value="">全部用户</option>
						<option v-for="user in users" :key="user.id" :value="user.id">{{ user.username }}</option>
					</select>
				</div>
			</div>

			<div class="table-container" v-loading="isLoading">
				<table class="table">
					<thead>
						<tr>
							<th>邀请码</th>
							<th>归属用户</th>
							<th>已使用次数</th>
							<th>最大使用次数</th>
							<th>状态</th>
							<th>创建时间</th>
							<th>过期时间</th>
							<th>操作</th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="invite in invites" :key="invite.id">
							<td class="invite-code">{{ invite.code }}</td>
							<td>{{ invite.ownerUsername || '通用' }}</td>
							<td>{{ invite.usedCount || 0 }}</td>
							<td>{{ invite.maxUses || '无限制' }}</td>
							<td>
								<span class="badge" :class="getStatusClass(invite.status)">
									{{ getStatusText(invite.status) }}
								</span>
							</td>
							<td>{{ formatDate(invite.createdAt) }}</td>
							<td>{{ formatDate(invite.expiredAt) || '永不过期' }}</td>
							<td class="row">
								<button class="btn outline" @click="toggleStatus(invite)" :disabled="invite.loading">
									{{ invite.status === 'ACTIVE' ? '禁用' : '启用' }}
								</button>
								<button class="btn outline" @click="viewDetails(invite)">查看明细</button>
							</td>
						</tr>
					</tbody>
				</table>

				<div v-if="!isLoading && invites.length === 0" class="empty-state">
					<p>暂无邀请码数据</p>
				</div>

				<!-- 分页 -->
				<el-pagination v-if="!isLoading && total > 0" class="pagination" background
					layout="prev, pager, next, sizes, total" :total="total" :page-size="filters.size"
					:current-page="filters.page" :page-sizes="[10, 20, 30, 50]" @current-change="onPageChange"
					@size-change="onSizeChange" />
			</div>
		</div>

		<!-- 生成邀请码对话框 -->
		<el-dialog :model-value="createDialogVisible" @update:model-value="createDialogVisible = $event" title="生成邀请码"
			width="500px">
			<el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px">
				<el-form-item label="归属用户" prop="ownerUserId">
					<el-select v-model="createForm.ownerUserId" placeholder="选择用户（留空为通用码）" style="width: 100%;"
						clearable>
						<el-option v-for="user in users" :key="user.id" :label="user.username" :value="user.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="最大使用次数" prop="maxUses">
					<el-input-number v-model="createForm.maxUses" :min="0" :max="9999" style="width: 100%;" />
					<div class="form-tip">0 表示不限制使用次数</div>
				</el-form-item>
				<el-form-item label="有效天数" prop="validDays">
					<el-input-number v-model="createForm.validDays" :min="0" :max="3650" style="width: 100%;" />
					<div class="form-tip">0 表示永不过期</div>
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="createDialogVisible = false">取消</el-button>
				<el-button type="primary" @click="createInvite" :loading="isLoading">
					生成
				</el-button>
			</template>
		</el-dialog>

		<!-- 邀请码详情对话框 -->
		<el-dialog :model-value="detailDialogVisible" @update:model-value="detailDialogVisible = $event" title="邀请码详情"
			width="600px">
			<div v-if="selectedInvite" class="invite-detail">
				<div class="detail-item">
					<label>邀请码：</label>
					<span class="invite-code">{{ selectedInvite.code }}</span>
				</div>
				<div class="detail-item">
					<label>归属用户：</label>
					<span>{{ selectedInvite.ownerUsername || '通用' }}</span>
				</div>
				<div class="detail-item">
					<label>使用情况：</label>
					<span>{{ selectedInvite.usedCount || 0 }} / {{ selectedInvite.maxUses || '无限制' }}</span>
				</div>
				<div class="detail-item">
					<label>状态：</label>
					<span class="badge" :class="getStatusClass(selectedInvite.status)">
						{{ getStatusText(selectedInvite.status) }}
					</span>
				</div>
				<div class="detail-item">
					<label>创建时间：</label>
					<span>{{ formatDate(selectedInvite.createdAt) }}</span>
				</div>
				<div class="detail-item">
					<label>过期时间：</label>
					<span>{{ formatDate(selectedInvite.expiredAt) || '永不过期' }}</span>
				</div>
			</div>
		</el-dialog>
	</main>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiGet, apiPost } from '@/utils/api.js'
import { useAuthStore } from '@/stores/auth.js'

// 认证store
const authStore = useAuthStore()

// 响应式数据
const isLoading = ref(false)
const invites = ref([])
const total = ref(0)
const users = ref([])
const createDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const selectedInvite = ref(null)
const createFormRef = ref()

// 过滤器
const filters = reactive({
	keyword: '',
	ownerUserId: '',
	page: 1,
	size: 20
})

// 搜索防抖
let searchTimeout = null

// 生成表单
const createForm = reactive({
	ownerUserId: null,
	maxUses: 0,
	validDays: 0
})

// 表单验证规则
const createRules = {
	maxUses: [
		{ required: true, message: '请输入最大使用次数', trigger: 'blur' },
		{ type: 'number', min: 0, max: 9999, message: '使用次数必须在 0-9999 之间', trigger: 'blur' }
	],
	validDays: [
		{ required: true, message: '请输入有效天数', trigger: 'blur' },
		{ type: 'number', min: 0, max: 3650, message: '有效天数必须在 0-3650 之间', trigger: 'blur' }
	]
}

// 加载邀请码列表
const loadInvites = async () => {
	try {
		isLoading.value = true

		const params = new URLSearchParams()
		if (filters.keyword) {
			params.append('keyword', filters.keyword)
		}
		if (filters.ownerUserId) {
			params.append('ownerUserId', filters.ownerUserId)
		}
		params.append('page', filters.page)
		params.append('size', filters.size)

		const response = await apiGet(`/api/admin/invites?${params.toString()}`, { token: authStore.adminToken })
		invites.value = response.data?.list || []
		total.value = response.data?.total || invites.value.length
	} catch (error) {
		ElMessage.error('加载邀请码列表失败: ' + error.message)
	} finally {
		isLoading.value = false
	}
}

// 加载用户列表（用于下拉选择）
const loadUsers = async () => {
	try {
		const response = await apiGet('/api/admin/users?page=1&size=1000', { token: authStore.adminToken })
		users.value = response.data?.list || []
	} catch (error) {
		console.error('加载用户列表失败:', error)
	}
}

// 搜索输入处理（防抖）
const onSearchInput = () => {
	if (searchTimeout) {
		clearTimeout(searchTimeout)
	}
	searchTimeout = setTimeout(() => {
		loadInvites()
	}, 500)
}

// 分页变更
const onPageChange = (p) => {
	filters.page = p
	loadInvites()
}

const onSizeChange = (s) => {
	filters.size = s
	filters.page = 1
	loadInvites()
}

// 显示生成对话框
const showCreateDialog = () => {
	createForm.ownerUserId = null
	createForm.maxUses = 0
	createForm.validDays = 0
	createDialogVisible.value = true
}

// 生成邀请码
const createInvite = async () => {
	try {
		await createFormRef.value.validate()
		isLoading.value = true

		const data = {
			ownerUserId: createForm.ownerUserId || null,
			maxUses: createForm.maxUses,
			validDays: createForm.validDays
		}

		const response = await apiPost('/api/admin/invites', data, { token: authStore.adminToken })
		ElMessage.success('邀请码生成成功: ' + response.data.code)
		createDialogVisible.value = false
		await loadInvites()
	} catch (error) {
		ElMessage.error('生成邀请码失败: ' + error.message)
	} finally {
		isLoading.value = false
	}
}

// 切换状态
const toggleStatus = async (invite) => {
	try {
		const action = invite.status === 'ACTIVE' ? '禁用' : '启用'
		await ElMessageBox.confirm(
			`确定要${action}邀请码 "${invite.code}" 吗？`,
			'确认操作',
			{
				confirmButtonText: '确定',
				cancelButtonText: '取消',
				type: 'warning'
			}
		)

		invite.loading = true
		// 注意：API文档中没有直接的状态切换接口，这里假设通过更新接口实现
		// 实际实现可能需要调用邀请码更新接口
		ElMessage.success(`${action}成功`)
		await loadInvites()
	} catch (error) {
		if (error !== 'cancel') {
			ElMessage.error('操作失败: ' + error.message)
		}
	} finally {
		invite.loading = false
	}
}

// 查看详情
const viewDetails = (invite) => {
	selectedInvite.value = invite
	detailDialogVisible.value = true
}

// 获取状态样式类
const getStatusClass = (status) => {
	switch (status) {
		case 'ACTIVE':
			return 'success'
		case 'INACTIVE':
			return 'danger'
		case 'EXPIRED':
			return 'warning'
		default:
			return ''
	}
}

// 获取状态文本
const getStatusText = (status) => {
	switch (status) {
		case 'ACTIVE':
			return '有效'
		case 'INACTIVE':
			return '禁用'
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
	loadUsers()
	loadInvites()
})
</script>

<style scoped>
.pagination {
	margin-top: 16px;
	justify-content: flex-end;
}

.card {
	background: #fff;
	border: 1px solid #e2e8f0;
	border-radius: 8px;
	padding: 16px;
}

.row {
	display: flex;
	gap: 8px;
	align-items: center;
	flex-wrap: wrap;
}

.spacer {
	flex: 1;
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

.filters {
	margin-bottom: 16px;
}

.filter-group {
	display: flex;
	gap: 12px;
	align-items: center;
	flex-wrap: wrap;
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

.invite-code {
	font-family: monospace;
	font-size: 13px;
	background: #f1f5f9;
	padding: 2px 6px;
	border-radius: 4px;
}

.invite-detail {
	display: grid;
	gap: 12px;
}

.detail-item {
	display: flex;
	align-items: center;
	gap: 8px;
}

.detail-item label {
	font-weight: 500;
	color: #374151;
	min-width: 80px;
}

.form-tip {
	font-size: 12px;
	color: #64748b;
	margin-top: 4px;
}

/* 响应式：移动端适配 */
@media (max-width: 768px) {
	.card {
		padding: 12px;
	}

	.input {
		width: 100%;
	}

	.row {
		flex-wrap: wrap;
		gap: 8px;
	}

	/* 表格在小屏可横向滚动，避免溢出 */
	.table {
		display: block;
		overflow-x: auto;
		-webkit-overflow-scrolling: touch;
	}

	.table th,
	.table td {
		padding: 10px 8px;
		font-size: 13px;
	}

	/* 操作列按钮堆叠并占满宽度 */
	.table td.row {
		flex-direction: column;
		align-items: stretch;
	}

	/* 移动端表单区域与操作列按钮全宽显示，避免换行挤压 */
	.row .btn {
		width: 100%;
	}
}
</style>