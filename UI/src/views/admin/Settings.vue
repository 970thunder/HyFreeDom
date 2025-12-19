<template>
	<main class="main container">
		<div class="grid cols-2">
			<div class="card">
				<div class="card-header">
					<h3>积分策略</h3>
					<button class="btn outline" @click="loadSettings" :disabled="isLoading">
						{{ isLoading ? '加载中...' : '刷新' }}
					</button>
				</div>
				<div class="form">
					<div class="grid cols-2">
						<div class="input-row">
							<label class="label">注册赠送积分</label>
							<input class="input" v-model.number="settings.initial_register_points" type="number"
								min="0" />
						</div>
						<div class="input-row">
							<label class="label">邀请人奖励</label>
							<input class="input" v-model.number="settings.inviter_points" type="number" min="0" />
						</div>
						<div class="input-row">
							<label class="label">被邀请人奖励</label>
							<input class="input" v-model.number="settings.invitee_points" type="number" min="0" />
						</div>
						<div class="input-row">
							<label class="label">申请域名消耗</label>
							<input class="input" v-model.number="settings.domain_cost_points" type="number" min="1" />
						</div>
						<div class="input-row">
							<label class="label">单用户域名上限</label>
							<input class="input" v-model.number="settings.max_domains_per_user" type="number" min="1" />
						</div>
						<div class="input-row">
							<label class="label">实名赠送积分</label>
							<input class="input" v-model.number="settings.verification_reward_points" type="number"
								min="0" />
						</div>
					</div>
					<div class="row">
						<button class="btn primary" @click="saveSettings" :disabled="isLoading">
							{{ isLoading ? '保存中...' : '保存' }}
						</button>
						<button class="btn outline" @click="resetSettings" :disabled="isLoading">
							重置
						</button>
					</div>
				</div>
			</div>

			<div class="card">
				<div class="card-header">
					<h3>同步与任务</h3>
					<button class="btn primary" @click="triggerSync" :disabled="isLoading">
						{{ isLoading ? '同步中...' : '立即触发同步' }}
					</button>
				</div>
				<div class="form">
					<div class="input-row">
						<label class="label">同步 Cron 表达式</label>
						<input class="input" v-model="settings.sync_cron_expression" placeholder="0 */5 * * * *" />
						<div class="form-tip">
							格式：秒 分 时 日 月 周，如 "0 */5 * * * *" 表示每5分钟执行一次
						</div>
					</div>
					<div class="input-row">
						<label class="label">默认 TTL</label>
						<input class="input" v-model.number="settings.default_ttl" type="number" min="1" max="86400" />
						<div class="form-tip">
							DNS 记录的默认生存时间（秒），范围：1-86400
						</div>
					</div>
				</div>
			</div>
		</div>

		<div class="card" style="margin-top:16px;">
			<div class="card-header">
				<h3>系统信息</h3>
				<button class="btn outline" @click="loadStats" :disabled="isLoading">
					{{ isLoading ? '加载中...' : '刷新' }}
				</button>
			</div>
			<div class="form">
				<div class="grid cols-2">
					<div class="info-item">
						<label class="label">系统版本</label>
						<div class="info-value">v1.0.0</div>
					</div>
					<div class="info-item">
						<label class="label">最后同步时间</label>
						<div class="info-value">{{ lastSyncTime || '未同步' }}</div>
					</div>
					<div class="info-item">
						<label class="label">Cloudflare 账户数</label>
						<div class="info-value">{{ cfAccountCount }}</div>
					</div>
					<div class="info-item">
						<label class="label">启用 Zone 数</label>
						<div class="info-value">{{ enabledZoneCount }}</div>
					</div>
				</div>
			</div>
		</div>

		<!-- 公告管理 -->
		<div class="card" style="margin-top:16px;">
			<div class="card-header">
				<h3>公告管理</h3>
				<button class="btn primary" @click="showCreateAnnouncementDialog">
					<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
						<line x1="12" y1="5" x2="12" y2="19"></line>
						<line x1="5" y1="12" x2="19" y2="12"></line>
					</svg>
					新建公告
				</button>
			</div>

			<div v-if="announcements.length === 0" class="empty-state">
				<svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor" style="color: #9ca3af;">
					<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
					<polyline points="14,2 14,8 20,8"></polyline>
					<line x1="16" y1="13" x2="8" y2="13"></line>
					<line x1="16" y1="17" x2="8" y2="17"></line>
					<polyline points="10,9 9,9 8,9"></polyline>
				</svg>
				<p>暂无公告</p>
			</div>

			<div v-else class="announcement-list">
				<div v-for="announcement in announcements" :key="announcement.id" class="announcement-item">
					<div class="announcement-header">
						<div class="announcement-title">
							<span class="priority-badge" :class="getPriorityClass(announcement.priority)">
								{{ getPriorityText(announcement.priority) }}
							</span>
							{{ announcement.title }}
						</div>
						<div class="announcement-actions">
							<span class="status-badge" :class="getStatusClass(announcement.status)">
								{{ getStatusText(announcement.status) }}
							</span>
							<button class="btn-icon" @click="editAnnouncement(announcement)" title="编辑">
								<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
									stroke-width="2">
									<path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
									<path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
								</svg>
							</button>
							<button class="btn-icon" @click="deleteAnnouncement(announcement.id)" title="删除">
								<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
									stroke-width="2">
									<polyline points="3,6 5,6 21,6"></polyline>
									<path
										d="M19,6v14a2,2 0 0,1 -2,2H7a2,2 0 0,1 -2,-2V6m3,0V4a2,2 0 0,1 2,-2h4a2,2 0 0,1 2,2v2">
									</path>
								</svg>
							</button>
						</div>
					</div>
					<div class="announcement-content" v-html="announcement.content"></div>
					<div class="announcement-meta">
						<span>创建时间：{{ formatTime(announcement.createdAt) }}</span>
						<span v-if="announcement.publishedAt">发布时间：{{ formatTime(announcement.publishedAt) }}</span>
						<span>创建者：{{ announcement.createdByUsername }}</span>
					</div>
				</div>
			</div>
		</div>

		<!-- 创建/编辑公告对话框 -->
		<el-dialog :model-value="announcementDialogVisible" @update:model-value="announcementDialogVisible = $event"
			:title="isEditing ? '编辑公告' : '新建公告'" width="600px" :close-on-click-modal="false">
			<el-form :model="announcementForm" :rules="announcementRules" ref="announcementFormRef" label-width="80px">
				<el-form-item label="标题" prop="title">
					<el-input v-model="announcementForm.title" placeholder="请输入公告标题" maxlength="200" show-word-limit />
				</el-form-item>
				<el-form-item label="优先级" prop="priority">
					<el-select v-model="announcementForm.priority" placeholder="请选择优先级">
						<el-option label="普通" :value="1"></el-option>
						<el-option label="重要" :value="2"></el-option>
						<el-option label="紧急" :value="3"></el-option>
					</el-select>
				</el-form-item>
				<el-form-item label="状态" prop="status">
					<el-select v-model="announcementForm.status" placeholder="请选择状态">
						<el-option label="草稿" value="DRAFT"></el-option>
						<el-option label="已发布" value="PUBLISHED"></el-option>
						<el-option label="已归档" value="ARCHIVED"></el-option>
					</el-select>
				</el-form-item>
				<el-form-item label="内容" prop="content">
					<div class="rich-editor-container">
						<QuillEditor v-model:content="announcementForm.content" content-type="html"
							:options="editorOptions" placeholder="请输入公告内容" />
					</div>
				</el-form-item>
			</el-form>
			<template #footer>
				<div class="dialog-footer">
					<el-button @click="announcementDialogVisible = false">取消</el-button>
					<el-button type="primary" @click="saveAnnouncement" :loading="isSaving">保存</el-button>
				</div>
			</template>
		</el-dialog>
	</main>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { apiGet, apiPut, apiPost, apiDelete } from '@/utils/api.js'
