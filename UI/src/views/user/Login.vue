<template>
	<div class="container" style="display:grid;place-items:center;min-height:100vh;">
		<!-- SEO组件 -->
		<SEOHead pageName="userLogin" />
		<form class="form" @submit.prevent="onSubmit">
			<div class="flex-column">
				<label>用户名/邮箱</label>
			</div>
			<div class="inputForm">
				<svg xmlns="http://www.w3.org/2000/svg" width="20" viewBox="0 0 32 32" height="20">
					<g data-name="Layer 3" id="Layer_3">
						<path
							d="m30.853 13.87a15 15 0 0 0 -29.729 4.082 15.1 15.1 0 0 0 12.876 12.918 15.6 15.6 0 0 0 2.016.13 14.85 14.85 0 0 0 7.715-2.145 1 1 0 1 0 -1.031-1.711 13.007 13.007 0 1 1 5.458-6.529 2.149 2.149 0 0 1 -4.158-.759v-10.856a1 1 0 0 0 -2 0v1.726a8 8 0 1 0 .2 10.325 4.135 4.135 0 0 0 7.83.274 15.2 15.2 0 0 0 .823-7.455zm-14.853 8.13a6 6 0 1 1 6-6 6.006 6.006 0 0 1 -6 6z">
						</path>
					</g>
				</svg>
				<input v-model="account" placeholder="输入用户名或邮箱" class="input" type="text">
			</div>

			<div class="flex-column">
				<label>密码</label>
			</div>
			<div class="inputForm">
				<svg xmlns="http://www.w3.org/2000/svg" width="20" viewBox="-64 0 512 512" height="20">
					<path
						d="m336 512h-288c-26.453125 0-48-21.523438-48-48v-224c0-26.476562 21.546875-48 48-48h288c26.453125 0 48 21.523438 48 48v224c0 26.476562-21.546875 48-48 48zm-288-288c-8.8125 0-16 7.167969-16 16v224c0 8.832031 7.1875 16 16 16h288c8.8125 0 16-7.167969 16-16v-224c0-8.832031-7.1875-16-16-16zm0 0">
					</path>
					<path
						d="m304 224c-8.832031 0-16-7.167969-16-16v-80c0-52.929688-43.070312-96-96-96s-96 43.070312-96 96v80c0 8.832031-7.167969 16-16 16s-16-7.167969-16-16v-80c0-70.59375 57.40625-128 128-128s128 57.40625 128 128v80c0 8.832031-7.167969 16-16 16zm0 0">
					</path>
				</svg>
				<input v-model="password" placeholder="输入密码" class="input" type="password">
			</div>

			<div class="flex-row">
				<div class="remember-me">
					<input type="checkbox" v-model="remember" id="remember-me">
					<label for="remember-me">记住我</label>
				</div>
				<div style="display:flex; gap:12px;">
					<span class="span" @click.prevent="goForgot">忘记密码？</span>
					<span class="span" @click.prevent="goRegister">没有账号？去注册</span>
				</div>
			</div>

			<div class="error-message" v-if="errorMessage">
				{{ errorMessage }}
			</div>

			<button class="button-submit" type="submit" :disabled="isLoading">
				{{ isLoading ? '登录中...' : '登录' }}
			</button>

			<div class="agreement-box">
				<div class="agreement-checkbox">
					<input type="checkbox" id="agreement" v-model="isAgreed">
					<label for="agreement">我已阅读并同意
						<router-link to="/legal/user-agreement" target="_blank" class="link">《用户协议》</router-link>和
						<router-link to="/legal/privacy-policy" target="_blank" class="link">《隐私条款》</router-link>
					</label>
				</div>
			</div>

			<!-- 
			<div class="flex-row">
				<button class="btn github" type="button">
					<svg width="20" height="20" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"
						fill="currentColor">
						<path
							d="M12 0C5.37 0 0 5.37 0 12c0 5.3 3.438 9.8 8.205 11.385.6.11.82-.26.82-.58 0-.285-.01-1.04-.015-2.04-3.338.725-4.042-1.61-4.042-1.61-.546-1.385-1.333-1.755-1.333-1.755-1.09-.745.082-.73.082-.73 1.205.085 1.84 1.24 1.84 1.24 1.07 1.835 2.807 1.305 3.492.998.107-.775.418-1.305.762-1.605-2.665-.305-5.466-1.332-5.466-5.93 0-1.31.468-2.38 1.235-3.22-.123-.303-.535-1.525.117-3.176 0 0 1.008-.322 3.3 1.23.96-.267 1.99-.4 3.01-.405 1.02.005 2.05.138 3.01.405 2.29-1.552 3.297-1.23 3.297-1.23.654 1.651.242 2.873.12 3.176.77.84 1.233 1.91 1.233 3.22 0 4.61-2.807 5.624-5.48 5.92.43.37.823 1.096.823 2.21 0 1.595-.014 2.883-.014 3.276 0 .322.217.698.825.58C20.565 21.796 24 17.297 24 12 24 5.37 18.63 0 12 0z" />
					</svg>
					GitHub
				</button>
			</div>
			-->
		</form>

	</div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import { ElMessage } from 'element-plus'
