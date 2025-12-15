<template>
    <div class="profile-container" v-loading="isLoading">
        <div class="card">
            <div class="card-header">
                <h3>用户信息</h3>
                <button class="btn outline" @click="refreshUserInfo">刷新</button>
            </div>

            <div v-if="userInfo.username" class="profile-content">
                <div class="info-grid">
                    <div class="info-item">
                        <span class="label">用户名：</span>
                        <span class="value">{{ userInfo.username }}</span>
                    </div>
                    <div class="info-item">
                        <span class="label">邮箱：</span>
                        <span class="value">{{ userInfo.email }}</span>
                    </div>
                    <div class="info-item">
                        <span class="label">创建时间：</span>
                        <span class="value">{{ formatTime(userInfo.createdAt) }}</span>
                    </div>
                    <div class="info-item">
                        <span class="label">状态：</span>
                        <span class="badge" :class="getStatusClass(userInfo.status)">{{ getStatusText(userInfo.status)
                        }}</span>
                    </div>
                    <div class="info-item">
                        <span class="label">实名认证：</span>
                        <div class="verification-wrapper" style="display: flex; align-items: center; gap: 8px;">
                            <span class="badge" :class="verificationInfo.isVerified ? 'success' : 'warning'">
                                {{ verificationInfo.isVerified ? '已认证' : '未认证' }}
                            </span>
                            <el-button v-if="!verificationInfo.isVerified" type="primary" link size="small"
                                @click="showVerificationDialog">
                                立即认证
                            </el-button>
                        </div>
                    </div>
                    <div v-if="verificationInfo.isVerified" class="info-item">
                        <span class="label">真实姓名：</span>
                        <span class="value">{{ verificationInfo.realName }}</span>
                    </div>
                    <div v-if="verificationInfo.isVerified" class="info-item">
                        <span class="label">身份证号：</span>
                        <span class="value">{{ verificationInfo.idCard }}</span>
                    </div>
                </div>
            </div>

            <div v-else class="empty-state">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor" style="color: #9ca3af;">
                    <path
                        d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
                </svg>
                <p>暂无用户信息</p>
            </div>
        </div>

        <div class="card" style="margin-top: 16px;">
            <div class="card-header">
                <h3>账户管理</h3>
            </div>

            <div class="action-buttons">
                <button class="btn primary" @click="showResetPasswordDialog">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path
                            d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4" />
                    </svg>
                    重置密码
                </button>
                <button class="btn danger" @click="showDeleteAccountDialog">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path
                            d="M3 6h18M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2M10 11v6M14 11v6" />
                    </svg>
                    注销账号
                </button>
            </div>
        </div>
    </div>

    <!-- 重置密码对话框 -->
    <el-dialog :model-value="resetPasswordDialogVisible" @update:model-value="resetPasswordDialogVisible = $event"
        title="重置密码" :width="dialogWidth" :close-on-click-modal="false" class="reset-password-dialog">
        <el-form :model="resetForm" :rules="resetRules" ref="resetFormRef" :label-width="labelWidth" class="reset-form">
            <el-form-item label="邮箱" prop="email">
                <el-input v-model="resetForm.email" disabled />
            </el-form-item>
            <el-form-item label="验证码" prop="code">
                <div class="code-input-group">
                    <el-input v-model="resetForm.code" placeholder="请输入6位验证码" maxlength="6" />
                    <el-button @click="sendResetCode" :disabled="isSendingCode || resetCountdown > 0"
                        :loading="isSendingCode" class="code-button">
                        {{ isSendingCode ? '发送中...' : (resetCountdown > 0 ? `${resetCountdown}s 后可重发` : '发送验证码') }}
                    </el-button>
                </div>
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="resetForm.newPassword" type="password" placeholder="请输入新密码" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="resetForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
            </el-form-item>
        </el-form>
        <template #footer>
            <div class="dialog-footer">
                <el-button @click="resetPasswordDialogVisible = false" class="cancel-btn">取消</el-button>
                <el-button type="primary" @click="handleResetPassword" :loading="isResetting"
                    class="confirm-btn">确定</el-button>
            </div>
        </template>
    </el-dialog>

    <!-- 注销账号对话框 -->
    <el-dialog :model-value="deleteAccountDialogVisible" @update:model-value="deleteAccountDialogVisible = $event"
        title="注销账号" :width="dialogWidth" :close-on-click-modal="false" class="delete-account-dialog">
        <div v-if="!deletionEligibility" class="eligibility-check">
            <div class="check-loading" v-if="isCheckingEligibility">
                <el-icon class="is-loading">
                    <Loading />
                </el-icon>
                <span>检查注销条件...</span>
            </div>
            <div v-else class="check-result">
                <div class="warning-box">
                    <el-icon class="warning-icon">
                        <Warning />
                    </el-icon>
                    <div class="warning-content">
                        <h4>无法注销账号</h4>
                        <p>{{ eligibilityMessage }}</p>
                        <div v-if="userDomains && userDomains.length > 0" class="domains-list">
                            <h5>需要先释放以下域名：</h5>
                            <ul>
                                <li v-for="domain in userDomains" :key="domain.id">
                                    {{ domain.fullDomain }} ({{ domain.type }})
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div v-else class="deletion-confirmation">
            <div class="warning-box">
                <el-icon class="warning-icon">
                    <Warning />
                </el-icon>
                <div class="warning-content">
                    <h4>⚠️ 账号注销确认</h4>
                    <p>您即将注销账号 <strong>{{ userInfo.username }}</strong>，此操作<strong>不可撤销</strong>！</p>

                    <div class="consequences">
                        <h5>注销后将发生以下情况：</h5>
                        <ul>
                            <li>❌ 账号将无法登录</li>
                            <li>❌ 所有积分将被清零（当前积分：<strong>{{ userPoints }}</strong>）</li>
                            <li>❌ 无法申请新的域名</li>
                            <li>❌ 无法使用邀请功能</li>
                            <li>✅ 已申请的域名将保留（但无法管理）</li>
                            <li>✅ 邀请关系数据将保留</li>
                        </ul>
                    </div>
                </div>
            </div>

            <el-form :model="deleteForm" :rules="deleteRules" ref="deleteFormRef" class="delete-form">
                <el-form-item prop="confirmDeletion">
                    <el-checkbox v-model="deleteForm.confirmDeletion">
                        我确认要注销账号，理解此操作不可撤销
                    </el-checkbox>
                </el-form-item>
                <el-form-item prop="confirmPointsLoss">
                    <el-checkbox v-model="deleteForm.confirmPointsLoss">
                        我确认放弃所有积分（{{ userPoints }} 积分）
                    </el-checkbox>
                </el-form-item>
            </el-form>
        </div>

        <template #footer>
            <div class="dialog-footer">
                <el-button @click="deleteAccountDialogVisible = false" class="cancel-btn">取消</el-button>
                <el-button v-if="deletionEligibility" type="danger" @click="handleDeleteAccount" :loading="isDeleting"
                    :disabled="!canDelete" class="delete-btn">
                    确认注销
                </el-button>
            </div>
        </template>
    </el-dialog>

    <!-- 实名认证对话框 -->
    <el-dialog v-model="verificationDialogVisible" title="实名认证" :width="dialogWidth" :close-on-click-modal="false">
        <el-form :model="verificationForm" :rules="verificationRules" ref="verificationFormRef"
            :label-width="labelWidth">
            <el-form-item label="真实姓名" prop="realName">
                <el-input v-model="verificationForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="身份证号" prop="idCard">
                <el-input v-model="verificationForm.idCard" placeholder="请输入身份证号" />
            </el-form-item>
        </el-form>
        <template #footer>
            <div class="dialog-footer">
                <el-button @click="verificationDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleVerification" :loading="isVerifying">提交认证</el-button>
            </div>
        </template>
    </el-dialog>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth.js'
