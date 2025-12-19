<template>
  <Teleport to="body">
    <div class="featured-sites-sidebar" :class="{ 'is-expanded': isExpanded }">
      <!-- Toggle Button -->
      <div class="toggle-btn" @click="toggleSidebar">
        <svg v-if="!isExpanded" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
        <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
          stroke-linecap="round" stroke-linejoin="round">
          <polyline points="9 18 15 12 9 6"></polyline>
        </svg>
      </div>

      <!-- Content -->
      <div class="sidebar-content">
        <div class="sidebar-inner">
          <div class="sites-list">
            <a v-for="site in sites" :key="site.id" :href="site.url" target="_blank" class="site-item">
              <img v-if="site.logoUrl" :src="site.logoUrl" :alt="site.name" class="site-logo">
              <div class="site-info">
                <span class="site-name">{{ site.name }}</span>
                <span v-if="site.description" class="site-desc">{{ site.description }}</span>
              </div>
            </a>
            <div v-if="sites.length === 0" class="empty-state">
              暂无推荐网站
            </div>
          </div>

          <div class="sidebar-footer">
            <div class="marquee-container">
              <div class="marquee-content">
                如果您使用本站提供的域名，想分享您的站点，可以
                <el-tooltip effect="dark" content="QQ：1010411661，微信：abc1010411661" placement="top">
                  <span class="contact-link">联系站长</span>
                </el-tooltip>
                添加到此列表进行展示
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { apiGet } from '@/utils/api.js'

const isExpanded = ref(false)
const sites = ref([])

const toggleSidebar = () => {
  isExpanded.value = !isExpanded.value
}

const fetchSites = async () => {
  try {
    const result = await apiGet('/api/user/featured-sites')
    if (result.code === 200) {
      sites.value = result.data
    }
  } catch (error) {
    console.error('Failed to fetch featured sites:', error)
  }
}

onMounted(() => {
  fetchSites()
})
</script>

<style scoped>
.featured-sites-sidebar {
  position: fixed;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  z-index: 999999;
  display: flex;
  align-items: flex-start;
  transition: all 0.3s ease;
}

.toggle-btn {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-right: none;
  border-radius: 8px 0 0 8px;
  width: 40px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #333;
  box-shadow: -2px 0 10px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.toggle-btn:hover {
  background: rgba(255, 255, 255, 0.4);
}

.sidebar-content {
  width: 0;
  opacity: 0;
  height: 70vh;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid transparent;
  border-radius: 0 0 0 16px;
  box-shadow: none;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.is-expanded .sidebar-content {
  width: 300px;
  opacity: 1;
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.1);
}

.sites-list {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
}

.site-item {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 10px;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 10px;
  text-decoration: none;
  color: #333;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.site-item:hover {
  background: rgba(255, 255, 255, 0.7);
  transform: translateX(-5px);
  border-color: rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.site-logo {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  margin-right: 12px;
  object-fit: cover;
  background-color: #f0f0f0;
}

.site-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.site-name {
  font-weight: 500;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.site-desc {
  font-size: 12px;
  color: #666;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.empty-state {
  text-align: center;
  color: #666;
  padding: 20px;
  font-size: 14px;
}

.sidebar-footer {
  padding: 10px;
  background: rgba(255, 255, 255, 0.3);
  font-size: 12px;
  color: #555;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.marquee-container {
  overflow: hidden;
  white-space: nowrap;
  width: 100%;
}

.marquee-content {
  display: inline-block;
  animation: marquee 15s linear infinite;
  padding-left: 100%;
}

.contact-link {
  color: #409eff;
  font-weight: bold;
  cursor: pointer;
  text-decoration: underline;
}

@keyframes marquee {
  0% {
    transform: translateX(0);
  }

  100% {
    transform: translateX(-100%);
  }
}

.sites-list::-webkit-scrollbar {
  width: 6px;
}

.sites-list::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
}

.sites-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}
</style>
