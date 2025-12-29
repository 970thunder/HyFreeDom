<template>
	<div class="recharge-container">
		<!-- 积分充值卡片 -->
		<div class="card">
			<h3>积分充值</h3>
			<div class="form">
				<div class="input-row">
					<label class="label">充值金额（¥）</label>
					<input class="input" placeholder="例如：10" disabled>
				</div>
				<div class="input-row">
					<label class="label">预计获得积分</label>
					<input class="input" value="10" disabled>
				</div>
				<div class="row">
					<button class="btn primary disabled" disabled>创建订单</button>
					<button class="btn outline disabled" disabled>查看历史订单</button>
				</div>
				<div class="notice">
					<p>💡 在线充值功能暂时停用，请使用卡密充值</p>
				</div>
			</div>
		</div>

		<!-- 卡密充值卡片 -->
		<div class="card">
			<h3>卡密充值</h3>
			<div class="form">
				<div class="input-row">
					<label class="label">卡密</label>
					<input class="input" v-model="cardCode" placeholder="请输入卡密" :disabled="loading">
				</div>
				<div class="row">
					<button class="btn primary" @click="redeemCard" :disabled="!cardCode.trim() || loading">
						{{ loading ? '兑换中...' : '兑换卡密' }}
					</button>
					<button class="btn outline" @click="clearForm">清空</button>
				</div>
				<div class="notice">
					<p>💡 输入卡密即可获得对应积分，卡密可联系站长获取</p>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiPost } from '@/utils/api.js'
import { useAuthStore } from '@/stores/auth.js'

const authStore = useAuthStore()
const cardCode = ref('')
const loading = ref(false)

// 兑换卡密
const redeemCard = async () => {
	if (!cardCode.value.trim()) {
		ElMessage.warning('请输入卡密')
		return
	}

	loading.value = true
	try {
		const response = await apiPost('/api/user/card/redeem', {
			cardCode: cardCode.value.trim()
		}, {
			token: authStore.token
		})

		if (response.code === 200) {
			ElMessage.success(`兑换成功！获得 ${response.data.points} 积分`)
			cardCode.value = ''
			// 刷新用户积分信息
			await authStore.refreshUserInfo()
		} else {
			ElMessage.error(response.message || '兑换失败')
		}
	} catch (error) {
		console.error('卡密兑换失败:', error)
		ElMessage.error(error.message || '兑换失败，请稍后重试')
	} finally {
		loading.value = false
	}
}

// 清空表单
const clearForm = () => {
	cardCode.value = ''
}
</script>

<style scoped>
.recharge-container {
	display: flex;
	flex-direction: column;
	gap: 20px;
}

.card {
	background: white;
	border-radius: 8px;
	padding: 24px;
	box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.card h3 {
	margin: 0 0 20px 0;
	font-size: 18px;
	font-weight: 600;
	color: #333;
}

.form {
	display: flex;
	flex-direction: column;
	gap: 16px;
}

.input-row {
	display: flex;
	flex-direction: column;
	gap: 8px;
}

.label {
	font-size: 14px;
	font-weight: 500;
	color: #555;
}

.input {
	padding: 12px 16px;
	border: 1px solid #ddd;
	border-radius: 6px;
	font-size: 14px;
	transition: border-color 0.2s;
}

.input:focus {
	outline: none;
	border-color: #409eff;
}

.input:disabled {
	background-color: #f5f5f5;
	color: #999;
	cursor: not-allowed;
}

.row {
	display: flex;
	gap: 12px;
	margin-top: 8px;
}

.btn {
	padding: 12px 24px;
	border: none;
	border-radius: 6px;
	font-size: 14px;
	font-weight: 500;
	cursor: pointer;
	transition: all 0.2s;
}

.btn.primary {
	background-color: #409eff;
	color: white;
}

.btn.primary:hover:not(:disabled) {
	background-color: #337ecc;
}

.btn.primary:disabled {
	background-color: #c0c4cc;
	cursor: not-allowed;
}

.btn.outline {
	background-color: transparent;
	color: #409eff;
	border: 1px solid #409eff;
}

.btn.outline:hover:not(:disabled) {
	background-color: #409eff;
	color: white;
}

.btn.outline:disabled {
	color: #c0c4cc;
	border-color: #c0c4cc;
	cursor: not-allowed;
}

.btn.disabled {
	background-color: #c0c4cc !important;
	color: #fff !important;
	cursor: not-allowed !important;
}

.notice {
	margin-top: 16px;
	padding: 12px;
	background-color: #f0f9ff;
	border: 1px solid #b3d8ff;
	border-radius: 6px;
}

.notice p {
	margin: 0;
	font-size: 13px;
	color: #409eff;
}
</style>