import { apiGet, apiPost } from '@/utils/api.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, Warning } from '@element-plus/icons-vue'

const authStore = useAuthStore()

// 响应式计算属性
const dialogWidth = computed(() => {
    if (window.innerWidth <= 480) return '95%'
    if (window.innerWidth <= 768) return '90%'
    return '400px'
})

const labelWidth = computed(() => {
    if (window.innerWidth <= 480) return '80px'
    if (window.innerWidth <= 768) return '90px'
    return '100px'
})

// 响应式数据
const isLoading = ref(false)
const userInfo = ref({
    username: '',
    email: '',
    createdAt: '',
    status: ''
})

// 实名认证相关
const verificationInfo = ref({
    isVerified: false,
    realName: '',
    idCard: ''
})
const verificationDialogVisible = ref(false)
const isVerifying = ref(false)
const verificationForm = ref({
    realName: '',
    idCard: ''
})
const verificationFormRef = ref(null)
const verificationRules = {
    realName: [
        { required: true, message: '请输入真实姓名', trigger: 'blur' },
        { min: 2, message: '姓名长度至少2位', trigger: 'blur' }
    ],
    idCard: [
        { required: true, message: '请输入身份证号', trigger: 'blur' },
        { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号', trigger: 'blur' }
    ]
}

// 重置密码相关
const resetPasswordDialogVisible = ref(false)
const isSendingCode = ref(false)
const isResetting = ref(false)
const resetCountdown = ref(0)
let resetTimer = null
const resetForm = ref({
    email: '',
    code: '',
    newPassword: '',
    confirmPassword: ''
})

const resetRules = {
    email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
    ],
    code: [
        { required: true, message: '请输入验证码', trigger: 'blur' },
        { len: 6, message: '验证码长度为6位', trigger: 'blur' }
    ],
    newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, message: '密码长度至少6位', trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, message: '请确认新密码', trigger: 'blur' },
        {
            validator: (rule, value, callback) => {
                if (value !== resetForm.value.newPassword) {
                    callback(new Error('两次输入的密码不一致'))
                } else {
                    callback()
                }
            },
            trigger: 'blur'
        }
    ]
}

