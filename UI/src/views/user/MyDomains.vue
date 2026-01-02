<template>
	<div class="domains-container" v-loading="isLoading">
		<!-- SEO组件 -->
		<SEOHead pageName="myDomains" />
		<div class="card">
			<div class="card-header">
				<h3>我的域名</h3>
				<div class="header-actions">
					<button class="btn primary" @click="refreshDomains">
						<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
							<path
								d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z" />
						</svg>
						刷新
					</button>
				</div>
			</div>

			<div v-if="domains.length === 0 && !isLoading" class="empty-state">
				<svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor" style="color: #9ca3af;">
					<path
						d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
				</svg>
				<p>暂无域名记录</p>
				<router-link to="/user/apply" class="btn primary">申请域名</router-link>
			</div>

			<table class="table" v-else>
				<thead>
					<tr>
						<th>域名</th>
						<th>记录类型</th>
						<th>记录值</th>
						<th>TTL</th>
						<th>创建时间</th>
						<th>状态</th>
						<th>操作</th>
					</tr>
				</thead>
				<tbody>
					<tr v-for="domain in domains" :key="domain.id">
						<td data-label="域名" class="domain-cell">
						<div class="domain-with-tag">
							<span>{{ domain.fullDomain }}</span>
							<span v-if="isExclusiveDomain(domain)" class="exclusive-tag">专属域名</span>
						</div>
					</td>
						<td data-label="记录类型">{{ getRecordType(domain) }}</td>
						<td data-label="记录值">{{ getRecordValue(domain) }}</td>
						<td data-label="TTL">{{ getRecordTtl(domain) }}</td>
						<td data-label="创建时间">{{ formatTime(domain.createdAt) }}</td>
						<td data-label="状态">
							<span class="badge" :class="getStatusClass(domain.status)">{{ domain.status }}</span>
						</td>
						<td data-label="操作" class="row">
						<button class="btn outline" @click="editRecord(domain)" :disabled="isDeleting">
							编辑记录
						</button>
							<button class="btn danger" @click="releaseDomain(domain)" :disabled="isDeleting">
								释放
							</button>
						</td>
					</tr>
				</tbody>
			</table>

			<!-- 分页 -->
			<div class="pagination" v-if="totalPages > 1">
				<button class="btn outline" @click="goToPage(currentPage - 1)" :disabled="currentPage <= 1">
					上一页
				</button>
				<span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
				<button class="btn outline" @click="goToPage(currentPage + 1)" :disabled="currentPage >= totalPages">
					下一页
				</button>
			</div>
		</div>

		<!-- 编辑记录对话框 -->
		<div class="modal" v-if="showEditModal" @click="closeEditModal">
			<div class="modal-content" @click.stop>
				<h3>编辑DNS记录</h3>
				<div class="form-group">
					<label>域名：{{ editingDomain?.fullDomain }}</label>
				</div>
				<div class="form-group">
					<label>记录类型</label>
					<select v-model="editForm.type" class="select">
						<option value="A">A (IPv4地址)</option>
						<option value="AAAA">AAAA (IPv6地址)</option>
						<option value="CNAME">CNAME (别名)</option>
						<option value="TXT">TXT (文本记录)</option>
					</select>
				</div>
				<div class="form-group" v-if="editForm.type === 'NS'">
					<label>记录值 (NS1)</label>
					<input v-model="editForm.value" type="text" class="input" placeholder="请输入 NS1 记录值" />
				</div>
				<div class="form-group" v-if="editForm.type === 'NS'">
					<label>记录值 (NS2)</label>
					<input v-model="editForm.value2" type="text" class="input" placeholder="请输入 NS2 记录值" />
				</div>
				<div class="form-group" v-else>
					<label>记录值</label>
					<input v-model="editForm.value" type="text" class="input" placeholder="请输入记录值" />
				</div>
				<div class="form-group">
					<label>TTL (秒)</label>
					<input v-model="editForm.ttl" type="number" class="input" placeholder="120" min="60" max="86400" />
				</div>
				<div class="form-group">
					<label>备注</label>
					<textarea v-model="editForm.remark" placeholder="请输入备注信息" class="textarea"></textarea>
				</div>
				<div class="modal-actions">
					<button class="btn outline" @click="closeEditModal">取消</button>
					<button class="btn primary" @click="saveRecord" :disabled="isSaving">
						{{ isSaving ? '保存中...' : '保存' }}
					</button>
				</div>
			</div>
		</div>
	</div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth.js'
import { apiGet, apiDelete, apiPut } from '@/utils/api.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import SEOHead from '@/components/SEOHead.vue'

const authStore = useAuthStore()