import { useAuthStore } from '@/stores/auth.js'

// 认证store
const authStore = useAuthStore()

// 响应式数据
const isLoading = ref(false)
const lastSyncTime = ref('')
const cfAccountCount = ref(0)
const enabledZoneCount = ref(0)

// 公告相关
const announcements = ref([])
const announcementDialogVisible = ref(false)
const isEditing = ref(false)
const isSaving = ref(false)
const currentAnnouncementId = ref(null)

// 系统设置
const settings = reactive({
	initial_register_points: 5,
	inviter_points: 3,
	invitee_points: 3,
	domain_cost_points: 10,
	max_domains_per_user: 5,
	verification_reward_points: 15,
	default_ttl: 120,
	sync_cron_expression: '0 */5 * * * *'
})

// 默认设置（用于重置）
const defaultSettings = {
	initial_register_points: 5,
	inviter_points: 3,
	invitee_points: 3,
	domain_cost_points: 10,
	max_domains_per_user: 5,
	verification_reward_points: 15,
	default_ttl: 120,
	sync_cron_expression: '0 */5 * * * *'
}

// 公告表单
const announcementForm = reactive({
	title: '',
	content: '',
	status: 'DRAFT',
	priority: 1
})

// 公告表单验证规则
const announcementRules = {
	title: [
		{ required: true, message: '请输入公告标题', trigger: 'blur' },
		{ max: 200, message: '标题长度不能超过200个字符', trigger: 'blur' }
	],
	content: [
		{ required: true, message: '请输入公告内容', trigger: 'blur' }
	],
	status: [
		{ required: true, message: '请选择状态', trigger: 'change' }
	],
	priority: [
		{ required: true, message: '请选择优先级', trigger: 'change' }
	]
}