// 加载用户信息
const loadUserInfo = async () => {
    try {
        if (!authStore.token) {
            console.error('用户token不存在')
            return
        }

        const response = await apiGet('/api/user/info', { token: authStore.token })
        if (response.data) {
            userInfo.value = response.data
            // 设置重置密码表单的邮箱
            resetForm.value.email = response.data.email
            // 调试日志已移除
        }
    } catch (error) {
        console.error('加载用户信息失败:', error)
        ElMessage.error('加载用户信息失败')
    }
}

// 加载实名认证状态
const loadVerificationStatus = async () => {
    try {
        const response = await apiGet('/api/user/verification', { token: authStore.token })
        if (response.data) {
            verificationInfo.value = response.data
        }
    } catch (error) {
        console.error('加载实名认证状态失败:', error)
    }
}

// 刷新用户信息
const refreshUserInfo = () => {
    loadUserInfo()
    loadVerificationStatus()
}

// 显示认证对话框
const showVerificationDialog = () => {
    verificationDialogVisible.value = true
    verificationForm.value.realName = ''
    verificationForm.value.idCard = ''
}

// 提交认证
const handleVerification = async () => {
    if (!verificationFormRef.value) return
    await verificationFormRef.value.validate(async (valid) => {
        if (valid) {
            isVerifying.value = true
            try {
                await apiPost('/api/user/verification', verificationForm.value, { token: authStore.token })
                ElMessage.success('实名认证成功')
                verificationDialogVisible.value = false
                loadVerificationStatus()
                // 刷新页面状态，可能影响导航栏等
                authStore.refreshUserInfo()
            } catch (error) {
                console.error(error)
                ElMessage.error(error.message || '认证失败')
            } finally {
                isVerifying.value = false
            }
        }
    })
}

// 格式化时间
const formatTime = (timeStr) => {
    if (!timeStr) return '未知'
    const date = new Date(timeStr)
    return date.toLocaleString()
}

// 获取状态样式类
const getStatusClass = (status) => {
    switch (status) {
        case 'ACTIVE': return 'success'
        case 'INACTIVE': return 'warning'
        case 'BANNED': return 'danger'
        default: return ''
    }
}

// 获取状态文本
const getStatusText = (status) => {
    switch (status) {
        case 'ACTIVE': return '正常'
        case 'INACTIVE': return '未激活'
        case 'BANNED': return '已封禁'
        default: return '未知'
    }
}

// 显示重置密码对话框
const showResetPasswordDialog = () => {
    resetPasswordDialogVisible.value = true
    // 重置表单
    resetForm.value = {
        email: userInfo.value.email,
        code: '',
        newPassword: '',
        confirmPassword: ''
    }
}

