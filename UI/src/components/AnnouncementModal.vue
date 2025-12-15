<template>
    <el-dialog :model-value="visible" @update:model-value="$emit('update:visible', $event)" title="系统公告" width="60%"
        :close-on-click-modal="false" :close-on-press-escape="false" :show-close="false" class="announcement-modal"
        align-center>
        <div class="announcement-content">
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

            <div v-else class="announcement-tabs-container">
                <el-tabs v-model="activeTab" type="card" class="announcement-tabs">
                    <el-tab-pane v-for="announcement in announcements" :key="announcement.id" :name="announcement.id">
                        <template #label>
                            <span class="tab-label">
                                <span class="priority-dot" :class="getPriorityClass(announcement.priority)"></span>
                                {{ announcement.title }}
                            </span>
                        </template>
                        <div class="announcement-detail">
                            <div class="announcement-header">
                                <div class="announcement-title-large">
                                    <span class="priority-badge" :class="getPriorityClass(announcement.priority)">
                                        {{ getPriorityText(announcement.priority) }}
                                    </span>
                                    {{ announcement.title }}
                                </div>
                                <div class="announcement-meta">
                                    <span class="publish-time">发布时间：{{ formatTime(announcement.publishedAt) }}</span>
                                </div>
                            </div>
                            <div class="announcement-body" v-html="announcement.content"></div>
                        </div>
                    </el-tab-pane>
                </el-tabs>
            </div>
        </div>

        <template #footer>
            <div class="dialog-footer">
                <el-button @click="viewAllAnnouncements" type="primary" plain>
                    查看更多历史公告
                </el-button>
                <el-button @click="confirmRead" type="primary">
                    我知道了
                </el-button>
            </div>
        </template>
    </el-dialog>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet } from '@/utils/api.js'
import { ElMessage } from 'element-plus'

const router = useRouter()

// Props
const props = defineProps({
    visible: {
        type: Boolean,
        default: false
    }
})

// Emits
const emit = defineEmits(['update:visible', 'confirm'])

// 响应式数据
const announcements = ref([])
const isLoading = ref(false)
const activeTab = ref('')

// 加载公告
const loadAnnouncements = async () => {
    try {
        isLoading.value = true
        // 增加获取数量，以便在标签页中展示更多
        const response = await apiGet('/api/admin/announcements/published?limit=10')
        let data = response.data || []

        // 按紧急程度排序（3:紧急 > 2:重要 > 1:普通），同级按时间倒序
        data.sort((a, b) => {
            if (b.priority !== a.priority) {
                return b.priority - a.priority
            }
            return new Date(b.publishedAt) - new Date(a.publishedAt)
        })

        announcements.value = data

        // 默认选中第一个
        if (data.length > 0) {
            activeTab.value = data[0].id
        }
    } catch (error) {
        console.error('加载公告失败:', error)
        ElMessage.error('加载公告失败')
    } finally {
        isLoading.value = false
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

// 查看全部公告
const viewAllAnnouncements = () => {
    emit('update:visible', false)
    router.push('/user/announcements')
}

// 确认已读
const confirmRead = () => {
    emit('confirm')
    emit('update:visible', false)
}

// 监听visible变化
watch(() => props.visible, (newVal) => {
    if (newVal) {
        loadAnnouncements()
    }
})

onMounted(() => {
    if (props.visible) {
        loadAnnouncements()
    }
})
</script>

<style scoped>
.announcement-modal :deep(.el-dialog) {
    /* 移除固定宽度，使用百分比，但在小屏幕下保持响应式 */
    max-width: 95vw;
}

.announcement-modal :deep(.el-dialog__header) {
    background: #f8fafc;
    border-bottom: 1px solid #e2e8f0;
    padding: 16px 24px;
}

.announcement-modal :deep(.el-dialog__title) {
    font-size: 18px;
    font-weight: 600;
    color: #0f172a;
}

.announcement-modal :deep(.el-dialog__body) {
    padding: 24px;
    height: 60vh;
    /* 固定高度比例，配合 scroll */
    display: flex;
    flex-direction: column;
}

.announcement-modal :deep(.el-dialog__footer) {
    background: #f8fafc;
    border-top: 1px solid #e2e8f0;
    padding: 16px 24px;
}

.announcement-content {
    flex: 1;
    overflow: hidden;
    /* 内容区域溢出隐藏，由内部 tabs 处理滚动 */
    display: flex;
    flex-direction: column;
}

.empty-state {
    text-align: center;
    padding: 40px 20px;
    color: #64748b;
    margin: auto;
}

.empty-state svg {
    margin-bottom: 16px;
}

.empty-state p {
    margin: 0;
    font-size: 16px;
}

/* Tabs 样式定制 */
.announcement-tabs-container {
    height: 100%;
    display: flex;
    flex-direction: column;
}

.announcement-tabs {
    height: 100%;
    display: flex;
    flex-direction: column;
}

.announcement-tabs :deep(.el-tabs__header) {
    margin-bottom: 20px;
}

.announcement-tabs :deep(.el-tabs__content) {
    flex: 1;
    overflow-y: auto;
    padding-right: 10px;
    /* 滚动条间距 */
}

.tab-label {
    display: flex;
    align-items: center;
    gap: 6px;
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.priority-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
}

/* 公告详情样式 */
.announcement-detail {
    padding: 0 4px;
}

.announcement-header {
    border-bottom: 1px solid #e2e8f0;
    padding-bottom: 16px;
    margin-bottom: 20px;
}

.announcement-title-large {
    font-size: 20px;
    font-weight: 700;
    color: #0f172a;
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
    flex-wrap: wrap;
}

.priority-badge {
    padding: 4px 10px;
    border-radius: 4px;
    font-size: 13px;
    font-weight: 500;
    flex-shrink: 0;
}

.priority-normal {
    background-color: #f1f5f9;
    color: #64748b;
}

.priority-dot.priority-normal {
    background-color: #94a3b8;
}

.priority-important {
    background-color: #fef3c7;
    color: #92400e;
}

.priority-dot.priority-important {
    background-color: #f59e0b;
}

.priority-urgent {
    background-color: #fee2e2;
    color: #dc2626;
}

.priority-dot.priority-urgent {
    background-color: #ef4444;
}

.announcement-meta {
    color: #64748b;
    font-size: 14px;
}

.announcement-body {
    color: #334155;
    line-height: 1.7;
    font-size: 15px;
}

.announcement-body :deep(img) {
    max-width: 100%;
    height: auto;
    border-radius: 6px;
    margin: 10px 0;
}

.announcement-body :deep(p) {
    margin-bottom: 12px;
}

.announcement-body :deep(h1),
.announcement-body :deep(h2),
.announcement-body :deep(h3) {
    margin-top: 20px;
    margin-bottom: 12px;
    color: #0f172a;
    font-weight: 600;
}

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
}

/* 响应式调整 */
@media (max-width: 768px) {
    .announcement-modal :deep(.el-dialog) {
        width: 90% !important;
        /* 手机端宽度占比更大 */
    }

    .announcement-title-large {
        font-size: 18px;
    }

    .tab-label {
        max-width: 120px;
    }
}
</style>
