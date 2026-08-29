/**
 * SEO配置
 * 用于动态设置页面标题、描述和关键词
 */

// 基础SEO配置
export const SEO_CONFIG = {
    // 网站基础信息
    siteName: 'HyFreeDom',
    siteUrl: 'https://freedns.hyper99.top',
    siteDescription: '专业的免费二级域名申请平台，为用户提供免费的二级域名服务。支持自定义域名、DNS解析、域名管理等功能。',

    // 默认关键词
    defaultKeywords: [
        '免费域名',
        '二级域名',
        '免费二级域名',
        '域名申请',
        '免费域名注册',
        'DNS解析',
        '个人域名',
        '网站域名',
        '免费网站域名',
        '域名管理',
        '域名解析',
        '免费DNS',
        '域名服务',
        '免费子域名',
        '自定义域名',
        '域名绑定',
        '域名转发'
    ],

    // 页面特定配置
    pages: {
        // 首页
        home: {
            title: 'HyFreeDom - 免费二级域名申请平台 | 免费获取个人域名',
            description: 'HyFreeDom是专业的免费二级域名申请平台，为用户提供免费的二级域名服务。支持自定义域名、DNS解析、域名管理等功能，是个人网站、博客、项目的理想选择。',
            keywords: ['免费域名', '二级域名', '免费二级域名', '域名申请', '免费域名注册', 'DNS解析', '个人域名', '网站域名']
        },

        // 用户登录页
        userLogin: {
            title: '用户登录 - HyFreeDom免费二级域名平台',
            description: '登录HyFreeDom免费二级域名平台，管理您的域名和DNS解析设置。',
            keywords: ['用户登录', '免费域名', '二级域名', '域名管理']
        },

        // 用户注册页
        userRegister: {
            title: '用户注册 - HyFreeDom免费二级域名平台',
            description: '注册HyFreeDom免费二级域名平台，开始申请您的免费二级域名。',
            keywords: ['用户注册', '免费域名', '二级域名', '域名申请']
        },

        // 申请域名页
        applyDomain: {
            title: '申请免费二级域名 - HyFreeDom',
            description: '在HyFreeDom平台申请免费的二级域名，支持自定义域名前缀，快速获得个人域名。',
            keywords: ['申请域名', '免费二级域名', '自定义域名', '域名申请', '个人域名']
        },

        // 我的域名页
        myDomains: {
            title: '我的域名管理 - HyFreeDom',
            description: '管理您在HyFreeDom平台申请的所有域名，包括DNS解析设置和域名状态管理。',
            keywords: ['域名管理', '我的域名', 'DNS解析', '域名设置', '域名状态']
        },

        // 管理员登录页
        adminLogin: {
            title: '管理员登录 - HyFreeDom管理后台',
            description: 'HyFreeDom管理后台登录页面，用于系统管理和用户服务。',
            keywords: ['管理员登录', '管理后台', '系统管理']
        },

        // 管理员仪表板
        adminDashboard: {
            title: '管理后台 - HyFreeDom系统管理',
            description: 'HyFreeDom管理后台，提供用户管理、域名管理、系统设置等功能。',
            keywords: ['管理后台', '系统管理', '用户管理', '域名管理']
        }
    }
}

/**
 * 设置页面SEO信息
 * @param {Object} options - SEO配置选项
 * @param {string} options.title - 页面标题
 * @param {string} options.description - 页面描述
 * @param {string[]} options.keywords - 页面关键词
 * @param {string} options.url - 页面URL
 * @param {string} options.image - 页面图片
 */
export function setPageSEO(options) {
    const {
        title,
        description,
        keywords = [],
        url = window.location.href,
        image = '/src/assets/logo.ico'
    } = options

    // 设置页面标题
    if (title) {
        document.title = title
    }

    // 设置或更新meta标签
    const setMetaTag = (name, content, property = false) => {
        const selector = property ? `meta[property="${name}"]` : `meta[name="${name}"]`
        let meta = document.querySelector(selector)

        if (!meta) {
            meta = document.createElement('meta')
            if (property) {
                meta.setAttribute('property', name)
            } else {
                meta.setAttribute('name', name)
            }
            document.head.appendChild(meta)
        }
        meta.setAttribute('content', content)
    }

    // 设置基础SEO标签
    if (description) {
        setMetaTag('description', description)
        setMetaTag('og:description', description, true)
        setMetaTag('twitter:description', description)
    }

    if (keywords.length > 0) {
        setMetaTag('keywords', keywords.join(', '))
    }

    // 设置Open Graph标签
    setMetaTag('og:title', title, true)
    setMetaTag('og:url', url, true)
    setMetaTag('og:image', image, true)
    setMetaTag('og:type', 'website', true)

    // 设置Twitter Card标签
    setMetaTag('twitter:title', title)
    setMetaTag('twitter:image', image)
}

/**
 * 获取页面SEO配置
 * @param {string} pageName - 页面名称
 * @returns {Object} 页面SEO配置
 */
export function getPageSEO(pageName) {
    return SEO_CONFIG.pages[pageName] || SEO_CONFIG.pages.home
}

/**
 * 生成结构化数据
 * @param {Object} options - 结构化数据选项
 * @returns {Object} 结构化数据对象
 */
export function generateStructuredData(options = {}) {
    const {
        type = 'WebSite',
        name = SEO_CONFIG.siteName,
        description = SEO_CONFIG.siteDescription,
        url = SEO_CONFIG.siteUrl,
        potentialAction = null
    } = options

    const structuredData = {
        '@context': 'https://schema.org',
        '@type': type,
        name,
        description,
        url
    }

    if (potentialAction) {
        structuredData.potentialAction = potentialAction
    }

    return structuredData
}
