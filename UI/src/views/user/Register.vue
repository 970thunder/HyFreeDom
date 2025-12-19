<template>
	<div class="container" style="display:grid;place-items:center;min-height:100vh;">
		<!-- SEO组件 -->
		<SEOHead pageName="userRegister" />
		<!-- 注册表单 -->
		<form class="form" @submit.prevent="onSubmit" v-if="!showEmailVerification">
			<h2 class="form-title">注册账号</h2>

			<div class="flex-column">
				<label>用户名</label>
			</div>
			<div class="inputForm">
				<svg xmlns="http://www.w3.org/2000/svg" width="20" viewBox="0 0 24 24" height="20">
					<path
						d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
						fill="currentColor" />
				</svg>
				<input v-model="formData.username" placeholder="输入用户名" class="input" type="text" required>
			</div>

			<div class="flex-column">
				<label>邮箱
					<span class="email-help" @click="openEmailWhitelistDialog" title="点击查看支持的邮箱">
						<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
							<path
								d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 17h-2v-2h2v2zm2.07-7.75l-.9.92C13.45 12.9 13 13.5 13 15h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H8c0-2.21 1.79-4 4-4s4 1.79 4 4c0 .88-.36 1.68-.93 2.25z" />
						</svg>
					</span>
				</label>
			</div>
			<div class="inputForm">
				<svg xmlns="http://www.w3.org/2000/svg" width="20" viewBox="0 0 24 24" height="20">
					<path
						d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"
						fill="currentColor" />
				</svg>
				<input v-model="formData.email" placeholder="name@example.com" class="input" type="email" required
					@blur="validateEmail" @input="clearEmailError">
				<div v-if="emailError" class="error-message">{{ emailError }}</div>
			</div>

			<div class="flex-column">
				<label>密码</label>
			</div>
			<div class="inputForm">
				<svg xmlns="http://www.w3.org/2000/svg" width="20" viewBox="0 0 24 24" height="20">
					<path
						d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"
						fill="currentColor" />
				</svg>
				<input v-model="formData.password" placeholder="设置密码" class="input" type="password" required>
			</div>

			<div class="flex-column">
				<label>确认密码</label>
			</div>
			<div class="inputForm">
				<svg xmlns="http://www.w3.org/2000/svg" width="20" viewBox="0 0 24 24" height="20">
					<path
						d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"
						fill="currentColor" />
				</svg>
				<input v-model="formData.confirmPassword" placeholder="再次输入密码" class="input" type="password" required>
			</div>

			<div class="flex-column">
				<label>邀请码 <span class="optional">(可选)</span></label>
			</div>
			<div class="inputForm">
				<svg xmlns="http://www.w3.org/2000/svg" width="20" viewBox="0 0 24 24" height="20">
					<path
						d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"
						fill="currentColor" />
				</svg>
				<input v-model="formData.inviteCode" placeholder="输入邀请码享积分奖励" class="input" type="text">
			</div>

			<div class="error-message" v-if="errorMessage">
				{{ errorMessage }}
			</div>

			<button class="button-submit" type="submit" :disabled="isLoading">
				{{ isLoading ? '处理中...' : '发送验证码' }}
			</button>

			<div class="flex-row">
				<span class="span" @click="goLogin">已有账号？去登录</span>
			</div>
		</form>

		<!-- 邮箱验证码验证 -->
		<div class="verification-modal" v-if="showEmailVerification">
			<div class="verification-card">
				<div class="verification-header">
					<h3>邮箱验证</h3>
					<p class="verification-subtitle">验证码正在发送中，请稍后查收邮箱</p>
				</div>

				<div class="email-display">
					<svg xmlns="http://www.w3.org/2000/svg" width="20" viewBox="0 0 24 24" height="20">
						<path
							d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"
							fill="currentColor" />
					</svg>
					<span>{{ formData.email }}</span>
				</div>

				<div class="verification-input">
					<label>请输入6位验证码</label>
					<div class="code-input-container">
						<input v-for="(digit, index) in verificationCode" :key="index" v-model="verificationCode[index]"
							@input="handleCodeInput(index, $event)" @keydown="handleCodeKeydown(index, $event)"
							@paste="handlePaste" class="code-digit" type="text" maxlength="1" ref="codeInputs" />
					</div>
				</div>

				<div class="resend-section">
					<button type="button" class="resend-btn" @click="resendCode" :disabled="resendCountdown > 0">
						{{ resendCountdown > 0 ? `重新发送 (${resendCountdown}s)` : '重新发送验证码' }}
					</button>
				</div>

				<div class="error-message" v-if="verificationError">
					{{ verificationError }}
				</div>

				<div class="verification-actions">
					<button class="button-submit" @click="verifyAndRegister" :disabled="!isCodeComplete || isVerifying">
						{{ isVerifying ? '验证中...' : '完成注册' }}
					</button>
					<button class="button-outline" @click="goBack" :disabled="isVerifying">
						返回修改
					</button>
				</div>
			</div>
		</div>

		<!-- 邮箱白名单说明弹窗 -->
		<div v-if="showEmailWhitelistDialog" class="modal-overlay" @click="showEmailWhitelistDialog = false">
			<div class="modal-content" @click.stop>
				<div class="modal-header">
					<h3>支持的邮箱服务</h3>
					<button class="modal-close" @click="showEmailWhitelistDialog = false">×</button>
				</div>
				<div class="modal-body">
					<p class="modal-description">
						为了确保账号安全和防止滥用，我们只支持以下主流邮箱服务：
					</p>

					<div class="email-categories">
						<div class="email-category">
							<h4>国际邮箱服务</h4>
							<div class="email-list">
								<span v-for="domain in getEmailDomains().slice(0, 4)" :key="domain"
									class="email-domain">
									@{{ domain }}
								</span>
							</div>
						</div>

						<div class="email-category">
							<h4>国内邮箱服务</h4>
							<div class="email-list">
								<span v-for="domain in getEmailDomains().slice(4)" :key="domain" class="email-domain">
									@{{ domain }}
								</span>
							</div>
						</div>

						<div class="email-category">
							<h4>教育邮箱</h4>
							<div class="email-list">
								<span v-for="suffix in getEduSuffixes()" :key="suffix" class="email-domain">
									*{{ suffix }}
								</span>
							</div>
						</div>
					</div>

					<div class="modal-footer">
						<button class="btn primary" @click="showEmailWhitelistDialog = false">我知道了</button>
					</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import SEOHead from '@/components/SEOHead.vue'