// 富文本编辑器配置
const editorOptions = {
	theme: 'snow',
	modules: {
		toolbar: [
			[{ 'header': [1, 2, 3, 4, 5, 6, false] }],
			['bold', 'italic', 'underline', 'strike'],
			[{ 'color': [] }, { 'background': [] }],
			[{ 'list': 'ordered' }, { 'list': 'bullet' }],
			[{ 'indent': '-1' }, { 'indent': '+1' }],
			[{ 'align': [] }],
			['link', 'image'],
			['clean']
		]
	},
	placeholder: '请输入公告内容...'
}

// 加载系统设置
const loadSettings = async () => {
	try {
		isLoading.value = true

		const response = await apiGet('/api/admin/settings', { token: authStore.adminToken })
		const data = response.data || {}

		// 更新设置
		Object.assign(settings, {
			initial_register_points: parseInt(data.initial_register_points) || 5,
			inviter_points: parseInt(data.inviter_points) || 3,
			invitee_points: parseInt(data.invitee_points) || 3,
			domain_cost_points: parseInt(data.domain_cost_points) || 10,
			max_domains_per_user: parseInt(data.max_domains_per_user) || 5,
			verification_reward_points: parseInt(data.verification_reward_points) || 15,
			default_ttl: parseInt(data.default_ttl) || 120,
			sync_cron_expression: data.sync_cron_expression || '0 */5 * * * *'
		})

		// 加载统计信息
		await loadStats()

		// 加载公告列表
		await loadAnnouncements()

		ElMessage.success('设置加载成功')
	} catch (error) {
		ElMessage.error('加载设置失败: ' + error.message)
	} finally {
		isLoading.value = false
	}
}

// 加载统计信息
const loadStats = async () => {
	try {
		// 加载 Cloudflare 账户数量
		const cfResponse = await apiGet('/api/admin/cf-accounts', { token: authStore.adminToken })
		cfAccountCount.value = cfResponse.data?.length || 0

		// 加载启用 Zone 数量
		const zonesResponse = await apiGet('/api/admin/zones?enabled=true', { token: authStore.adminToken })
		enabledZoneCount.value = zonesResponse.data?.length || 0
	} catch (error) {
		console.error('加载统计信息失败:', error)
	}
}

// 加载公告列表
const loadAnnouncements = async () => {
	try {
		const response = await apiGet('/api/admin/announcements', { token: authStore.adminToken })
		announcements.value = response.data || []
	} catch (error) {
		console.error('加载公告列表失败:', error)
		ElMessage.error('加载公告列表失败')
	}
}

// 显示创建公告对话框
const showCreateAnnouncementDialog = () => {
	isEditing.value = false
	currentAnnouncementId.value = null
	Object.assign(announcementForm, {
		title: '',
		content: '',
		status: 'DRAFT',
		priority: 1
	})
	announcementDialogVisible.value = true
}

// 编辑公告
const editAnnouncement = (announcement) => {
	isEditing.value = true
	currentAnnouncementId.value = announcement.id
	Object.assign(announcementForm, {
		title: announcement.title,
		content: announcement.content,
		status: announcement.status,
		priority: announcement.priority
	})
	announcementDialogVisible.value = true
}

// 保存公告
const saveAnnouncement = async () => {
	try {
		isSaving.value = true

		if (isEditing.value) {
			// 更新公告
			await apiPut(`/api/admin/announcements/${currentAnnouncementId.value}`, announcementForm, { token: authStore.adminToken })
			ElMessage.success('公告更新成功')
		} else {
			// 创建公告
			await apiPost('/api/admin/announcements', announcementForm, { token: authStore.adminToken })
			ElMessage.success('公告创建成功')
		}

		announcementDialogVisible.value = false
		await loadAnnouncements()
	} catch (error) {
		console.error('保存公告失败:', error)
		ElMessage.error('保存公告失败')
	} finally {
		isSaving.value = false
	}
}

