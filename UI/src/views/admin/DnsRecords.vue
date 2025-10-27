<template>
	<main class="main container">
		<div class="card">
			<div class="card-header">
				<h3>DNS 记录管理</h3>
				<div class="header-actions">
					<button class="btn outline" @click="loadZones" :disabled="isLoading">
						{{ isLoading ? '加载中...' : '刷新 Zones' }}
					</button>
					<button class="btn primary" @click="showAddDialog" :disabled="isLoading || !currentZone">
						添加记录
					</button>
				</div>
			</div>

			<!-- Zone 标签页 -->
			<div class="zone-tabs" v-if="zones.length > 0">
				<div class="tab-list">
					<button v-for="zone in zones" :key="zone.id" class="tab-item"
						:class="{ active: currentZone?.id === zone.id }" @click="selectZone(zone)">
						<span class="zone-name">{{ zone.name }}</span>
						<span class="zone-status" :class="{ enabled: zone.enabled }">
							{{ zone.enabled ? '启用' : '禁用' }}
						</span>
					</button>
				</div>
			</div>

			<!-- 无 Zone 提示 -->
			<div v-else-if="!isLoading" class="empty-zones">
				<p>暂无可用的 Zone，请先添加 Cloudflare 账户并同步 Zones</p>
				<button class="btn primary" @click="goToZones">前往 Zones 管理</button>
			</div>

			<!-- DNS 记录区域 -->
			<div v-if="currentZone" class="dns-records-section">
				<div class="zone-info">
					<h4>{{ currentZone.name }} 的 DNS 记录</h4>
					<span class="zone-id">Zone ID: {{ currentZone.zoneId }}</span>
				</div>

				<div class="filters" style="margin-bottom:12px;">
					<div class="filter-group">
						<input class="input" style="max-width:260px;" placeholder="搜索记录..." v-model="filters.name"
							@input="onSearchInput" />
						<select class="select" style="max-width:160px;" v-model="filters.type" @change="loadRecords">
							<option value="">全部类型</option>
							<option value="A">A</option>
							<option value="AAAA">AAAA</option>
							<option value="CNAME">CNAME</option>
							<option value="TXT">TXT</option>
							<option value="MX">MX</option>
							<option value="NS">NS</option>
						</select>
						<button class="btn primary" @click="syncRecords" :disabled="isLoading">
							{{ isLoading ? '同步中...' : '从 Cloudflare 同步' }}
						</button>
					</div>
				</div>

				<div class="table-container" v-loading="isLoading">
					<table class="table">
						<thead>
							<tr>
								<th>名称</th>
								<th>类型</th>
								<th>内容</th>
								<th>TTL</th>
								<th>代理</th>
								<th>创建用户</th>
								<th>操作</th>
							</tr>
						</thead>
						<tbody>
							<tr v-for="record in filteredRecords" :key="record.id">
								<td class="record-name">{{ record.name }}</td>
								<td><span class="record-type">{{ record.type }}</span></td>
								<td class="record-content">{{ record.content }}</td>
								<td>{{ record.ttl }}</td>
								<td>
									<span class="badge" :class="record.proxied ? 'success' : ''">
										{{ record.proxied ? '是' : '否' }}
									</span>
								</td>
								<td>{{ record.username || '系统' }}</td>
								<td class="row">
									<button class="btn outline" @click="editRecord(record)" :disabled="record.loading">
										编辑
									</button>
									<button class="btn danger" @click="deleteRecord(record)" :disabled="record.loading">
										删除
									</button>
								</td>
							</tr>
						</tbody>
					</table>

					<div v-if="!isLoading && filteredRecords.length === 0" class="empty-state">
						<p>暂无 DNS 记录</p>
						<button class="btn primary" @click="showAddDialog">添加记录</button>
					</div>
				</div>
			</div>

			<!-- 未选择 Zone 提示 -->
			<div v-else-if="zones.length > 0" class="no-zone-selected">
				<p>请选择一个 Zone 来查看 DNS 记录</p>
			</div>
		</div>

		<!-- 添加/编辑记录对话框 -->
		<el-dialog :model-value="recordDialogVisible" @update:model-value="recordDialogVisible = $event"
			:title="editingRecord ? '编辑 DNS 记录' : '添加 DNS 记录'" width="600px">
			<el-form :model="recordForm" :rules="recordRules" ref="recordFormRef" label-width="100px">
				<el-form-item label="记录名称" prop="name">
					<el-input v-model="recordForm.name" placeholder="例如：www 或 @" />
				</el-form-item>
				<el-form-item label="记录类型" prop="type">
					<el-select v-model="recordForm.type" style="width: 100%;">
						<el-option label="A" value="A" />
						<el-option label="AAAA" value="AAAA" />
						<el-option label="CNAME" value="CNAME" />
						<el-option label="TXT" value="TXT" />
						<el-option label="MX" value="MX" />
						<el-option label="NS" value="NS" />
					</el-select>
				</el-form-item>
				<el-form-item label="记录内容" prop="content">
					<el-input v-model="recordForm.content" placeholder="IP 地址、域名或文本内容" />
				</el-form-item>
				<el-form-item label="TTL" prop="ttl">
					<el-input-number v-model="recordForm.ttl" :min="1" :max="86400" style="width: 100%;" />
				</el-form-item>
				<el-form-item label="代理状态" prop="proxied">
					<el-switch v-model="recordForm.proxied" :disabled="!isProxiedSupported(recordForm.type)" />
					<span class="form-tip" v-if="!isProxiedSupported(recordForm.type)">
						该记录类型不支持代理
					</span>
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="recordDialogVisible = false">取消</el-button>
				<el-button type="primary" @click="saveRecord" :loading="isLoading">
					{{ editingRecord ? '更新' : '添加' }}
				</el-button>
			</template>
		</el-dialog>
	</main>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiGet, apiPost, apiPut, apiDelete } from '@/utils/api.js'
