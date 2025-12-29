# DomainDNS - 二级域名分发系统

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.4.21-4FC08D.svg)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-6.0+-red.svg)](https://redis.io/)

一个基于 Spring Boot 和 Vue.js 的现代化二级域名分发平台，支持 Cloudflare DNS 管理、积分系统、邀请机制和完整的用户管理功能。

这是我搭建好的域名分发网站：https://freedns.hyper99.shop/

## ✨ 主要特性

### 🎯 核心功能
- **二级域名申请** - 用户通过积分申请自定义子域名
- **Cloudflare 集成** - 自动同步和管理 Cloudflare DNS 记录
- **积分系统** - 完整的积分获取、消费和交易机制
- **邀请机制** - 用户邀请奖励和邀请码管理
- **GitHub 任务** - 通过 GitHub Star 任务获取积分
- **支付充值** - 支持多种支付方式充值积分

### 🔧 管理功能
- **多账户管理** - 支持多个 Cloudflare 账户
- **域名管理** - 批量同步和管理域名
- **用户管理** - 用户信息、积分调整、状态管理
- **系统设置** - 灵活的配置管理
- **审计日志** - 完整的操作记录和审计
- **数据统计** - 丰富的仪表板和数据可视化

### 🛡️ 安全特性
- **JWT 认证** - 安全的用户认证机制
- **权限控制** - 基于角色的访问控制
- **数据加密** - 敏感数据 AES 加密存储
- **限流保护** - 防止恶意请求和滥用
- **审计追踪** - 完整的操作日志记录

## 🏗️ 技术架构

### 后端技术栈
- **Java 21** - 现代 Java 特性
- **Spring Boot 3.1.5** - 企业级应用框架
- **Spring Security** - 安全认证和授权
- **MyBatis** - 数据持久层框架
- **MySQL 8.0+** - 关系型数据库
- **Redis** - 缓存和会话存储
- **Spring Mail** - 邮件服务
- **JWT** - 无状态认证

### 前端技术栈
- **Vue.js 3** - 渐进式前端框架
- **Element Plus** - 企业级 UI 组件库
- **Pinia** - 状态管理
- **Vue Router** - 路由管理
- **Vite** - 现代化构建工具
- **Quill** - 富文本编辑器

## 📋 系统要求

### 最低要求
- **Java**: 21+
- **Node.js**: 16+
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **内存**: 2GB+
- **存储**: 10GB+

### 推荐配置
- **Java**: 21 LTS
- **Node.js**: 18 LTS
- **MySQL**: 8.0+
- **Redis**: 7.0+
- **内存**: 4GB+
- **存储**: 50GB+ SSD

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/your-username/DomainDNS.git
cd DomainDNS
```

### 2. 数据库配置

创建 MySQL 数据库并导入表结构：

```bash
mysql -u root -p
CREATE DATABASE domaindns CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE domaindns;
SOURCE backend/db/schema.sql;
```

### 3. 后端配置

#### 3.1 复制配置文件

```bash
cp backend/src/main/resources/application-dev.yml.template backend/src/main/resources/application-dev.yml
cp backend/src/main/resources/application-prod.yml.template backend/src/main/resources/application-prod.yml
```

#### 3.2 配置环境变量

创建 `.env` 文件：

```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=domaindns
DB_USER=root
DB_PASSWORD=your_password

# Redis 配置
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASS=

# JWT 配置
JWT_SECRET=your-very-strong-secret-key-here
JWT_ISSUER=domaindns
JWT_EXPIRE_MINUTES=120

# 邮件配置
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USER=your-email@qq.com
MAIL_PASS=your-email-password

# 加密密钥
SECRETS_AES_KEY=your-32-char-aes-key-here

# GitHub API
GITHUB_TOKEN=your-github-token
```

#### 3.3 启动后端服务

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动。

### 4. 前端配置

#### 4.1 安装依赖

```bash
cd UI
npm install
```

#### 4.2 配置环境变量

```bash
# 开发环境
cp env.development .env.development

# 生产环境
cp env.production .env.production
```

#### 4.3 启动前端服务

```bash
# 开发环境
npm run dev

# 生产环境
npm run dev:prod
```

前端服务将在 `http://localhost:5173` 启动。

## 📖 使用指南

### 管理员操作

1. **注册管理员账号**
   - 访问 `/api/auth/admin/register`
   - 或使用数据库直接插入管理员用户

2. **配置 Cloudflare 账户**
   - 登录管理后台
   - 添加 Cloudflare API Key 或 Token
   - 同步域名列表

3. **启用域名分发**
   - 选择要用于分发的域名
   - 启用域名状态
   - 配置积分策略

### 用户操作

1. **用户注册**
   - 访问注册页面
   - 填写用户信息
   - 验证邮箱（可选）

2. **申请子域名**
   - 选择可用域名
   - 输入子域名前缀
   - 配置 DNS 记录
   - 消耗积分完成申请

3. **管理域名**
   - 查看已申请的域名
   - 释放不需要的域名
   - 查看积分余额和流水

## 🔧 配置说明

### 系统设置

系统支持以下可配置参数（存储在 `system_settings` 表）：

| 参数名 | 默认值 | 说明 |
|--------|--------|------|
| `initial_register_points` | 1 | 注册奖励积分 |
| `invitee_points` | 3 | 被邀请者奖励积分 |
| `inviter_points` | 3 | 邀请者奖励积分 |
| `domain_cost_points` | 5 | 申请域名消耗积分 |
| `sync_cron_expression` | `0 */5 * * * *` | 同步任务执行时间 |
| `default_ttl` | 120 | 默认 DNS TTL |
| `max_domains_per_user` | 5 | 每用户最大域名数 |
| `allow_user_set_invite` | true | 允许用户设置邀请码 |

### Cloudflare 配置

支持两种认证方式：

1. **Global API Key**
   - 需要邮箱和 Global API Key
   - 权限较高，建议谨慎使用

2. **API Token**
   - 推荐使用，权限更精细
   - 支持自定义权限范围

## 📊 API 文档

### 认证接口

#### 用户注册
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123",
  "inviteCode": "optional"
}
```

#### 用户登录
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

### 域名管理

#### 申请子域名
```http
POST /api/user/domains/apply
Authorization: Bearer <token>
Content-Type: application/json