const router = useRouter()
const authStore = useAuthStore()

// 表单数据
const formData = ref({
	username: '',
	email: '',
	password: '',
	confirmPassword: '',
	inviteCode: ''
})

// 状态管理
const isLoading = ref(false)
const showEmailVerification = ref(false)
const errorMessage = ref('')
const verificationError = ref('')
const isVerifying = ref(false)

// 邮箱验证相关
const emailError = ref('')
const showEmailWhitelistDialog = ref(false)
const emailWhitelist = ref(null)

// 静态邮箱白名单数据（作为备用）
const staticEmailWhitelist = {
	allowedDomains: [
		'gmail.com',
		'googlemail.com',
		'outlook.com',
		'icloud.com',
		'qq.com',
		'vip.qq.com',
		'163.com',
		'vip.163.com',
		'sina.com',
		'sina.cn',
		'139.com',
		'189.cn'
	],
	allowedEduSuffixes: [
		'.edu.cn',
		'.edu'
	]
}

// 验证码相关
const verificationCode = ref(['', '', '', '', '', ''])
const resendCountdown = ref(0)
const codeInputs = ref([])

// 计算属性
const isCodeComplete = computed(() => {
	return verificationCode.value.every(digit => digit !== '')
})

// 表单验证
const validateForm = () => {
	errorMessage.value = ''

	if (!formData.value.username.trim()) {
		errorMessage.value = '请输入用户名'
		return false
	}

	if (!formData.value.email.trim()) {
		errorMessage.value = '请输入邮箱'
		return false
	}

	// 邮箱格式验证
	const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
	if (!emailRegex.test(formData.value.email)) {
		errorMessage.value = '请输入有效的邮箱地址'
		return false
	}

	// 检查邮箱错误
	if (emailError.value) {
		errorMessage.value = emailError.value
		return false
	}

	if (!formData.value.password) {
		errorMessage.value = '请输入密码'
		return false
	}

	if (formData.value.password.length < 6) {
		errorMessage.value = '密码长度至少6位'
		return false
	}

	if (formData.value.password !== formData.value.confirmPassword) {
		errorMessage.value = '两次输入的密码不一致'
		return false
	}

	return true
}

// 发送验证码
const sendVerificationCode = async () => {
	try {
		const result = await authStore.sendRegisterCode(formData.value.email)

		if (result.success) {
			// 立即进入验证码输入界面，不等待邮件发送完成
			showEmailVerification.value = true
			startResendCountdown()
		} else {
			errorMessage.value = result.message || '发送验证码失败'
		}
	} catch (error) {
		// 即使发送失败，也进入验证码输入界面，让用户可以重试
		showEmailVerification.value = true
		startResendCountdown()
		verificationError.value = '验证码发送中，请稍后重试或检查邮箱'
	}
}