import SEOHead from '@/components/SEOHead.vue'

const router = useRouter()
const authStore = useAuthStore()

const account = ref('')
const password = ref('')
const remember = ref(false)
const isLoading = ref(false)
const errorMessage = ref('')
const isAgreed = ref(false)

const onSubmit = async () => {
	if (!isAgreed.value) {
		ElMessage.warning('请阅读并同意用户协议和隐私条款')
		return
	}

	if (!account.value.trim() || !password.value.trim()) {
		errorMessage.value = '请输入用户名和密码'
		return
	}

	isLoading.value = true
	errorMessage.value = ''

	try {
		const result = await authStore.loginUser({
			username: account.value.trim(),
			password: password.value
		})

		if (result.success) {
			// 设置记住我
			authStore.setRememberMe(remember.value)

			// 如果选择了记住我，保存用户名
			if (remember.value) {
				authStore.setRememberedUsername(account.value.trim())
			} else {
				authStore.clearRememberedUsername()
			}

			// 调试日志已移除

			ElMessage.success('登录成功')

			// 延迟跳转，确保状态完全保存
			setTimeout(() => {
				router.push('/user/dashboard')
			}, 100)
		} else {
			errorMessage.value = result.message || '登录失败'
		}
	} catch (error) {
		errorMessage.value = error.message || '登录失败'
		console.error('登录错误:', error)
	} finally {
		isLoading.value = false
	}
}

const goRegister = () => router.push('/user/register')
const goForgot = () => router.push('/user/forgot')

// 组件挂载时恢复记住的用户名
onMounted(() => {
	const rememberedUsername = authStore.getRememberedUsername()
	if (rememberedUsername) {
		account.value = rememberedUsername
		remember.value = true
	}
})
</script>
<style scoped>
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

::placeholder {
	font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
}

.form button {
	align-self: flex-end;
}

.flex-column>label {
	color: #151717;
	font-weight: 600;
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

.input {
	border-radius: 10px;
	border: none;
	flex: 1;
	height: 100%;
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

.inputForm:focus-within {
	border: 1.5px solid #2d79f3;
}

.flex-row {
	display: flex;
	flex-direction: row;
	align-items: center;
	gap: 10px;
	justify-content: space-between;
}

.flex-row>div>label {
	font-size: 14px;
	color: black;
	font-weight: 400;
}

.span {
	font-size: 14px;
	margin-left: 5px;
	color: #2d79f3;
	font-weight: 500;
	cursor: pointer;
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
}

.button-submit:hover {
	background-color: #252727;
}

.p {
	text-align: center;
	color: black;
	font-size: 14px;
	margin: 5px 0;
}

.btn {
	margin-top: 10px;
	width: 100%;
	height: 50px;
	border-radius: 10px;
	display: flex;
	justify-content: center;
	align-items: center;
	font-weight: 500;
	gap: 10px;
	border: 1px solid #ededef;
	background-color: white;
	cursor: pointer;
	transition: 0.2s ease-in-out;
}

.btn:hover {
	border: 1px solid #2d79f3;
}

.remember-me {
	display: flex;
	align-items: center;
	gap: 8px;
}

.remember-me input[type="checkbox"] {
	width: 16px;
	height: 16px;
	cursor: pointer;
	accent-color: #2d79f3;
}

.remember-me label {
	cursor: pointer;
	user-select: none;
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

.button-submit:disabled {
	background-color: #9ca3af;
	cursor: not-allowed;
}

/* 响应式：窄屏表单宽度适配 */
@media (max-width: 520px) {
	.form {
		width: 100%;
		border-radius: 0;
		padding: 20px;
	}
}

.agreement-box {
	margin-top: 15px;
	width: 100%;
}

.agreement-checkbox {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 8px;
	font-size: 13px;
	color: #64748b;
}

.agreement-checkbox input[type="checkbox"] {
	width: 16px;
	height: 16px;
	cursor: pointer;
	accent-color: #2d79f3;
}

.agreement-checkbox label {
	cursor: pointer;
	user-select: none;
	display: flex;
	align-items: center;
	flex-wrap: wrap;
}

.agreement-checkbox .link {
	color: #2d79f3;
	text-decoration: none;
	margin: 0 2px;
}

.agreement-checkbox .link:hover {
	text-decoration: underline;
}
</style>