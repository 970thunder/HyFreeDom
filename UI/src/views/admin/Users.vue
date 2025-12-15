<template>
	<main class="main container">
		<div class="card">
			<div class="card-header">
				<h3>用户列表</h3>
				<div class="header-actions">
					<button class="btn outline" @click="loadUsers" :disabled="isLoading">
						{{ isLoading ? '加载中...' : '刷新' }}
					</button>
				</div>
			</div>

			<div class="filters" style="margin-bottom:12px;">
				<div class="filter-group">
					<input class="input" style="max-width:240px;" placeholder="搜索用户名/邮箱" v-model="filters.keyword"
						@input="onSearchInput" />
					<select class="select" style="max-width:160px;" v-model="filters.role" @change="loadUsers">
						<option value="">全部角色</option>
						<option value="USER">USER</option>
						<option value="ADMIN">ADMIN</option>
					</select>
					<select class="select" style="max-width:160px;" v-model="filters.status" @change="loadUsers">
						<option value="">全部状态</option>
						<option value="1">启用</option>
						<option value="0">禁用</option>
						<option value="2">注销</option>
					</select>
					<select class="select" style="max-width:160px;" v-model="filters.isVerified" @change="loadUsers">
						<option value="">全部实名</option>
						<option :value="true">已认证</option>
						<option :value="false">未认证</option>
					</select>
				</div>
			</div>

			<div class="table-container" v-loading="isLoading">
				<table class="table">
					<thead>
						<tr>
							<th>ID</th>
							<th>用户名</th>
							<th>邮箱</th>
							<th>实名</th>
							<th>IP地址</th>
							<th>积分</th>
							<th>角色</th>
							<th>状态</th>
							<th>注册时间</th>
							<th class="col-actions">操作</th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="user in users" :key="user.id">
							<td>{{ user.id }}</td>
							<td>{{ user.username }}</td>
							<td>{{ user.email }}</td>
							<td>
								<span class="badge" :class="user.isVerified ? 'success' : 'info'">
									{{ user.isVerified ? '已认证' : '未认证' }}
								</span>
							</td>
							<td>{{ user.ipAddress || '-' }}</td>
							<td>{{ user.points || 0 }}</td>
							<td>
								<span class="badge" :class="user.role === 'ADMIN' ? 'warning' : ''">
									{{ user.role }}
								</span>
							</td>
							<td>
								<span class="badge" :class="getStatusClass(user.status)">
									{{ getStatusText(user.status) }}
								</span>
							</td>
							<td>{{ formatDate(user.createdAt) }}</td>
							<td class="row">
								<button class="btn outline" @click="adjustPoints(user)" :disabled="user.loading">
									调整积分
								</button>
								<button class="btn outline" @click="toggleRole(user)" :disabled="user.loading">
									{{ user.role === 'ADMIN' ? '取消管理员' : '设为管理员' }}
								</button>
								<button class="btn primary" @click="resetPassword(user)" :disabled="user.loading">
									重置密码
								</button>
								<button class="btn danger" @click="toggleStatus(user)" :disabled="user.loading"
									v-if="user.status !== 2">
									{{ user.status === 1 ? '禁用' : '启用' }}
								</button>
								<button class="btn danger" @click="deleteUser(user)" :disabled="user.loading"
									v-if="user.status === 1">
									注销账号
								</button>
								<button class="btn outline" disabled v-else-if="user.status === 2">
									已注销
								</button>
							</td>
						</tr>
					</tbody>
				</table>

				<div v-if="!isLoading && users.length === 0" class="empty-state">
					<p>暂无用户数据</p>
				</div>

				<!-- 分页 -->
				<el-pagination v-if="!isLoading && total > 0" class="pagination" background
					layout="prev, pager, next, sizes, total" :total="total" :page-size="filters.size"
					:current-page="filters.page" :page-sizes="[10, 20, 30, 50]" @current-change="onPageChange"
					@size-change="onSizeChange" />
			</div>
		</div>

		<!-- 积分调整对话框 -->
		<el-dialog :model-value="pointsDialogVisible" @update:model-value="pointsDialogVisible = $event" title="调整积分"
			width="400px">
			<el-form :model="pointsForm" :rules="pointsRules" ref="pointsFormRef" label-width="80px">
				<el-form-item label="用户">
					<el-input v-model="pointsForm.username" disabled />
				</el-form-item>
				<el-form-item label="当前积分">
					<el-input v-model="pointsForm.currentPoints" disabled />
				</el-form-item>
				<el-form-item label="调整数量" prop="delta">
					<el-input-number v-model="pointsForm.delta" :min="-9999" :max="9999" style="width: 100%;" />
					<div class="form-tip">正数为增加，负数为减少</div>
				</el-form-item>
				<el-form-item label="备注" prop="remark">
					<el-input v-model="pointsForm.remark" type="textarea" placeholder="请输入调整原因" />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="pointsDialogVisible = false">取消</el-button>
				<el-button type="primary" @click="savePointsAdjustment" :loading="isLoading">
					确定
				</el-button>
			</template>
		</el-dialog>

		<!-- 注销用户对话框 -->
		<el-dialog :model-value="deleteUserDialogVisible" @update:model-value="deleteUserDialogVisible = $event"
			title="注销用户账号" :width="dialogWidth" :close-on-click-modal="false" class="delete-user-dialog">
			<div class="warning-box">
				<el-icon class="warning-icon">
					<Warning />
				</el-icon>
				<div class="warning-content">
					<h4>⚠️ 用户账号注销确认</h4>
					<p>您即将注销用户 <strong>{{ currentUser.username }}</strong> 的账号，此操作<strong>不可撤销</strong>！</p>

					<div class="consequences">
						<h5>注销后将发生以下情况：</h5>
						<ul>
							<li>❌ 用户账号将无法登录</li>
							<li>❌ 用户将无法申请新的域名</li>
							<li>❌ 用户将无法使用邀请功能</li>
							<li>❌ 用户的所有积分将被清零（当前积分：<strong>{{ currentUser.points }}</strong>）</li>
							<li>✅ 已申请的域名将保留（但用户无法管理）</li>
							<li>✅ 邀请关系数据将保留</li>
						</ul>
					</div>

					<!-- 用户域名列表 -->
					<div v-if="isLoadingDomains" class="domains-loading">
						<el-icon class="is-loading">
							<Loading />
						</el-icon>
						<span>正在加载用户域名信息...</span>
					</div>
					<div v-else-if="domainCount > 0" class="domains-section">
						<h5>该用户当前拥有的域名（{{ domainCount }} 个）：</h5>
						<div class="domains-list">
							<div v-for="domain in userDomains" :key="domain.id" class="domain-item">
								<span class="domain-name">{{ domain.fullDomain }}</span>
								<span class="domain-type">{{ domain.type || 'A' }}</span>
								<span v-if="domain.remark" class="domain-remark">{{ domain.remark }}</span>
							</div>
						</div>
						<p class="domains-note">⚠️ 注销后，这些域名将自动释放，用户将无法再管理它们。</p>
					</div>
					<div v-else class="no-domains">
						<p>该用户当前没有申请任何域名。</p>
					</div>
				</div>
			</div>

			<el-form :model="deleteForm" :rules="deleteRules" ref="deleteFormRef" class="delete-form">
				<el-form-item prop="confirmDeletion">
					<el-checkbox v-model="deleteForm.confirmDeletion">
						我确认要注销此用户账号，理解此操作不可撤销
					</el-checkbox>
				</el-form-item>
				<el-form-item prop="confirmDnsRelease">
					<el-checkbox v-model="deleteForm.confirmDnsRelease">
						我确认已释放该用户的所有DNS域名
					</el-checkbox>
				</el-form-item>
			</el-form>

			<template #footer>
				<div class="dialog-footer">
					<el-button @click="deleteUserDialogVisible = false" class="cancel-btn">取消</el-button>
					<el-button type="danger" @click="handleDeleteUser" :loading="isDeleting" :disabled="!canDelete"
						class="delete-btn">
						确认注销
					</el-button>
				</div>
			</template>
		</el-dialog>
	</main>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Warning, Loading } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '@/utils/api.js'
