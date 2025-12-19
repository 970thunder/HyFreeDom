<template>
	<div class="landing-page">
		<!-- 导航栏 -->
		<nav class="navbar" :class="{ 'scrolled': isScrolled }">
			<div class="nav-content">
				<div class="logo">
					<span class="logo-text">HyFreeDom</span>
				</div>
				<div class="nav-links">
					<router-link to="/user/login" class="nav-link">登录</router-link>
					<router-link to="/user/register" class="nav-btn">注册账户</router-link>
				</div>
			</div>
		</nav>

		<!-- 主要内容区 -->
		<main class="hero-section">
			<!-- 背景动画 -->
			<div class="bg-animation" ref="bgRef">
				<div class="grid-overlay"></div>
				<div class="blob blob-1" :style="{ translate: `${mouseX * -0.02}px ${mouseY * -0.02}px` }"></div>
				<div class="blob blob-2" :style="{ translate: `${mouseX * 0.03}px ${mouseY * 0.03}px` }"></div>
				<div class="blob blob-3" :style="{ translate: `${mouseX * -0.04}px ${mouseY * -0.04}px` }"></div>
				<div class="blob blob-4" :style="{ translate: `${mouseX * 0.02}px ${mouseY * 0.05}px` }"></div>
				<div class="stars"></div>
			</div>

			<!-- 交互特效画布 -->
			<canvas ref="canvasRef" class="interaction-canvas"></canvas>

			<div class="content-wrapper">
				<h1 class="hero-title">
					<span class="gradient-text">构建您的</span><br>
					专属网络标识
				</h1>
				<p class="hero-subtitle">
					永久免费的二级域名分发与动态 DNS 解析服务<br>
					简单、快速、稳定
				</p>

				<!-- 搜索框区域 -->
				<div class="search-container" :class="{ 'has-results': searchResults.length > 0 }">
					<div class="input-group">
						<input type="text" v-model="searchPrefix" @keyup.enter="handleSearch"
							placeholder="输入您想要的前缀 (如: myblog)" class="search-input" :disabled="loading" />
						<button class="search-btn" @click="handleSearch" :disabled="loading || !searchPrefix">
							<span v-if="loading" class="loader"></span>
							<span v-else>查询</span>
						</button>
					</div>
					<div class="input-hint">支持字母、数字和连字符，长度 2-63 位</div>
				</div>

				<!-- 搜索结果 -->
				<div class="results-container" v-if="searchResults.length > 0">
					<div class="results-grid">
						<div v-for="item in searchResults" :key="item.domain" class="result-card"
							:class="{ 'available': item.available, 'unavailable': !item.available }">
							<div class="domain-info">
								<span class="domain-name">{{ item.domain }}</span>
								<span class="status-badge" v-if="item.available">
									<i class="status-icon">✓</i> 可注册
								</span>
								<span class="status-badge occupied" v-else>
									<i class="status-icon">×</i> 已被占用
								</span>
							</div>
							<div class="action-area">
								<button v-if="item.available" class="apply-btn" @click="goToLogin">
									免费申请
								</button>
								<span v-else class="reason-text">
									{{ item.reason === 'occupied' ? '已被他人注册' : '暂时不可用' }}
								</span>
							</div>
						</div>
					</div>
				</div>
			</div>
		</main>

		<!-- 底部 -->
		<footer class="footer">
			<p>&copy; 2025 HyFreeDom</p>
		</footer>
	</div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet } from '@/utils/api.js'
import { ElMessage } from 'element-plus'

const router = useRouter()
const isScrolled = ref(false)
const searchPrefix = ref('')
const loading = ref(false)
const searchResults = ref([])
const mouseX = ref(0)
const mouseY = ref(0)
const canvasRef = ref(null)

// 粒子系统
let ctx = null
let animationFrameId = null
const particles = []
const trailParticles = []
let hue = 0

// 随机数生成
const random = (min, max) => Math.random() * (max - min) + min

// 创建拖尾粒子
const createTrail = (x, y) => {
	trailParticles.push({
		x,
		y,
		size: random(2, 5),
		color: `hsla(${hue}, 100%, 70%, 0.8)`,
		life: 1,
		decay: 0.05
	})
}

// 创建点击爆炸粒子
const createExplosion = (x, y) => {
	const particleCount = 20
	for (let i = 0; i < particleCount; i++) {
		const angle = (Math.PI * 2 / particleCount) * i + random(-0.2, 0.2)
		const velocity = random(2, 6)
		particles.push({
			x,
			y,
			vx: Math.cos(angle) * velocity,
			vy: Math.sin(angle) * velocity,
			size: random(2, 6),
			color: `hsla(${hue + random(-30, 30)}, 100%, 70%, 1)`,
			life: 1,
			decay: random(0.02, 0.04)
		})
	}
}