import { useAuthStore } from '@/stores/auth.js'

const route = useRoute()

// 认证store
const authStore = useAuthStore()

// 响应式数据
const isLoading = ref(false)
const records = ref([])
const zones = ref([])
const currentZone = ref(null)
const recordDialogVisible = ref(false)
const editingRecord = ref(null)
const recordFormRef = ref()

// 过滤器
const filters = reactive({
	name: '',
	type: ''
})

// 搜索防抖
let searchTimeout = null

// 表单数据
const recordForm = reactive({
	name: '',
	type: 'A',
	content: '',
	ttl: 120,
	proxied: false
})

// 表单验证规则
const recordRules = {
	name: [
		{ required: true, message: '请输入记录名称', trigger: 'blur' }
	],
	type: [
		{ required: true, message: '请选择记录类型', trigger: 'change' }
	],
	content: [
		{ required: true, message: '请输入记录内容', trigger: 'blur' }
	],
	ttl: [
		{ required: true, message: '请输入 TTL 值', trigger: 'blur' },
		{ type: 'number', min: 1, max: 86400, message: 'TTL 值必须在 1-86400 之间', trigger: 'blur' }
	]
}

// 计算属性
const filteredRecords = computed(() => {
	let result = records.value

	// 按名称搜索
	if (filters.name) {
		const keyword = filters.name.toLowerCase()
		result = result.filter(record =>
			record.name.toLowerCase().includes(keyword) ||
			record.content.toLowerCase().includes(keyword)
		)
	}

	// 按类型过滤
	if (filters.type) {
		result = result.filter(record => record.type === filters.type)
	}

	return result
})

// 检查记录类型是否支持代理
const isProxiedSupported = (type) => {
	return ['A', 'AAAA', 'CNAME'].includes(type)
}

// 加载 Zones 列表
const loadZones = async () => {
	try {
		isLoading.value = true

		const response = await apiGet('/api/admin/zones', { token: authStore.adminToken })
		zones.value = response.data || []

		// 如果从路由参数指定了 Zone，自动选择
		if (route.query.zoneId) {
			const targetZone = zones.value.find(zone => zone.id == route.query.zoneId)
			if (targetZone) {
				selectZone(targetZone)
			}
		} else if (zones.value.length > 0) {
			// 默认选择第一个 Zone
			selectZone(zones.value[0])
		}
	} catch (error) {
		ElMessage.error('加载 Zones 失败: ' + error.message)
	} finally {
		isLoading.value = false
	}
}