// 开始重发倒计时
const startResendCountdown = () => {
	resendCountdown.value = 60
	const timer = setInterval(() => {
		resendCountdown.value--
		if (resendCountdown.value <= 0) {
			clearInterval(timer)
		}
	}, 1000)
}

// 重发验证码
const resendCode = async () => {
	if (resendCountdown.value > 0) return

	verificationError.value = ''
	try {
		await sendVerificationCode()
	} catch (error) {
		verificationError.value = '重发失败，请稍后重试'
	}
}

// 验证码输入处理
const handleCodeInput = (index, event) => {
	const value = event.target.value.replace(/\D/g, '') // 只允许数字
	verificationCode.value[index] = value

	if (value && index < 5) {
		// 自动跳转到下一个输入框
		nextTick(() => {
			codeInputs.value[index + 1]?.focus()
		})
	}
}

// 验证码键盘事件处理
const handleCodeKeydown = (index, event) => {
	if (event.key === 'Backspace' && !verificationCode.value[index] && index > 0) {
		// 退格时跳转到上一个输入框
		nextTick(() => {
			codeInputs.value[index - 1]?.focus()
		})
	}
}

// 粘贴处理
const handlePaste = (event) => {
	event.preventDefault()
	const pastedData = event.clipboardData.getData('text').replace(/\D/g, '')

	if (pastedData.length === 6) {
		verificationCode.value = pastedData.split('')
		nextTick(() => {
			codeInputs.value[5]?.focus()
		})
	}
}

// 完成注册
const verifyAndRegister = async () => {
	if (!isCodeComplete.value) {
		verificationError.value = '请输入完整的验证码'
		return
	}

	isVerifying.value = true
	verificationError.value = ''

	try {
		const result = await authStore.registerUser({
			username: formData.value.username,
			email: formData.value.email,
			password: formData.value.password,
			emailCode: verificationCode.value.join(''),
			inviteCode: formData.value.inviteCode || undefined
		})

		if (result.success) {
			// 注册成功，跳转到登录页面
			router.push('/user/login?message=注册成功，请登录')
		} else {
			verificationError.value = result.message || '注册失败'
		}
	} catch (error) {
		verificationError.value = '网络错误，请稍后重试'
	} finally {
		isVerifying.value = false
	}
}

// 邮箱验证方法
const validateEmail = async () => {
	if (!formData.value.email) {
		emailError.value = ''
		return
	}

	// 获取邮箱白名单数据
	await loadEmailWhitelist()

	// 简单的客户端验证
	const email = formData.value.email.toLowerCase()
	const allowedDomains = getEmailDomains()
	const allowedEduSuffixes = getEduSuffixes()

	const domain = email.split('@')[1]
	if (!domain) {
		emailError.value = '邮箱格式不正确'
		return
	}

	// 检查是否在允许的域名列表中
	const isAllowed = allowedDomains.includes(domain) ||
		allowedEduSuffixes.some(suffix => domain.endsWith(suffix))

	if (!isAllowed) {
		emailError.value = '该邮箱域名不在允许列表中，请使用主流邮箱服务'
	}
}

// 加载邮箱白名单数据
const loadEmailWhitelist = async () => {
	if (emailWhitelist.value) return // 已经加载过了

	try {
		const response = await fetch('/api/auth/email-whitelist')
		if (response.ok) {
			const data = await response.json()
			emailWhitelist.value = data.data || data
		}
	} catch (error) {
		console.error('获取邮箱白名单失败:', error)
		// 使用静态数据作为备用
		emailWhitelist.value = staticEmailWhitelist
	}
}

const clearEmailError = () => {
	emailError.value = ''
}

// 获取邮箱域名列表
const getEmailDomains = () => {
	return emailWhitelist.value?.allowedDomains || staticEmailWhitelist.allowedDomains
}

// 获取教育邮箱后缀
const getEduSuffixes = () => {
	return emailWhitelist.value?.allowedEduSuffixes || staticEmailWhitelist.allowedEduSuffixes
}

// 打开邮箱白名单弹窗
const openEmailWhitelistDialog = async () => {
	await loadEmailWhitelist()
	showEmailWhitelistDialog.value = true
}