import { useAuthStore } from '@/stores/auth.js'

// 认证store
const authStore = useAuthStore()

// 响应式数据
const isLoading = ref(false)
const users = ref([])
const total = ref(0)
const pointsDialogVisible = ref(false)
const pointsFormRef = ref()

// 注销用户相关
const deleteUserDialogVisible = ref(false)
const isDeleting = ref(false)
const isLoadingDomains = ref(false)
const currentUser = ref({
	username: '',
	displayName: '',
	points: 0
})
const userDomains = ref([])
const domainCount = ref(0)
const deleteForm = ref({
	confirmDeletion: false,
	confirmDnsRelease: false
})

const deleteRules = {
	confirmDeletion: [
		{
			validator: (rule, value, callback) => {
				if (!value) {
					callback(new Error('请确认注销操作'))
				} else {
					callback()
				}
			},
			trigger: 'change'
		}
	],
	confirmDnsRelease: [
		{
			validator: (rule, value, callback) => {
				if (!value) {
					callback(new Error('请确认释放DNS域名'))
				} else {
					callback()
				}
			},
			trigger: 'change'
		}
	]
}

const deleteFormRef = ref(null)

// 响应式计算属性
const dialogWidth = computed(() => {
	if (window.innerWidth <= 480) return '95%'
	if (window.innerWidth <= 768) return '90%'
	return '500px'
})