// 注销账号相关
const deleteAccountDialogVisible = ref(false)
const isCheckingEligibility = ref(false)
const isDeleting = ref(false)
const deletionEligibility = ref(false)
const eligibilityMessage = ref('')
const userDomains = ref([])
const userPoints = ref(0)
const deleteForm = ref({
    confirmDeletion: false,
    confirmPointsLoss: false
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
    confirmPointsLoss: [
        {
            validator: (rule, value, callback) => {
                if (!value) {
                    callback(new Error('请确认放弃积分'))
                } else {
                    callback()
                }
            },
            trigger: 'change'
        }
    ]
}

const deleteFormRef = ref(null)

// 计算属性
const canDelete = computed(() => {
    return deleteForm.value.confirmDeletion && deleteForm.value.confirmPointsLoss
})

// 显示注销账号对话框
const showDeleteAccountDialog = async () => {
    deleteAccountDialogVisible.value = true
    await checkDeletionEligibility()
}

// 检查注销资格
const checkDeletionEligibility = async () => {
    try {
        isCheckingEligibility.value = true
        const response = await apiGet('/api/user/account/deletion/check', { token: authStore.token })

        if (response.data) {
            deletionEligibility.value = response.data.canDelete
            eligibilityMessage.value = response.data.message
            userDomains.value = response.data.domains || []
            userPoints.value = response.data.points || 0
        }
    } catch (error) {
        console.error('检查注销资格失败:', error)

        // 设置默认的错误状态
        deletionEligibility.value = false
        eligibilityMessage.value = error.message || '检查注销资格失败'
        userDomains.value = []
        userPoints.value = 0

        ElMessage.error(error.message || '检查注销资格失败')
    } finally {
        isCheckingEligibility.value = false
    }
}

// 处理账号注销
const handleDeleteAccount = async () => {
    try {
        await deleteFormRef.value.validate()

        const confirmed = await ElMessageBox.confirm(
            '您确定要注销账号吗？此操作不可撤销！',
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
        const response = await apiPost('/api/user/account/deletion', {
            confirmDeletion: deleteForm.value.confirmDeletion,
            confirmPointsLoss: deleteForm.value.confirmPointsLoss
        }, { token: authStore.token })

        if (response.code === 0) {
            ElMessage.success('账号注销成功')
            deleteAccountDialogVisible.value = false

            // 清除本地状态并跳转到登录页
            authStore.logout()
            window.location.href = '/user/login'
        } else {
            ElMessage.error(response.message || '账号注销失败')
        }
    } catch (error) {
        if (error !== 'cancel') {
            console.error('账号注销失败:', error)
            ElMessage.error(error.message || '账号注销失败')
        }
    } finally {
        isDeleting.value = false
    }
}



// 发送重置密码验证码
const sendResetCode = async () => {
    try {
        if (!resetForm.value.email) {
            ElMessage.error('请先输入邮箱')
            return
        }

        isSendingCode.value = true
        const response = await apiPost('/api/auth/forgot', {
            email: resetForm.value.email
        })

        if (response.code === 0) {
            ElMessage.success('验证码已发送到您的邮箱')
            // 开始60秒倒计时
            resetCountdown.value = 60
            if (resetTimer) clearInterval(resetTimer)
            resetTimer = setInterval(() => {
                resetCountdown.value = resetCountdown.value - 1
                if (resetCountdown.value <= 0) {
                    resetCountdown.value = 0
                    clearInterval(resetTimer)
                    resetTimer = null
                }
            }, 1000)
        } else {
            ElMessage.error(response.message || '发送验证码失败')
        }
    } catch (error) {
        console.error('发送验证码失败:', error)
        ElMessage.error(error.message || '发送验证码失败')
    } finally {
        isSendingCode.value = false
    }
}

// 处理重置密码
const handleResetPassword = async () => {
    try {
        isResetting.value = true
        const response = await apiPost('/api/auth/reset', {
            email: resetForm.value.email,
            code: resetForm.value.code,
            newPassword: resetForm.value.newPassword
        })

        if (response.code === 0) {
            ElMessage.success('密码重置成功，请重新登录')
            resetPasswordDialogVisible.value = false
            // 跳转到登录页
            authStore.logout()
            window.location.href = '/user/login'
        } else {
            ElMessage.error(response.message || '密码重置失败')
        }
    } catch (error) {
        console.error('密码重置失败:', error)
        ElMessage.error(error.message || '密码重置失败')
    } finally {
        isResetting.value = false
    }
}

// 初始化数据
const initData = async () => {
    isLoading.value = true
    try {
        await loadUserInfo()
        await loadVerificationStatus()
    } catch (error) {
        console.error('初始化数据失败:', error)
    } finally {
        isLoading.value = false
    }
}

onMounted(() => {
    initData()
})

// 清理倒计时定时器
onUnmounted(() => {
    if (resetTimer) clearInterval(resetTimer)
})
</script>

<style scoped>
.profile-container {
    padding: 20px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
}

.profile-content {
    margin-bottom: 20px;
}

.info-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 20px;
}

.info-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    background: #f8fafc;
    border-radius: 8px;
    border: 1px solid #e2e8f0;
}