// 表单提交
const onSubmit = async () => {
	if (!validateForm()) return

	isLoading.value = true
	errorMessage.value = ''

	try {
		// 立即进入验证码输入界面，异步发送邮件
		showEmailVerification.value = true
		startResendCountdown()

		// 异步发送验证码，不等待结果
		sendVerificationCode().catch(error => {
			console.error('发送验证码失败:', error)
			verificationError.value = '验证码发送中，请稍后重试或检查邮箱'
		})
	} catch (error) {
		errorMessage.value = '发送验证码失败，请稍后重试'
	} finally {
		isLoading.value = false
	}
}

// 返回修改
const goBack = () => {
	showEmailVerification.value = false
	verificationCode.value = ['', '', '', '', '', '']
	verificationError.value = ''
}

// 跳转到登录页面
const goLogin = () => {
	router.push('/user/login')
}
</script>

<style scoped>
/* 基础表单样式 - 继承登录页面的样式 */
.form {
	display: flex;
	flex-direction: column;
	gap: 10px;
	background-color: #ffffff;
	padding: 30px;
	width: 450px;
	border-radius: 20px;
	box-shadow: 0 12px 28px rgba(15, 23, 42, 0.12), 0 2px 6px rgba(15, 23, 42, 0.06);
	font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
}

.form-title {
	margin: 0 0 20px 0;
	font-size: 24px;
	font-weight: 700;
	color: #151717;
	text-align: center;
}

.flex-column {
	display: flex;
	flex-direction: column;
	gap: 5px;
}

.flex-column>label {
	color: #151717;
	font-weight: 600;
	font-size: 14px;
}

.optional {
	color: #64748b;
	font-weight: 400;
	font-size: 12px;
}

.inputForm {
	border: 1.5px solid #ecedec;
	border-radius: 10px;
	height: 50px;
	display: flex;
	align-items: center;
	padding-left: 10px;
	transition: 0.2s ease-in-out;
	background-color: white;
	gap: 10px;
}

.inputForm:focus-within {
	border: 1.5px solid #2d79f3;
}

.input {
	border-radius: 10px;
	border: none;
	flex: 1;
	height: 100%;
	font-size: 14px;
	background-color: transparent;
	padding-right: 10px;
	color: #151717;
}

.input:focus {
	outline: none;
}

/* 修复浏览器自动填充背景色问题 */
.input:-webkit-autofill,
.input:-webkit-autofill:hover,
.input:-webkit-autofill:focus,
.input:-webkit-autofill:active {
	-webkit-box-shadow: 0 0 0 30px white inset !important;
	-webkit-text-fill-color: #151717 !important;
	transition: background-color 5000s ease-in-out 0s;
}

.flex-row {
	display: flex;
	flex-direction: row;
	align-items: center;
	gap: 10px;
	justify-content: space-between;
}

.span {
	font-size: 14px;
	color: #2d79f3;
	font-weight: 500;
	cursor: pointer;
	transition: color 0.2s;
}

.span:hover {
	color: #1d4ed8;
}

.button-submit {
	margin: 20px 0 10px 0;
	background-color: #151717;
	border: none;
	color: white;
	font-size: 15px;
	font-weight: 500;
	border-radius: 10px;
	height: 50px;
	width: 100%;
	cursor: pointer;
	transition: background-color 0.2s;
}

.button-submit:hover:not(:disabled) {
	background-color: #252727;
}

.button-submit:disabled {
	background-color: #9ca3af;
	cursor: not-allowed;
}

.error-message {
	color: #ef4444;
	font-size: 14px;
	text-align: center;
	margin: 10px 0;
	padding: 8px 12px;
	background-color: #fef2f2;
	border: 1px solid #fecaca;
	border-radius: 8px;
}

/* 邮箱验证模态框样式 */
.verification-modal {
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
	padding: 20px;
}

.verification-card {
	background: #ffffff;
	border-radius: 20px;
	padding: 40px;
	width: 100%;
	max-width: 400px;
	box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
	text-align: center;
}

.verification-header h3 {
	margin: 0 0 8px 0;
	font-size: 24px;
	font-weight: 700;
	color: #151717;
}

.verification-subtitle {
	margin: 0 0 30px 0;
	color: #64748b;
	font-size: 14px;
}

.email-display {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 8px;
	margin-bottom: 30px;
	padding: 12px 16px;
	background-color: #f8fafc;
	border: 1px solid #e2e8f0;
	border-radius: 10px;
	color: #475569;
	font-size: 14px;
	font-weight: 500;
}

.verification-input {
	margin-bottom: 20px;
}