// 计算属性
const canDelete = computed(() => {
	return deleteForm.value.confirmDeletion && deleteForm.value.confirmDnsRelease
})

// 过滤器
const filters = reactive({
	keyword: '',
	role: '',
	status: '',
	isVerified: '',
	page: 1,
	size: 20
})

// 搜索防抖
let searchTimeout = null

// 积分调整表单
const pointsForm = reactive({
	userId: null,
	username: '',
	currentPoints: 0,
	delta: 0,
	remark: ''
})

// 表单验证规则
const pointsRules = {
	delta: [
		{ required: true, message: '请输入调整数量', trigger: 'blur' },
		{ type: 'number', message: '请输入有效数字', trigger: 'blur' }
	],
	remark: [
		{ required: true, message: '请输入调整原因', trigger: 'blur' }
	]
}

// 加载用户列表
const loadUsers = async () => {
	try {
		isLoading.value = true

		const params = new URLSearchParams()
		if (filters.keyword) {
			params.append('keyword', filters.keyword)
		}
		if (filters.role) {
			params.append('role', filters.role)
		}
		if (filters.status) {
			params.append('status', filters.status)
		}
		if (filters.isVerified !== '') {
			params.append('isVerified', filters.isVerified)
		}
		params.append('page', filters.page)
		params.append('size', filters.size)

		const response = await apiGet(`/api/admin/users?${params.toString()}`, { token: authStore.adminToken })
		users.value = response.data?.list || []
		total.value = response.data?.total || users.value.length
	} catch (error) {
		ElMessage.error('加载用户列表失败: ' + error.message)
	} finally {
		isLoading.value = false
	}
}

// 搜索输入处理（防抖）
const onSearchInput = () => {
	if (searchTimeout) {
		clearTimeout(searchTimeout)
	}
	searchTimeout = setTimeout(() => {
		loadUsers()
	}, 500)
}

// 分页变更
const onPageChange = (p) => {
	filters.page = p
	loadUsers()
}

const onSizeChange = (s) => {
	filters.size = s
	filters.page = 1
	loadUsers()
}

// 调整积分
const adjustPoints = (user) => {
	pointsForm.userId = user.id
	pointsForm.username = user.username
	pointsForm.currentPoints = user.points || 0
	pointsForm.delta = 0
	pointsForm.remark = ''
	pointsDialogVisible.value = true
}