// 删除公告
const deleteAnnouncement = async (id) => {
	try {
		await ElMessageBox.confirm('确定要删除这条公告吗？', '确认删除', {
			confirmButtonText: '确定',
			cancelButtonText: '取消',
			type: 'warning'
		})

		await apiDelete(`/api/admin/announcements/${id}`, { token: authStore.adminToken })
		ElMessage.success('公告删除成功')
		await loadAnnouncements()
	} catch (error) {
		if (error !== 'cancel') {
			console.error('删除公告失败:', error)
			ElMessage.error('删除公告失败')
		}
	}
}

// 格式化时间
const formatTime = (timeStr) => {
	if (!timeStr) return '未知'
	const date = new Date(timeStr)
	return date.toLocaleString('zh-CN')
}

// 获取优先级样式类
const getPriorityClass = (priority) => {
	switch (priority) {
		case 3: return 'priority-urgent'
		case 2: return 'priority-important'
		default: return 'priority-normal'
	}
}

// 获取优先级文本
const getPriorityText = (priority) => {
	switch (priority) {
		case 3: return '紧急'
		case 2: return '重要'
		default: return '普通'
	}
}

// 获取状态样式类
const getStatusClass = (status) => {
	switch (status) {
		case 'PUBLISHED': return 'status-published'
		case 'ARCHIVED': return 'status-archived'
		default: return 'status-draft'
	}
}

// 获取状态文本
const getStatusText = (status) => {
	switch (status) {
		case 'PUBLISHED': return '已发布'
		case 'ARCHIVED': return '已归档'
		default: return '草稿'
	}
}

// 保存系统设置
const saveSettings = async () => {
	try {
		isLoading.value = true

		// 验证设置
		if (settings.domain_cost_points < 1) {
			ElMessage.error('申请域名消耗积分必须大于 0')
			return
		}
		if (settings.max_domains_per_user < 1) {
			ElMessage.error('单用户域名上限必须大于 0')
			return
		}
		if (settings.verification_reward_points < 0) {
			ElMessage.error('实名赠送积分不能小于 0')
			return
		}
		if (settings.default_ttl < 1 || settings.default_ttl > 86400) {
			ElMessage.error('默认 TTL 必须在 1-86400 之间')
			return
		}

		// 转换数值为字符串（后端要求）
		const settingsData = {
			initial_register_points: settings.initial_register_points.toString(),
			inviter_points: settings.inviter_points.toString(),
			invitee_points: settings.invitee_points.toString(),
			domain_cost_points: settings.domain_cost_points.toString(),
			max_domains_per_user: settings.max_domains_per_user.toString(),
			verification_reward_points: settings.verification_reward_points.toString(),
			default_ttl: settings.default_ttl.toString(),
			sync_cron_expression: settings.sync_cron_expression
		}

		await apiPut('/api/admin/settings', settingsData, { token: authStore.adminToken })
		ElMessage.success('设置保存成功')
	} catch (error) {
		ElMessage.error('保存设置失败: ' + error.message)
	} finally {
		isLoading.value = false
	}
}

// 重置设置
const resetSettings = () => {
	Object.assign(settings, defaultSettings)
	ElMessage.info('设置已重置为默认值')
}

// 触发同步
const triggerSync = async () => {
	try {
		isLoading.value = true

		await apiPost('/api/admin/zones/sync', {}, { token: authStore.adminToken })
		lastSyncTime.value = new Date().toLocaleString('zh-CN')
		ElMessage.success('同步任务已触发')

		// 重新加载统计信息
		await loadStats()
	} catch (error) {
		ElMessage.error('触发同步失败: ' + error.message)
	} finally {
		isLoading.value = false
	}
}

// 组件挂载时加载数据
onMounted(() => {
	loadSettings()
})
</script>

<style scoped>
.grid {
	display: grid;
	gap: 16px;
}

.grid.cols-2 {
	grid-template-columns: repeat(2, minmax(0, 1fr));
}

.grid.cols-3 {
	grid-template-columns: repeat(3, minmax(0, 1fr));
}

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

.info-item {
	display: flex;
	flex-direction: column;
	gap: 4px;
}

.info-value {
	font-size: 14px;
	color: #0f172a;
	font-weight: 500;
}