{
  "zoneId": 1,
  "prefix": "mysite",
  "type": "A",
  "value": "1.2.3.4",
  "ttl": 120,
  "remark": "我的网站"
}
```

#### 获取我的域名
```http
GET /api/user/domains?page=1&size=20
Authorization: Bearer <token>
```

### 管理员接口

#### 同步域名
```http
POST /api/admin/zones/sync
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "cfAccountId": 1
}
```

#### 启用域名
```http
POST /api/admin/zones/{id}/enable
Authorization: Bearer <admin-token>
```

## 🐳 Docker 部署

### 使用 Docker Compose

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: domaindns
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - DB_HOST=mysql
      - REDIS_HOST=redis
    depends_on:
      - mysql
      - redis

  frontend:
    build: ./UI
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  mysql_data:
```

启动服务：

```bash
docker-compose up -d
```

## 🔍 监控和日志

### 健康检查

系统提供以下监控端点：

- `GET /actuator/health` - 应用健康状态
- `GET /actuator/info` - 应用信息
- `GET /actuator/metrics` - 应用指标

### 日志配置

日志文件位置：
- 应用日志：`logs/application.log`
- 错误日志：`logs/error.log`
- 审计日志：数据库 `audit_logs` 表

## 🤝 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📝 开发计划

- [ ] 支持更多 DNS 提供商（阿里云、腾讯云等）
- [ ] 增加域名自动续期功能
- [ ] 支持批量域名操作
- [ ] 增加更多支付方式
- [ ] 支持域名转移功能
- [ ] 增加 API 限流和监控
- [ ] 支持多语言国际化

## 🐛 问题反馈

如果您遇到问题或有建议，请：

1. 查看 [Issues](https://github.com/your-username/DomainDNS/issues) 是否已有相关问题
2. 创建新的 Issue 并详细描述问题
3. 提供复现步骤和环境信息

## 📄 许可证

本项目采用 [MIT 许可证](LICENSE)。

## 🙏 致谢

感谢以下开源项目的支持：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Vue.js](https://vuejs.org/)
- [Element Plus](https://element-plus.org/)
- [MyBatis](https://mybatis.org/)
- [Cloudflare API](https://api.cloudflare.com/)

---

**注意**: 请在生产环境中修改所有默认密码和密钥，并确保数据库和 Redis 的安全配置。

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=970thunder/HyFreeDom&type=date&legend=top-left)](https://www.star-history.com/#970thunder/HyFreeDom&type=date&legend=top-left)