// 保存积分调整
const savePointsAdjustment = async () => {
	try {
		await pointsFormRef.value.validate()
		isLoading.value = true

		await apiPost(`/api/admin/users/${pointsForm.userId}/points`, {
			delta: pointsForm.delta,
			remark: pointsForm.remark
		}, { token: authStore.adminToken })

		ElMessage.success('积分调整成功')
		pointsDialogVisible.value = false
		await loadUsers()
	} catch (error) {
		ElMessage.error('积分调整失败: ' + error.message)
	} finally {
		isLoading.value = false
	}
}

// 重置密码（设为默认 123456@）
const resetPassword = async (user) => {
	try {
		await ElMessageBox.confirm(
			`确定将用户 "${user.username}" 的密码重置为 123456@ 吗？`,
			'确认操作',
			{
				confirmButtonText: '确定',
				cancelButtonText: '取消',
				type: 'warning'
			}
		)
		user.loading = true
		await apiPost(`/api/admin/users/${user.id}/reset-password`, {}, { token: authStore.adminToken })
		ElMessage.success('密码已重置为 123456@')
	} catch (error) {
		if (error !== 'cancel') {
			ElMessage.error('重置失败: ' + (error.message || error))
		}
	} finally {
		user.loading = false
	}
}

// 切换角色
const toggleRole = async (user) => {
	try {
		const newRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
		await ElMessageBox.confirm(
			`确定要将用户 "${user.username}" 的角色改为 ${newRole} 吗？`,
			'确认操作',
			{
				confirmButtonText: '确定',
				cancelButtonText: '取消',
				type: 'warning'
			}
		)

		user.loading = true
		await apiPost(`/api/admin/users/${user.id}/role`, {
			role: newRole
		}, { token: authStore.adminToken })

		ElMessage.success('角色切换成功')
		await loadUsers()
	} catch (error) {
		if (error !== 'cancel') {
			ElMessage.error('角色切换失败: ' + (error.message || error))
		}
	} finally {
		user.loading = false
	}
}

// 切换状态
const toggleStatus = async (user) => {
	try {
		const newStatus = user.status === 1 ? 0 : 1
		const action = newStatus === 1 ? '启用' : '禁用'

		await ElMessageBox.confirm(
			`确定要${action}用户 "${user.username}" 吗？`,
			'确认操作',
			{
				confirmButtonText: '确定',
				cancelButtonText: '取消',
				type: 'warning'
			}
		)

		user.loading = true
		await apiPost(`/api/admin/users/${user.id}/status`, {
			status: newStatus
		}, { token: authStore.adminToken })

		ElMessage.success(`${action}成功`)
		await loadUsers()
	} catch (error) {
		if (error !== 'cancel') {
			ElMessage.error('操作失败: ' + (error.message || error))
		}
	} finally {
		user.loading = false
	}
}

// 格式化日期
const formatDate = (dateString) => {
	if (!dateString) return '-'
	return new Date(dateString).toLocaleString('zh-CN')
}

// 获取状态文本
const getStatusText = (status) => {
	switch (status) {
		case 1: return '启用'
		case 0: return '禁用'
		case 2: return '注销'
		default: return '未知'
	}
}

// 获取状态样式类
const getStatusClass = (status) => {
	switch (status) {
		case 1: return 'success'
		case 0: return 'danger'
		case 2: return 'warning'
		default: return 'danger'
	}
}

// 组件挂载时初始化
// 注销用户
const deleteUser = async (user) => {
	try {
		currentUser.value = {
			username: user.username,
			displayName: user.displayName || user.username,
			points: user.points || 0
		}

		// 重置表单
		deleteForm.value = {
			confirmDeletion: false,
			confirmDnsRelease: false
		}

		// 重置域名信息
		userDomains.value = []
		domainCount.value = 0

		deleteUserDialogVisible.value = true

		// 加载用户的域名信息
		await loadUserDomains(user.id)
	} catch (error) {
		ElMessage.error('打开注销对话框失败: ' + (error.message || error))
	}
}