// 动画循环
const animate = () => {
	if (!ctx) return
	ctx.clearRect(0, 0, window.innerWidth, window.innerHeight)

	hue += 2 // 颜色轮转

	// 更新和绘制拖尾
	for (let i = trailParticles.length - 1; i >= 0; i--) {
		const p = trailParticles[i]
		p.life -= p.decay

		if (p.life <= 0) {
			trailParticles.splice(i, 1)
			continue
		}

		ctx.beginPath()
		ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
		ctx.fillStyle = p.color
		ctx.globalAlpha = p.life
		ctx.shadowBlur = 10
		ctx.shadowColor = p.color
		ctx.fill()
	}

	// 更新和绘制爆炸粒子
	for (let i = particles.length - 1; i >= 0; i--) {
		const p = particles[i]
		p.x += p.vx
		p.y += p.vy
		p.life -= p.decay

		if (p.life <= 0) {
			particles.splice(i, 1)
			continue
		}

		ctx.beginPath()
		ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
		ctx.fillStyle = p.color
		ctx.globalAlpha = p.life
		ctx.shadowBlur = 15
		ctx.shadowColor = p.color
		ctx.fill()
	}

	ctx.globalAlpha = 1
	animationFrameId = requestAnimationFrame(animate)
}

// 鼠标移动监听
const handleMouseMove = (e) => {
	mouseX.value = e.clientX - window.innerWidth / 2
	mouseY.value = e.clientY - window.innerHeight / 2

	// 添加拖尾
	createTrail(e.clientX, e.clientY)
}

// 点击监听
const handleClick = (e) => {
	createExplosion(e.clientX, e.clientY)
}

// 窗口大小调整
const handleResize = () => {
	if (canvasRef.value) {
		canvasRef.value.width = window.innerWidth
		canvasRef.value.height = window.innerHeight
	}
}

// 滚动监听
const handleScroll = () => {
	isScrolled.value = window.scrollY > 50
}

onMounted(() => {
	window.addEventListener('scroll', handleScroll)
	window.addEventListener('mousemove', handleMouseMove)
	window.addEventListener('click', handleClick)
	window.addEventListener('resize', handleResize)

	// 初始化 Canvas
	if (canvasRef.value) {
		ctx = canvasRef.value.getContext('2d')
		canvasRef.value.width = window.innerWidth
		canvasRef.value.height = window.innerHeight
		animate()
	}
})

onUnmounted(() => {
	window.removeEventListener('scroll', handleScroll)
	window.removeEventListener('mousemove', handleMouseMove)
	window.removeEventListener('click', handleClick)
	window.removeEventListener('resize', handleResize)
	if (animationFrameId) {
		cancelAnimationFrame(animationFrameId)
	}
})

// 搜索功能
const handleSearch = async () => {
	if (!searchPrefix.value) return

	// 简单校验
	if (!/^[a-zA-Z0-9-]+$/.test(searchPrefix.value)) {
		ElMessage.warning('前缀只能包含字母、数字和连字符')
		return
	}

	loading.value = true
	searchResults.value = []

	try {
		const response = await apiGet('/api/domains/search', {
			params: { prefix: searchPrefix.value }
		})

		if (response.data) {
			searchResults.value = response.data
			if (searchResults.value.length === 0) {
				ElMessage.info('暂无可用域名')
			}
		}
	} catch (error) {
		console.error('Search failed:', error)
		ElMessage.error('查询失败，请稍后重试')
	} finally {
		loading.value = false
	}
}

// 跳转登录
const goToLogin = () => {
	router.push('/user/login')
}
</script>

<style scoped>
.landing-page {
	min-height: 100vh;
	background-color: #0f172a;
	color: #fff;
	font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
	overflow-x: hidden;
}

/* 导航栏 */
.navbar {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	padding: 20px 40px;
	z-index: 100;
	transition: all 0.3s ease;
}