.verification-input label {
	display: block;
	margin-bottom: 15px;
	color: #151717;
	font-weight: 600;
	font-size: 14px;
}

.code-input-container {
	display: flex;
	gap: 8px;
	justify-content: center;
}

.code-digit {
	width: 45px;
	height: 50px;
	border: 1.5px solid #ecedec;
	border-radius: 10px;
	text-align: center;
	font-size: 18px;
	font-weight: 600;
	transition: border-color 0.2s;
}

.code-digit:focus {
	outline: none;
	border-color: #2d79f3;
}

.resend-section {
	margin-bottom: 20px;
}

.resend-btn {
	background: none;
	border: none;
	color: #2d79f3;
	font-size: 14px;
	cursor: pointer;
	text-decoration: underline;
	transition: color 0.2s;
}

.resend-btn:hover:not(:disabled) {
	color: #1d4ed8;
}

.resend-btn:disabled {
	color: #9ca3af;
	cursor: not-allowed;
	text-decoration: none;
}

.verification-actions {
	display: flex;
	flex-direction: column;
	gap: 12px;
}

.button-outline {
	background: #ffffff;
	border: 1.5px solid #ecedec;
	color: #151717;
	font-size: 15px;
	font-weight: 500;
	border-radius: 10px;
	height: 50px;
	width: 100%;
	cursor: pointer;
	transition: all 0.2s;
}

.button-outline:hover:not(:disabled) {
	border-color: #2d79f3;
	color: #2d79f3;
}

.button-outline:disabled {
	background-color: #f3f4f6;
	color: #9ca3af;
	cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 520px) {
	.form {
		width: 100%;
		border-radius: 0;
		padding: 20px;
	}

	.verification-card {
		padding: 30px 20px;
		margin: 0 10px;
	}

	.code-digit {
		width: 40px;
		height: 45px;
		font-size: 16px;
	}

	.code-input-container {
		gap: 6px;
	}
}

@media (max-width: 400px) {
	.code-digit {
		width: 35px;
		height: 40px;
		font-size: 14px;
	}

	.code-input-container {
		gap: 4px;
	}
}

/* 邮箱帮助图标样式 */
.email-help {
	cursor: pointer;
	color: #6b7280;
	margin-left: 8px;
	display: inline-flex;
	align-items: center;
	transition: color 0.2s;
}

.email-help:hover {
	color: #3b82f6;
}

/* 邮箱白名单弹窗样式 */
.modal-overlay {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background-color: rgba(0, 0, 0, 0.5);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 1000;
}

.modal-content {
	background: white;
	border-radius: 12px;
	width: 90%;
	max-width: 500px;
	max-height: 80vh;
	overflow-y: auto;
	box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.modal-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 20px 24px;
	border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
	margin: 0;
	font-size: 18px;
	font-weight: 600;
	color: #111827;
}

.modal-close {
	background: none;
	border: none;
	font-size: 24px;
	color: #6b7280;
	cursor: pointer;
	padding: 0;
	width: 32px;
	height: 32px;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 6px;
	transition: background-color 0.2s;
}

.modal-close:hover {
	background-color: #f3f4f6;
}

.modal-body {
	padding: 24px;
}

.modal-description {
	margin: 0 0 20px 0;
	color: #6b7280;
	line-height: 1.5;
}

.email-categories {
	display: flex;
	flex-direction: column;
	gap: 20px;
}

.email-category h4 {
	margin: 0 0 12px 0;
	font-size: 16px;
	font-weight: 600;
	color: #374151;
}

.email-list {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
}

.email-domain {
	background-color: #f3f4f6;
	color: #374151;
	padding: 6px 12px;
	border-radius: 6px;
	font-size: 14px;
	font-weight: 500;
	border: 1px solid #e5e7eb;
}

.modal-footer {
	margin-top: 24px;
	display: flex;
	justify-content: flex-end;
}

.modal-footer .btn {
	padding: 10px 20px;
	border-radius: 8px;
	font-weight: 500;
	cursor: pointer;
	transition: all 0.2s;
}

.modal-footer .btn.primary {
	background-color: #3b82f6;
	color: white;
	border: none;
}

.modal-footer .btn.primary:hover {
	background-color: #2563eb;
}

/* 响应式设计 */
@media (max-width: 640px) {
	.modal-content {
		width: 95%;
		margin: 20px;
	}

	.modal-header,
	.modal-body {
		padding: 16px;
	}

	.email-list {
		gap: 6px;
	}

	.email-domain {
		font-size: 13px;
		padding: 4px 8px;
	}
}
</style>
