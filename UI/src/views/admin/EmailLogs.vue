<template>
  <main class="main container">
    <div class="card">
      <div class="card-header">
        <h3>验证码发送日志</h3>
        <button class="btn outline" @click="loadLogs" :disabled="loading">刷新</button>
      </div>
      <div class="filters">
        <input v-model.trim="filters.email" class="input" placeholder="按收件邮箱筛选" @keyup.enter="search" />
        <input v-model.trim="filters.ip" class="input" placeholder="按来源 IP 筛选" @keyup.enter="search" />
        <button class="btn primary" @click="search">查询</button>
      </div>
      <div class="table-container" v-loading="loading">
        <table class="table">
          <thead><tr><th>时间</th><th>类型</th><th>收件邮箱</th><th>结果</th><th>来源 IP</th><th>详情</th></tr></thead>
          <tbody>
            <tr v-for="item in logs" :key="item.id">
              <td>{{ formatDate(item.createdAt) }}</td>
              <td>{{ typeLabel(item.action) }}</td>
              <td>{{ item.target }}</td>
              <td><span class="badge" :class="statusClass(item.action)">{{ statusLabel(item.action) }}</span></td>
              <td>{{ item.ip || '-' }}</td>
              <td class="detail">{{ item.detail || '-' }}</td>
            </tr>
          </tbody>
        </table>
        <div v-if="!loading && logs.length === 0" class="empty-state"><p>暂无邮件日志</p></div>
        <el-pagination v-if="total > 0" class="pagination" background layout="prev, pager, next, sizes, total"
          :total="total" :page-size="filters.size" :current-page="filters.page" :page-sizes="[10, 20, 50]"
          @current-change="page => { filters.page = page; loadLogs() }"
          @size-change="size => { filters.size = size; filters.page = 1; loadLogs() }" />
      </div>
    </div>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiGet } from '@/utils/api.js'
import { useAuthStore } from '@/stores/auth.js'

const authStore = useAuthStore()
const loading = ref(false)
const logs = ref([])
const total = ref(0)
const filters = reactive({ email: '', ip: '', page: 1, size: 20 })

const loadLogs = async () => {
  loading.value = true
  try {
    const response = await apiGet('/api/admin/audit/email-codes', {
      token: authStore.adminToken,
      params: filters
    })
    logs.value = response.data?.list || []
    total.value = response.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载邮件日志失败')
  } finally {
    loading.value = false
  }
}

const search = () => { filters.page = 1; loadLogs() }
const formatDate = value => value ? new Date(value).toLocaleString() : '-'
const typeLabel = action => action?.includes('_RESET_') ? '找回密码' : '注册'
const statusLabel = action => action?.includes('_SENT') ? '已发送' : action?.includes('_FAILED') ? '发送失败' : '已拦截'
const statusClass = action => action?.includes('_SENT') ? 'success' : action?.includes('_FAILED') ? 'danger' : 'warning'

onMounted(loadLogs)
</script>

<style scoped>
.filters { display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; }
.filters .input { max-width: 260px; }
.detail { max-width: 360px; word-break: break-word; }
.badge.danger { color: #b91c1c; background: #fef2f2; }
.badge.warning { color: #92400e; background: #fffbeb; }
</style>