// 选择 Zone
const selectZone = (zone) => {
	currentZone.value = zone
	// 清空当前记录
	records.value = []
	// 清空过滤器
	filters.name = ''
	filters.type = ''
	// 加载该 Zone 的记录
	loadRecords()
}

// 前往 Zones 管理页面
const goToZones = () => {
	router.push('/admin/zones')
}

// 加载 DNS 记录
const loadRecords = async () => {
	if (!currentZone.value) return

	try {
		isLoading.value = true

		const params = new URLSearchParams()
		if (filters.type) {
			params.append('type', filters.type)
		}
		if (filters.name) {
			params.append('name', filters.name)
		}

		const response = await apiGet(`/api/admin/zones/${currentZone.value.id}/records?${params.toString()}`, { token: authStore.adminToken })
		records.value = response.data || []
	} catch (error) {
		ElMessage.error('加载 DNS 记录失败: ' + error.message)
	} finally {
		isLoading.value = false
	}
}

// 同步 DNS 记录
const syncRecords = async () => {
	if (!currentZone.value) return

	try {
		isLoading.value = true

		await apiPost(`/api/admin/zones/${currentZone.value.id}/sync-records`, {}, { token: authStore.adminToken })
		ElMessage.success('DNS 记录同步成功')
		await loadRecords()
	} catch (error) {
		ElMessage.error('同步失败: ' + error.message)
	} finally {
		isLoading.value = false
	}
}

// 显示添加记录对话框
const showAddDialog = () => {
	editingRecord.value = null
	Object.assign(recordForm, {
		name: '',
		type: 'A',
		content: '',
		ttl: 120,
		proxied: false
	})
	recordDialogVisible.value = true
}

// 编辑记录
const editRecord = (record) => {
	editingRecord.value = record
	Object.assign(recordForm, {
		name: record.name,
		type: record.type,
		content: record.content,
		ttl: record.ttl,
		proxied: record.proxied
	})
	recordDialogVisible.value = true
}

// 保存记录
const saveRecord = async () => {
	try {
		await recordFormRef.value.validate()
		isLoading.value = true

		const recordData = {
			type: recordForm.type,
			name: recordForm.name,
			content: recordForm.content,
			ttl: recordForm.ttl,
			proxied: recordForm.proxied
		}

		if (editingRecord.value) {
			// 更新记录
			await apiPut(`/api/admin/zones/${currentZone.value.id}/records/${editingRecord.value.cfRecordId}`, recordData, { token: authStore.adminToken })
			ElMessage.success('DNS 记录更新成功')
		} else {
			// 添加记录
			await apiPost(`/api/admin/zones/${currentZone.value.id}/records`, recordData, { token: authStore.adminToken })
			ElMessage.success('DNS 记录添加成功')
		}

		recordDialogVisible.value = false
		await loadRecords()
	} catch (error) {
		ElMessage.error('保存失败: ' + error.message)
	} finally {
		isLoading.value = false
	}
}

// 删除记录
const deleteRecord = async (record) => {
	try {
		await ElMessageBox.confirm(
			`确定要删除记录 "${record.name}" (${record.type}) 吗？`,
			'确认删除',
			{
				confirmButtonText: '确定',
				cancelButtonText: '取消',
				type: 'warning'
			}
		)

		record.loading = true
		await apiDelete(`/api/admin/zones/${currentZone.value.id}/records/${record.cfRecordId}`, { token: authStore.adminToken })
		ElMessage.success('DNS 记录删除成功')
		await loadRecords()
	} catch (error) {
		if (error !== 'cancel') {
			ElMessage.error('删除失败: ' + error.message)
		}
	} finally {
		record.loading = false
	}
}

// 搜索输入处理（防抖）
const onSearchInput = () => {
	if (searchTimeout) {
		clearTimeout(searchTimeout)
	}
	searchTimeout = setTimeout(() => {
		loadRecords()
	}, 500)
}