// 加载用户域名信息
const loadUserDomains = async (userId) => {
	try {
		isLoadingDomains.value = true
		const response = await apiGet(`/api/admin/users/${userId}/domains`, { token: authStore.adminToken })

		if (response.data) {
			userDomains.value = response.data.domains || []
			domainCount.value = response.data.domainCount || 0
			currentUser.value.points = response.data.points || 0
		}
	} catch (error) {
		console.error('加载用户域名信息失败:', error)
		ElMessage.error('加载用户域名信息失败: ' + (error.message || error))
		userDomains.value = []
		domainCount.value = 0
	} finally {
		isLoadingDomains.value = false
	}
}

// 处理用户注销
const handleDeleteUser = async () => {
	try {
		await deleteFormRef.value.validate()

		const confirmed = await ElMessageBox.confirm(
			`您确定要注销用户 "${currentUser.value.username}" 的账号吗？此操作不可撤销！`,
			'最终确认',
			{
				confirmButtonText: '确认注销',
				cancelButtonText: '取消',
				type: 'error',
				dangerouslyUseHTMLString: true
			}
		)

		if (!confirmed) return

		isDeleting.value = true

		// 找到要删除的用户ID
		const userToDelete = users.value.find(u => u.username === currentUser.value.username)
		if (!userToDelete) {
			ElMessage.error('用户不存在')
			return
		}

		const response = await apiPost(`/api/admin/users/${userToDelete.id}/delete`, {
			confirmDeletion: deleteForm.value.confirmDeletion,
			confirmDnsRelease: deleteForm.value.confirmDnsRelease
		}, { token: authStore.adminToken })

		if (response.code === 0) {
			// 显示详细的注销结果
			const releasedDomains = response.data.releasedDomains || 0
			const failedDomains = response.data.failedDomains || 0
			const totalDomains = response.data.totalDomains || 0

			let message = '用户账号注销成功'
			if (totalDomains > 0) {
				message += `，已释放 ${releasedDomains} 个域名`
				if (failedDomains > 0) {
					message += `，${failedDomains} 个域名释放失败`
				}
			}

			ElMessage.success(message)
			deleteUserDialogVisible.value = false
			await loadUsers()
		} else {
			ElMessage.error(response.message || '用户账号注销失败')
		}
	} catch (error) {
		if (error !== 'cancel') {
			ElMessage.error(error.message || '用户账号注销失败')
		}
	} finally {
		isDeleting.value = false
	}
}

onMounted(() => {
	loadUsers()
})
</script>

<style scoped>
.card {
	background: #fff;
	border: 1px solid #e2e8f0;
	border-radius: 8px;
	padding: 16px;
}

.row {
	display: flex;
	gap: 6px;
	align-items: center;
	flex-wrap: nowrap;
	min-width: 0;
}

.spacer {
	flex: 1;
}

.input,
.select {
	width: 100%;
	padding: 10px 12px;
	border: 1px solid #cbd5e1;
	border-radius: 10px;
}

.input:focus,
.select:focus {
	border-color: #6366f1;
	box-shadow: 0 0 0 3px rgba(99, 102, 241, .15);
	outline: none;
}