.info-item .label {
    font-size: 14px;
    color: #64748b;
    font-weight: 500;
    min-width: 80px;
}

.info-item .value {
    font-size: 14px;
    color: #151717;
    font-weight: 500;
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

.action-buttons {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
}

.action-buttons .btn {
    display: flex;
    align-items: center;
    gap: 8px;
}

.code-input-group {
    display: flex;
    gap: 8px;
    align-items: center;
}

.code-input-group .el-input {
    flex: 1;
}

.code-button {
    white-space: nowrap;
    min-width: 100px;
}

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
}

.cancel-btn,
.confirm-btn {
    min-width: 80px;
}

/* 重置密码对话框响应式样式 */
.reset-password-dialog .el-dialog {
    margin: 0 auto;
}

.reset-form {
    padding: 0;
}

.reset-form .el-form-item {
    margin-bottom: 20px;
}

.reset-form .el-form-item__label {
    font-weight: 500;
    color: #374151;
}

.reset-form .el-input__wrapper {
    border-radius: 6px;
}

/* 注销账号对话框样式 */
.delete-account-dialog .el-dialog {
    margin: 0 auto;
}

.eligibility-check {
    padding: 20px 0;
}

.check-loading {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 40px 0;
    color: #6b7280;
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

.domains-list {
    margin-top: 12px;
}

.domains-list ul {
    margin: 8px 0 0 0;
    padding-left: 20px;
}

.domains-list li {
    margin: 4px 0;
    color: #92400e;
    font-family: monospace;
    background: rgba(245, 158, 11, 0.1);
    padding: 2px 6px;
    border-radius: 4px;
    display: inline-block;
    margin-right: 8px;
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

.reset-form .el-button {
    border-radius: 6px;
    font-weight: 500;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .profile-container {
        padding: 12px;
    }

    .info-grid {
        grid-template-columns: 1fr;
        gap: 12px;
    }

    .info-item {
        flex-direction: column;
        align-items: flex-start;
        gap: 8px;
    }

    .info-item .label {
        min-width: auto;
    }

    .action-buttons {
        flex-direction: column;
    }

    .action-buttons .btn {
        width: 100%;
        justify-content: center;
    }

    .code-input-group {
        flex-direction: column;
        gap: 12px;
    }

    .code-button {
        width: 100%;
    }

    .dialog-footer {
        flex-direction: column;
        gap: 8px;
    }

    .cancel-btn,
    .confirm-btn {
        width: 100%;
    }
}

@media (max-width: 480px) {
    .profile-container {
        padding: 8px;
    }

    .card-header {
        flex-direction: column;
        gap: 12px;
        align-items: flex-start;
    }

    .card-header h3 {
        margin: 0;
    }

    .info-item {
        padding: 12px;
    }

    .info-item .label {
        font-size: 13px;
    }

    .info-item .value {
        font-size: 13px;
    }

    .reset-form .el-form-item {
        margin-bottom: 16px;
    }

    .reset-form .el-form-item__label {
        font-size: 14px;
    }

    .reset-form .el-input {
        font-size: 14px;
    }

    .reset-form .el-button {
        font-size: 14px;
        padding: 8px 16px;
    }
}
</style>