// 组件挂载时初始化
onMounted(() => {
	loadZones()
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
	margin: 0 0 16px 0;
	font-size: 14px;
}

.record-name {
	font-family: monospace;
	font-size: 13px;
}

.record-type {
	display: inline-block;
	padding: 2px 6px;
	background: #f1f5f9;
	color: #475569;
	border-radius: 4px;
	font-size: 12px;
	font-weight: 600;
}

.record-content {
	max-width: 200px;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.form-tip {
	font-size: 12px;
	color: #64748b;
	margin-left: 8px;
}

/* Zone 标签页样式 */
.zone-tabs {
	margin-bottom: 20px;
}

.tab-list {
	display: flex;
	gap: 8px;
	flex-wrap: wrap;
	border-bottom: 1px solid #e2e8f0;
	padding-bottom: 12px;
}

.tab-item {
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 8px 16px;
	border: 1px solid #e2e8f0;
	border-radius: 8px;
	background: #fff;
	cursor: pointer;
	transition: all 0.2s;
	font-size: 14px;
}

.tab-item:hover {
	border-color: #6366f1;
	background: #f8fafc;
}

.tab-item.active {
	border-color: #6366f1;
	background: #6366f1;
	color: #fff;
}

.zone-name {
	font-weight: 500;
}

.zone-status {
	font-size: 12px;
	padding: 2px 6px;
	border-radius: 4px;
	background: #f1f5f9;
	color: #64748b;
}

.tab-item.active .zone-status {
	background: rgba(255, 255, 255, 0.2);
	color: #fff;
}

.zone-status.enabled {
	background: #ecfdf5;
	color: #065f46;
}

.tab-item.active .zone-status.enabled {
	background: rgba(255, 255, 255, 0.3);
	color: #fff;
}

/* DNS 记录区域 */
.dns-records-section {
	margin-top: 20px;
}

.zone-info {
	display: flex;
	align-items: center;
	gap: 12px;
	margin-bottom: 16px;
	padding: 12px;
	background: #f8fafc;
	border-radius: 8px;
}

.zone-info h4 {
	margin: 0;
	font-size: 16px;
	color: #0f172a;
}

.zone-id {
	font-family: monospace;
	font-size: 12px;
	color: #64748b;
	background: #e2e8f0;
	padding: 2px 6px;
	border-radius: 4px;
}

/* 空状态样式 */
.empty-zones,
.no-zone-selected {
	text-align: center;
	padding: 40px 20px;
	color: #64748b;
}

.empty-zones p,
.no-zone-selected p {
	margin: 0 0 16px 0;
	font-size: 14px;
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

.table tr:hover td {
	background: #f8fafc;
}

.row {
	display: flex;
	gap: 8px;
	align-items: center;
	flex-wrap: nowrap;
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
	outline: none;
}

.input:focus,
.select:focus {
	border-color: #6366f1;
	box-shadow: 0 0 0 3px rgba(99, 102, 241, .15);
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

.btn.danger {
	background: #ef4444;
	color: #fff;
}

.btn.danger:hover {
	background: #dc2626;
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

/* 响应式：移动端友好 */
@media (max-width: 768px) {
	.card {
		padding: 12px;
	}

	.input,
	.select {
		width: 100%;
	}

	.row {
		flex-wrap: wrap;
		gap: 8px;
	}

	/* 表格在小屏可横向滚动，避免内容溢出 */
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

	/* 操作列按钮纵向排列，避免横向滚动 */
	.table td.row {
		flex-direction: column;
		align-items: stretch;
	}

	.table td.row .btn {
		width: 100%;
	}

	/* 顶部工具栏按钮在小屏占满宽度，避免溢出 */
	.row .btn {
		width: 100%;
	}

	/* Zone 标签页响应式 */
	.tab-list {
		flex-direction: column;
		align-items: stretch;
	}

	.tab-item {
		justify-content: space-between;
	}

	.zone-info {
		flex-direction: column;
		align-items: flex-start;
		gap: 8px;
	}
}
</style>