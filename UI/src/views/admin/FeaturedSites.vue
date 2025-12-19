<template>
  <div class="featured-sites">
    <div class="page-header">
      <h2>推荐网站管理</h2>
      <el-button type="primary" @click="showAddDialog">
        <el-icon class="el-icon--left">
          <Plus />
        </el-icon>
        新增网站
      </el-button>
    </div>

    <el-card class="box-card">
      <el-table :data="sites" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="Logo" width="100">
          <template #default="scope">
            <el-image v-if="scope.row.logoUrl" style="width: 40px; height: 40px; border-radius: 4px"
              :src="scope.row.logoUrl" fit="cover" :preview-src-list="[scope.row.logoUrl]" preview-teleported />
            <span v-else>无</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="网站名称" min-width="150" />
        <el-table-column prop="description" label="网站描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="url" label="网站链接" min-width="200">
          <template #default="scope">
            <a :href="scope.row.url" target="_blank" class="link">{{ scope.row.url }}</a>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="editSite(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteSite(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑网站' : '新增网站'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="网站名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入网站名称" />
        </el-form-item>
        <el-form-item label="网站描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入网站描述" />
        </el-form-item>
        <el-form-item label="网站链接" prop="url">
          <el-input v-model="form.url" placeholder="请输入网站链接 (包含http/https)" />
        </el-form-item>
        <el-form-item label="Logo链接" prop="logoUrl">
          <el-input v-model="form.logoUrl" placeholder="请输入Logo图片链接" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitting">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiGet, apiPost, apiPut, apiDelete } from '@/utils/api.js'
import { useAuthStore } from '@/stores/auth.js'

const authStore = useAuthStore()
const loading = ref(false)
const sites = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  url: '',
  logoUrl: ''
})

const rules = {
  name: [{ required: true, message: '请输入网站名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入网站链接', trigger: 'blur' }]
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

const fetchSites = async () => {
  loading.value = true
  try {
    const result = await apiGet('/api/admin/featured-sites', {
      token: authStore.adminToken
    })
    // 兼容空数组情况
    if (result.code === 0 || (result.data && Array.isArray(result.data))) {
      sites.value = result.data || []
    } else {
      ElMessage.error(result.message || '获取列表失败')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  isEdit.value = false
  form.id = null
  form.name = ''
  form.description = ''
  form.url = ''
  form.logoUrl = ''
  dialogVisible.value = true
}

const editSite = (row) => {
  isEdit.value = true
  form.id = row.id
  form.name = row.name
  form.description = row.description
  form.url = row.url
  form.logoUrl = row.logoUrl
  dialogVisible.value = true
}

const deleteSite = (row) => {
  ElMessageBox.confirm(
    `确定要删除网站 "${row.name}" 吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      const result = await apiDelete(`/api/admin/featured-sites/${row.id}`, {
        token: authStore.adminToken
      })
      if (result.code === 0) {
        ElMessage.success('删除成功')
        fetchSites()
      } else {
        ElMessage.error(result.message || '删除失败')
      }
    } catch (error) {
      console.error(error)
      ElMessage.error('删除失败')
    }
  })
}

const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        let result
        const options = { token: authStore.adminToken }

        if (isEdit.value) {
          result = await apiPut(`/api/admin/featured-sites/${form.id}`, form, options)
        } else {
          result = await apiPost('/api/admin/featured-sites', form, options)
        }

        if (result.code === 200) {
          ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
          dialogVisible.value = false
          fetchSites()
        } else {
          ElMessage.error(result.message || '操作失败')
        }
      } catch (error) {
        console.error(error)
        ElMessage.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

onMounted(() => {
  fetchSites()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.link {
  color: #409eff;
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
}
</style>