// 响应式数据
const isLoading = ref(false)
const isDeleting = ref(false)
const isSaving = ref(false)
const domains = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(1)

// 编辑记录相关
const showEditModal = ref(false)
const editingDomain = ref(null)
const editForm = ref({
	type: 'A',
	value: '',
	ttl: 120,
	remark: ''
})

// 加载域名列表
const loadDomains = async (page = 1) => {
	isLoading.value = true
	try {
		const response = await apiGet('/api/user/domains', {
			token: authStore.token,
			params: { page, size: pageSize.value }
		})

		if (response.data) {
			domains.value = response.data.list || []
			currentPage.value = response.data.page || 1
			totalPages.value = Math.ceil((response.data.total || 0) / pageSize.value)
		}
	} catch (error) {
		console.error('加载域名列表失败:', error)
		ElMessage.error('加载域名列表失败')
	} finally {
		isLoading.value = false
	}
}

// 刷新域名列表
const refreshDomains = () => {
	loadDomains(currentPage.value)
}

// 分页
const goToPage = (page) => {
	if (page >= 1 && page <= totalPages.value) {
		loadDomains(page)
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

// 获取记录类型
const getRecordType = (domain) => {
	return domain.recordType || 'A'
}

//获取记录值
const getRecordValue = (domain) => {
	// 对于NS记录，可能包含两条记录值（用空格分隔）
	if (domain.recordType === 'NS' && domain.recordValue && domain.recordValue.includes(' ')) {
		const values = domain.recordValue.split(' ')
		return values.join(', ') // 用逗号分隔显示
	}
	return domain.recordValue || '1.2.3.4'
}

// 获取记录TTL
const getRecordTtl = (domain) => {
	return domain.recordTtl || 120
}

// 判断是否为专属域名（NS记录类型）
const isExclusiveDomain = (domain) => {
	return domain.recordType === 'NS'
}

// 编辑记录
const editRecord = (domain) => {
	editingDomain.value = domain
	
	let val1 = domain.recordValue || ''
	let val2 = ''
	
	if (domain.recordType === 'NS' && val1.includes(' ')) {
		const parts = val1.split(' ')
		val1 = parts[0]
		val2 = parts.slice(1).join(' ')
	}

	editForm.value = {
		type: domain.recordType || 'A',
		value: val1,
		value2: val2,
		ttl: domain.recordTtl || 120,
		remark: domain.remark || ''
	}
	showEditModal.value = true
}

// 关闭编辑对话框
const closeEditModal = () => {
	showEditModal.value = false
	editingDomain.value = null
	editForm.value = {
		type: 'A',
		value: '',
		value2: '',
		ttl: 120,
		remark: ''
	}
}

// 保存记录
const saveRecord = async () => {
	if (!editingDomain.value) return

	isSaving.value = true
	try {
		let finalValue = editForm.value.value
		if (editForm.value.type === 'NS' && editForm.value.value2) {
			finalValue = finalValue + ' ' + editForm.value.value2
		}

		const response = await apiPut(`/api/user/domains/${editingDomain.value.id}`, {
			type: editForm.value.type,
			value: finalValue,
			ttl: editForm.value.ttl,
			remark: editForm.value.remark
		}, { token: authStore.token })

		if (response.code === 200) {
			ElMessage.success('记录保存成功')
			// 更新本地数据
			const domain = domains.value.find(d => d.id === editingDomain.value.id)
			if (domain) {
				domain.recordType = editForm.value.type
				domain.recordValue = finalValue
				domain.recordTtl = editForm.value.ttl
				domain.remark = editForm.value.remark
			}
			closeEditModal()
		} else {
			ElMessage.error(response.message || '保存失败')
		}
	} catch (error) {
		ElMessage.error('保存失败')
		console.error('保存记录失败:', error)
	} finally {
		isSaving.value = false
	}
}

// 释放域名
const releaseDomain = async (domain) => {
	try {
		await ElMessageBox.confirm(
			`确定要释放域名 "${domain.fullDomain}" 吗？释放后将删除DNS记录并返还50%积分。`,
			'确认释放',
			{
				confirmButtonText: '确定',
				cancelButtonText: '取消',
				type: 'warning'
			}
		)

		isDeleting.value = true
		const response = await apiDelete(`/api/user/domains/${domain.id}`, { token: authStore.token })

		if (response.code === 200) {
			ElMessage.success('域名释放成功')
			// 重新加载列表
			loadDomains(currentPage.value)
		} else {
			ElMessage.error(response.message || '释放失败')
		}
	} catch (error) {
		if (error !== 'cancel') {
			ElMessage.error('释放失败')
			console.error('释放域名失败:', error)
		}
	} finally {
		isDeleting.value = false
	}
}

onMounted(() => {
	loadDomains()
})
</script>
<style scoped>
.domains-container {
	padding: 20px;
}

.card-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 20px;
}

.header-actions {
	display: flex;
	gap: 10px;
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
	margin: 0 0 20px 0;
	font-size: 16px;
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

.pagination {
	display: flex;
	justify-content: center;
	align-items: center;
	gap: 16px;
	margin-top: 20px;
	padding: 20px 0;
}

.page-info {
	font-size: 14px;
	color: #64748b;
}

/* 模态框样式 - 优化性能版本 */
.modal {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(0, 0, 0, 0.5);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 1000;
}

.modal-content {
	background: #ffffff;
	border: 1px solid #e2e8f0;
	border-radius: 12px;
	padding: 24px;
	width: 90%;
	max-width: 500px;
	box-shadow:
		0 10px 25px rgba(0, 0, 0, 0.1),
		0 4px 10px rgba(0, 0, 0, 0.05);
}

.modal-content h3 {
	margin: 0 0 20px 0;
	font-size: 18px;
	font-weight: 600;
	color: #151717;
}

.form-group {
	margin-bottom: 20px;
}

.form-group label {
	display: block;
	margin-bottom: 8px;
	font-size: 14px;
	font-weight: 500;
	color: #374151;
}

/* 优化性能的表单输入框样式 */
.input,
.select,
.textarea {
	width: 100%;
	padding: 12px 16px;
	border: 1px solid #d1d5db;
	border-radius: 8px;
	font-size: 14px;
	background: #ffffff;
	color: #374151;
	outline: none;
	transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.input:focus,
.select:focus,
.textarea:focus {
	border-color: #6366f1;
	box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.textarea {
	min-height: 80px;
	resize: vertical;
}

.select {
	cursor: pointer;
}

.modal-actions {
	display: flex;
	justify-content: flex-end;
	gap: 12px;
	margin-top: 24px;
}

/* 优化性能的按钮样式 */
.btn {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	gap: 8px;
	padding: 10px 20px;
	border-radius: 8px;
	border: 1px solid transparent;
	font-size: 14px;
	font-weight: 500;
	cursor: pointer;
	transition: all 0.2s ease;
	text-decoration: none;
	min-height: 40px;
}

.btn.primary {
	background: #6366f1;
	color: #ffffff;
	border-color: #6366f1;
}

.btn.primary:hover:not(:disabled) {
	background: #5b5bd6;
	border-color: #5b5bd6;
	transform: translateY(-1px);
	box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.btn.outline {
	background: #ffffff;
	color: #374151;
	border-color: #d1d5db;
}

.btn.outline:hover:not(:disabled) {
	background: #f9fafb;
	border-color: #9ca3af;
	transform: translateY(-1px);
}

.btn.danger {
	background: #ef4444;
	color: #ffffff;
	border-color: #ef4444;
}

.btn.danger:hover:not(:disabled) {
	background: #dc2626;
	border-color: #dc2626;
	transform: translateY(-1px);
	box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.btn:disabled {
	background: #f3f4f6;
	color: #9ca3af;
	border-color: #e5e7eb;
	cursor: not-allowed;
	transform: none;
	box-shadow: none;
}

/* 专属域名橙色小标签样式 */
.domain-with-tag {
	display: flex;
	align-items: center;
	gap: 8px;
}

.exclusive-tag {
	background: linear-gradient(135deg, #ff7e5f 0%, #feb47b 100%);
	color: white;
	font-size: 10px;
	font-weight: bold;
	padding: 2px 6px;
	border-radius: 12px;
	white-space: nowrap;
	box-shadow: 0 2px 4px rgba(255, 126, 95, 0.3);
	animation: pulse 2s infinite;
}

@keyframes pulse {
	0% {
		transform: scale(1);
	}
	50% {
		transform: scale(1.05);
	}
	100% {
		transform: scale(1);
	}
}

/* 响应式设计优化 */
@media (max-width: 768px) {
	.modal-content {
		width: 95%;
		padding: 20px;
		margin: 20px;
	}

	.modal-actions {
		flex-direction: column;
		gap: 8px;
	}

	.btn {
		width: 100%;
		justify-content: center;
	}

	.form-group {
		margin-bottom: 16px;
	}

	.input,
	.select,
	.textarea {
		padding: 10px 12px;
		font-size: 16px;
		/* 防止iOS缩放 */
	}
}

@media (max-width: 480px) {
	.modal-content {
		padding: 16px;
		margin: 10px;
	}

	.modal-content h3 {
		font-size: 16px;
		margin-bottom: 16px;
	}
}
</style>