.form-tip {
	font-size: 12px;
	color: #64748b;
	margin-top: 4px;
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

/* 响应式：两列网格在窄屏改为单列 */
@media (max-width: 960px) {
	.grid.cols-2 {
		grid-template-columns: 1fr;
	}
}

/* 公告管理样式 */
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

.announcement-list {
	display: flex;
	flex-direction: column;
	gap: 16px;
}

.announcement-item {
	border: 1px solid #e2e8f0;
	border-radius: 8px;
	padding: 16px;
	background: #fff;
	transition: box-shadow 0.2s ease;
}

.announcement-item:hover {
	box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.announcement-header {
	display: flex;
	justify-content: space-between;
	align-items: flex-start;
	margin-bottom: 12px;
}

.announcement-title {
	display: flex;
	align-items: center;
	gap: 8px;
	font-size: 16px;
	font-weight: 600;
	color: #0f172a;
	flex: 1;
}

.priority-badge {
	padding: 2px 6px;
	border-radius: 4px;
	font-size: 12px;
	font-weight: 500;
}

.priority-normal {
	background-color: #f1f5f9;
	color: #64748b;
}

.priority-important {
	background-color: #fef3c7;
	color: #92400e;
}

.priority-urgent {
	background-color: #fee2e2;
	color: #dc2626;
}

.announcement-actions {
	display: flex;
	align-items: center;
	gap: 8px;
}

.status-badge {
	padding: 4px 8px;
	border-radius: 4px;
	font-size: 12px;
	font-weight: 500;
}

.status-draft {
	background-color: #f1f5f9;
	color: #64748b;
}

.status-published {
	background-color: #dcfce7;
	color: #166534;
}

.status-archived {
	background-color: #f3f4f6;
	color: #6b7280;
}

.btn-icon {
	background: none;
	border: none;
	color: #64748b;
	cursor: pointer;
	padding: 4px;
	border-radius: 4px;
	transition: background-color 0.2s ease;
}

.btn-icon:hover {
	background-color: #f1f5f9;
	color: #0f172a;
}

.announcement-content {
	color: #475569;
	line-height: 1.6;
	margin-bottom: 12px;
}

.announcement-content h1,
.announcement-content h2,
.announcement-content h3,
.announcement-content h4,
.announcement-content h5,
.announcement-content h6 {
	margin: 0 0 8px 0;
	font-weight: 600;
}

.announcement-content p {
	margin: 0 0 8px 0;
}

.announcement-content ul,
.announcement-content ol {
	margin: 0 0 8px 0;
	padding-left: 20px;
}

.announcement-content li {
	margin: 0 0 4px 0;
}

.announcement-content a {
	color: #6366f1;
	text-decoration: none;
}

.announcement-content a:hover {
	text-decoration: underline;
}

.announcement-content img {
	max-width: 100%;
	height: auto;
	border-radius: 4px;
}

.announcement-content blockquote {
	margin: 0 0 8px 0;
	padding: 8px 12px;
	background: #f8fafc;
	border-left: 4px solid #e2e8f0;
	border-radius: 0 4px 4px 0;
}

.announcement-content code {
	background: #f1f5f9;
	padding: 2px 4px;
	border-radius: 3px;
	font-family: 'Courier New', monospace;
	font-size: 0.9em;
}

.announcement-content pre {
	background: #f1f5f9;
	padding: 12px;
	border-radius: 4px;
	overflow-x: auto;
	margin: 0 0 8px 0;
}

.announcement-content pre code {
	background: none;
	padding: 0;
}

.announcement-meta {
	display: flex;
	gap: 16px;
	font-size: 12px;
	color: #94a3b8;
	flex-wrap: wrap;
}

.dialog-footer {
	display: flex;
	justify-content: flex-end;
	gap: 12px;
}

/* 富文本编辑器样式 */
.rich-editor-container {
	border: 1px solid #cbd5e1;
	border-radius: 8px;
	overflow: hidden;
}

.rich-editor-container .ql-editor {
	min-height: 200px;
	font-size: 14px;
	line-height: 1.5;
}

.rich-editor-container .ql-toolbar {
	border-bottom: 1px solid #e2e8f0;
	background: #f8fafc;
}

.rich-editor-container .ql-container {
	border: none;
}

.rich-editor-container .ql-editor.ql-blank::before {
	color: #94a3b8;
	font-style: normal;
}

@media (max-width: 768px) {
	.card {
		padding: 12px;
	}

	.form {
		max-width: 100%;
	}

	.announcement-header {
		flex-direction: column;
		gap: 12px;
		align-items: flex-start;
	}

	.announcement-actions {
		width: 100%;
		justify-content: flex-end;
	}

	.announcement-meta {
		flex-direction: column;
		gap: 4px;
	}
}
</style>