.btn {
	display: inline-flex;
	align-items: center;
	gap: 6px;
	padding: 8px 12px;
	border-radius: 6px;
	border: 1px solid transparent;
	font-weight: 600;
	cursor: pointer;
	font-size: 13px;
	white-space: nowrap;
	flex-shrink: 0;
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

.btn.danger {
	background: #ef4444;
	color: #fff;
}

.btn.danger:hover {
	background: #dc2626;
}

.table {
	width: 100%;
	border-collapse: collapse;
	border: 1px solid #e2e8f0;
	border-radius: 8px;
	overflow: hidden;
}

.table th,
.table td {
	text-align: left;
	padding: 12px 10px;
	border-bottom: 1px solid #e2e8f0;
	font-size: 14px;
}

.col-actions {
	width: 360px;
	/* 固定操作列宽度，避免按钮换行 */
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
	white-space: nowrap;
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

.badge.info {
	background: #f1f5f9;
	color: #475569;
	border-color: #cbd5e1;
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

.form-tip {
	font-size: 12px;
	color: #64748b;
	margin-top: 4px;
}

/* 响应式：表格与筛选区域在移动端更友好 */
@media (max-width: 768px) {
	.card {
		padding: 12px;
	}

	.row {
		flex-wrap: wrap;
		gap: 8px;
	}

	.input,
	.select {
		width: 100%;
		max-width: none;
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

	/* 操作列按钮优化 */
	.table td.row {
		flex-wrap: wrap;
		gap: 4px;
	}

	.table td.row .btn {
		font-size: 12px;
		padding: 6px 10px;
		min-width: auto;
	}

	/* 顶部筛选区按钮在小屏占满宽度，避免溢出 */
	.row .btn {
		width: 100%;
	}
}

/* 操作列按钮样式优化 */
.table td.row {
	display: flex;
	gap: 6px;
	align-items: center;
	flex-wrap: nowrap;
	min-width: 0;
	overflow: hidden;
}

.table td.row .btn {
	flex-shrink: 0;
	min-width: 0;
	overflow: hidden;
	text-overflow: ellipsis;
}

/* 注销用户对话框样式 */
.delete-user-dialog .el-dialog {
	margin: 0 auto;
}

.warning-box {
	display: flex;
	gap: 12px;
	padding: 16px;
	background: #fef3cd;
	border: 1px solid #f59e0b;
	border-radius: 8px;
	margin-bottom: 20px;
}

.warning-icon {
	color: #f59e0b;
	font-size: 20px;
	margin-top: 2px;
}

.warning-content h4 {
	margin: 0 0 8px 0;
	color: #92400e;
	font-size: 16px;
	font-weight: 600;
}

.warning-content h5 {
	margin: 12px 0 8px 0;
	color: #92400e;
	font-size: 14px;
	font-weight: 500;
}

.warning-content p {
	margin: 0 0 8px 0;
	color: #92400e;
	line-height: 1.5;
}

.consequences {
	margin-top: 16px;
}

.consequences ul {
	margin: 8px 0 0 0;
	padding-left: 20px;
}

.consequences li {
	margin: 4px 0;
	color: #92400e;
	line-height: 1.4;
}

.delete-form {
	padding: 0;
}

.delete-form .el-form-item {
	margin-bottom: 16px;
}

.delete-form .el-checkbox {
	display: flex;
	align-items: flex-start;
	gap: 8px;
}

.delete-form .el-checkbox__label {
	line-height: 1.4;
	color: #374151;
}

.delete-btn {
	background-color: #dc2626;
	border-color: #dc2626;
}

.delete-btn:hover {
	background-color: #b91c1c;
	border-color: #b91c1c;
}

/* 域名列表样式 */
.domains-loading {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 8px;
	padding: 20px 0;
	color: #6b7280;
}

.domains-section {
	margin-top: 16px;
	padding: 12px;
	background: #f8fafc;
	border: 1px solid #e2e8f0;
	border-radius: 6px;
}

.domains-section h5 {
	margin: 0 0 12px 0;
	color: #374151;
	font-size: 14px;
	font-weight: 600;
}

.domains-list {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
	margin-bottom: 12px;
}

.domain-item {
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 6px 12px;
	background: #fff;
	border: 1px solid #d1d5db;
	border-radius: 4px;
	font-size: 13px;
}

.domain-name {
	font-family: monospace;
	font-weight: 500;
	color: #1f2937;
}

.domain-type {
	padding: 2px 6px;
	background: #dbeafe;
	color: #1e40af;
	border-radius: 3px;
	font-size: 11px;
	font-weight: 500;
}

.domain-remark {
	color: #6b7280;
	font-size: 12px;
	font-style: italic;
}

.domains-note {
	margin: 0;
	padding: 8px 12px;
	background: #fef3cd;
	border: 1px solid #f59e0b;
	border-radius: 4px;
	color: #92400e;
	font-size: 13px;
	line-height: 1.4;
}

.no-domains {
	margin-top: 16px;
	padding: 12px;
	background: #f0f9ff;
	border: 1px solid #bae6fd;
	border-radius: 6px;
	text-align: center;
}

.no-domains p {
	margin: 0;
	color: #0369a1;
	font-size: 14px;
}
</style>