.navbar.scrolled {
	background: rgba(15, 23, 42, 0.8);
	backdrop-filter: blur(10px);
	padding: 15px 40px;
	border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.nav-content {
	max-width: 1200px;
	margin: 0 auto;
	display: flex;
	justify-content: space-between;
	align-items: center;
}

.logo {
	display: flex;
	align-items: center;
	gap: 10px;
	font-size: 24px;
	font-weight: 700;
}

.nav-links {
	display: flex;
	align-items: center;
	gap: 24px;
}

.nav-link {
	color: #cbd5e1;
	text-decoration: none;
	font-weight: 500;
	transition: color 0.2s;
}

.nav-link:hover {
	color: #fff;
}

.nav-btn {
	background: #6366f1;
	color: #fff;
	padding: 8px 20px;
	border-radius: 20px;
	text-decoration: none;
	font-weight: 600;
	transition: all 0.2s;
}

.nav-btn:hover {
	background: #4f46e5;
	transform: translateY(-1px);
}

/* 主视觉区 */
.hero-section {
	min-height: 100vh;
	display: flex;
	flex-direction: column;
	justify-content: center;
	align-items: center;
	position: relative;
	padding: 100px 20px;
	text-align: center;
}

/* 背景动画 */
.bg-animation {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	overflow: hidden;
	z-index: 0;
	background: radial-gradient(circle at center, #1e293b 0%, #0f172a 100%);
}

.interaction-canvas {
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 100%;
	pointer-events: none;
	z-index: 1;
}

.grid-overlay {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background-image:
		linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
		linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
	background-size: 50px 50px;
	perspective: 500px;
	transform-origin: center;
	opacity: 0.5;
	mask-image: radial-gradient(circle at center, black 40%, transparent 100%);
}

.stars {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background-image:
		radial-gradient(1px 1px at 10% 10%, white 100%, transparent),
		radial-gradient(1px 1px at 20% 40%, white 100%, transparent),
		radial-gradient(2px 2px at 30% 70%, white 100%, transparent),
		radial-gradient(1px 1px at 40% 20%, white 100%, transparent),
		radial-gradient(1px 1px at 50% 50%, white 100%, transparent),
		radial-gradient(2px 2px at 60% 80%, white 100%, transparent),
		radial-gradient(1px 1px at 70% 30%, white 100%, transparent),
		radial-gradient(1px 1px at 80% 60%, white 100%, transparent),
		radial-gradient(2px 2px at 90% 10%, white 100%, transparent);
	background-size: 200px 200px;
	opacity: 0.3;
	animation: twinkle 5s infinite;
}

.blob {
	position: absolute;
	border-radius: 50%;
	filter: blur(60px);
	opacity: 0.5;
	mix-blend-mode: screen;
	transition: transform 0.1s ease-out;
}

.blob-1 {
	width: 500px;
	height: 500px;
	background: radial-gradient(circle, #6366f1 0%, transparent 70%);
	top: -150px;
	left: -150px;
	animation: float 15s infinite ease-in-out;
}

.blob-2 {
	width: 400px;
	height: 400px;
	background: radial-gradient(circle, #ec4899 0%, transparent 70%);
	bottom: -50px;
	right: -100px;
	animation: float 12s infinite ease-in-out reverse;
}

.blob-3 {
	width: 450px;
	height: 450px;
	background: radial-gradient(circle, #06b6d4 0%, transparent 70%);
	top: 30%;
	left: 20%;
	animation: float 18s infinite ease-in-out 2s;
}

.blob-4 {
	width: 350px;
	height: 350px;
	background: radial-gradient(circle, #8b5cf6 0%, transparent 70%);
	bottom: 20%;
	left: 30%;
	animation: float 14s infinite ease-in-out 5s;
}

@keyframes float {

	0%,
	100% {
		transform: translate(0, 0) rotate(0deg);
	}

	25% {
		transform: translate(20px, 30px) rotate(5deg);
	}

	50% {
		transform: translate(-20px, 10px) rotate(-5deg);
	}

	75% {
		transform: translate(10px, -20px) rotate(2deg);
	}
}

@keyframes twinkle {

	0%,
	100% {
		opacity: 0.3;
	}

	50% {
		opacity: 0.5;
	}
}

.content-wrapper {
	position: relative;
	z-index: 1;
	max-width: 800px;
	width: 100%;
}

.hero-title {
	font-size: 64px;
	line-height: 1.1;
	font-weight: 800;
	margin-bottom: 24px;
	letter-spacing: -0.02em;
}

.gradient-text {
	background: linear-gradient(135deg, #fff 0%, #94a3b8 100%);
	-webkit-background-clip: text;
	-webkit-text-fill-color: transparent;
}

.hero-subtitle {
	font-size: 20px;
	color: #94a3b8;
	margin-bottom: 48px;
	line-height: 1.6;
}

/* 搜索框 */
.search-container {
	margin-bottom: 48px;
	transform: translateY(0);
	transition: transform 0.3s ease;
}

.search-container.has-results {
	transform: translateY(-20px);
}

.input-group {
	display: flex;
	background: rgba(255, 255, 255, 0.05);
	backdrop-filter: blur(10px);
	border: 1px solid rgba(255, 255, 255, 0.1);
	border-radius: 16px;
	padding: 8px;
	transition: all 0.3s ease;
	max-width: 600px;
	margin: 0 auto;
}

.input-group:focus-within {
	background: rgba(255, 255, 255, 0.1);
	border-color: rgba(99, 102, 241, 0.5);
	box-shadow: 0 0 30px rgba(99, 102, 241, 0.2);
}

.search-input {
	flex: 1;
	background: transparent;
	border: none;
	padding: 16px 24px;
	color: #fff;
	font-size: 18px;
	outline: none;
}

.search-input::placeholder {
	color: #64748b;
}

.search-btn {
	background: #6366f1;
	color: #fff;
	border: none;
	padding: 0 32px;
	border-radius: 12px;
	font-size: 16px;
	font-weight: 600;
	cursor: pointer;
	transition: all 0.2s;
	min-width: 100px;
}

.search-btn:hover:not(:disabled) {
	background: #4f46e5;
}

.search-btn:disabled {
	opacity: 0.7;
	cursor: not-allowed;
}

.input-hint {
	margin-top: 12px;
	font-size: 14px;
	color: #64748b;
}

/* 搜索结果 */
.results-container {
	max-width: 800px;
	margin: 0 auto;
	animation: slideUp 0.5s ease-out;
}

@keyframes slideUp {
	from {
		opacity: 0;
		transform: translateY(20px);
	}

	to {
		opacity: 1;
		transform: translateY(0);
	}
}

.results-grid {
	display: grid;
	gap: 16px;
}

.result-card {
	background: rgba(255, 255, 255, 0.05);
	backdrop-filter: blur(10px);
	border: 1px solid rgba(255, 255, 255, 0.1);
	border-radius: 12px;
	padding: 20px 24px;
	display: flex;
	justify-content: space-between;
	align-items: center;
	transition: all 0.3s ease;
}

.result-card:hover {
	background: rgba(255, 255, 255, 0.08);
	transform: translateY(-2px);
}

.result-card.available {
	border-left: 4px solid #10b981;
}

.result-card.unavailable {
	border-left: 4px solid #ef4444;
	opacity: 0.8;
}

.domain-info {
	display: flex;
	flex-direction: column;
	align-items: flex-start;
	gap: 4px;
}

.domain-name {
	font-size: 18px;
	font-weight: 600;
	color: #fff;
}

.status-badge {
	font-size: 13px;
	color: #10b981;
	display: flex;
	align-items: center;
	gap: 4px;
}

.status-badge.occupied {
	color: #ef4444;
}

.status-icon {
	font-style: normal;
	font-weight: bold;
}

.apply-btn {
	background: linear-gradient(135deg, #10b981 0%, #059669 100%);
	color: #fff;
	border: none;
	padding: 10px 24px;
	border-radius: 8px;
	font-weight: 600;
	cursor: pointer;
	transition: all 0.2s;
	box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.apply-btn:hover {
	transform: translateY(-1px);
	box-shadow: 0 6px 16px rgba(16, 185, 129, 0.4);
}

.reason-text {
	color: #64748b;
	font-size: 14px;
}

/* Loader */
.loader {
	width: 20px;
	height: 20px;
	border: 2px solid #fff;
	border-bottom-color: transparent;
	border-radius: 50%;
	display: inline-block;
	animation: rotation 1s linear infinite;
}

@keyframes rotation {
	0% {
		transform: rotate(0deg);
	}

	100% {
		transform: rotate(360deg);
	}
}

.footer {
	position: absolute;
	bottom: 0;
	width: 100%;
	padding: 20px;
	text-align: center;
	color: #475569;
	font-size: 14px;
	z-index: 1;
}

/* 移动端适配 */
@media (max-width: 768px) {
	.hero-title {
		font-size: 40px;
	}

	.hero-subtitle {
		font-size: 16px;
	}

	.navbar {
		padding: 15px 20px;
	}

	.result-card {
		flex-direction: column;
		gap: 16px;
		align-items: flex-start;
	}

	.action-area {
		width: 100%;
		display: flex;
		justify-content: flex-end;
	}
}
</style>