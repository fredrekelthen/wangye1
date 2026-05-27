# 🎓 智能教育平台

基于Spring Boot + Vue.js构建的现代化智能教育平台，集成AI Agent智能助手、RAG知识库检索增强、大模型交互等核心功能。

## 📋 项目概览

本项目是一个功能完善的在线教育平台，提供课程管理、学习进度跟踪、智能问答、AI辅助学习等功能。

### ✨ 核心功能

| 模块 | 功能描述 |
|------|----------|
| 🎯 **用户管理** | 用户注册、登录、个人中心、学习进度追踪 |
| 📚 **课程管理** | 课程结构管理、章节划分、课程内容展示 |
| 📁 **资料管理** | 学习资料上传、下载、分类管理 |
| 🤖 **AI智能助手** | RAG知识库检索、大模型问答、智能学习推荐 |
| 📊 **学习分析** | 学习成绩统计、知识图谱可视化、学习路径分析 |
| 💬 **讨论互动** | 课程讨论区、学习社区互动 |

## 🏗️ 技术架构

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 编程语言 |
| Spring Boot | 3.2.x | 后端框架 |
| Spring Data JPA | 3.2.x | 数据访问层 |
| SQLite | 3.x | 数据库 |
| Maven | 3.9+ | 构建工具 |

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue.js | 3.x | 前端框架 |
| TypeScript | 5.x | 类型安全 |
| Vue Router | 4.x | 路由管理 |
| Element Plus | 2.x | UI组件库 |
| Axios | 1.x | HTTP客户端 |

### AI技术栈

| 技术 | 说明 |
|------|------|
| GLM API | 大语言模型接口 |
| RAG | 检索增强生成 |
| 文件解析 | 支持多种文档格式解析 |

## 📁 项目结构

```
.
├── houduan/                    # 后端模块
│   ├── backend/                # Spring Boot主后端
│   │   ├── src/main/java/com/edu/backend/
│   │   │   ├── controller/     # REST API控制器
│   │   │   ├── service/        # 业务服务层
│   │   │   ├── repository/     # 数据访问层
│   │   │   ├── model/          # 数据模型
│   │   │   ├── config/         # 配置类
│   │   │   └── utils/          # 工具类
│   │   └── src/main/resources/
│   │       └── application.yml # 应用配置
│   └── learning-agent/         # AI Agent模块
│       ├── backend/            # Agent后端服务
│       ├── frontend/           # Agent前端界面
│       └── config/             # Agent配置
├── qianduan/                   # 前端主应用
│   ├── public/                 # 静态资源
│   └── src/
│       ├── components/         # Vue组件
│       ├── views/              # 页面视图
│       ├── router/             # 路由配置
│       ├── store/              # 状态管理
│       └── api/                # API接口
├── .gitignore                  # Git忽略配置
└── README.md                   # 项目说明
```

## 🚀 快速开始

### 环境要求

- JDK 21+
- Node.js 20+
- Maven 3.9+

### 后端启动

```bash
# 进入后端目录
cd houduan/backend

# 编译运行
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动。

### 前端启动

```bash
# 进入前端目录
cd qianduan

# 安装依赖
npm install

# 启动开发服务器
npm run serve
```

前端页面将在 `http://localhost:8081` 启动。

### AI Agent启动

```bash
# 进入Agent后端目录
cd houduan/learning-agent/backend

# 编译运行
mvn spring-boot:run

# 进入Agent前端目录
cd ../frontend

# 安装依赖并启动
npm install
npm run serve
```

## 🔌 API接口

### 用户接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 登录 | POST | `/api/user/login` | 用户登录 |
| 注册 | POST | `/api/user/register` | 用户注册 |
| 获取用户信息 | GET | `/api/user/info` | 获取当前用户信息 |

### 课程接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 获取课程列表 | GET | `/api/course/list` | 获取课程列表 |
| 获取课程详情 | GET | `/api/course/{id}` | 获取课程详情 |

### AI Agent接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 智能问答 | POST | `/api/agent/chat` | 与AI助手对话 |
| 知识库检索 | POST | `/api/agent/rag/search` | RAG知识库检索 |

## 📊 数据库设计

### 用户表 (User)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户ID |
| username | String | 用户名 |
| email | String | 邮箱 |
| password | String | 密码(加密) |
| createdAt | DateTime | 创建时间 |

### 课程表 (Course)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 课程ID |
| name | String | 课程名称 |
| description | String | 课程描述 |
| structure | JSON | 课程结构 |

### 学习进度表 (UserProgress)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 进度ID |
| userId | Long | 用户ID |
| courseId | Long | 课程ID |
| progress | JSON | 学习进度数据 |

## 🤝 贡献指南

欢迎贡献代码！请遵循以下流程：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 📄 许可证

本项目采用 MIT 许可证，详见 [LICENSE](LICENSE) 文件。

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

- 项目地址：https://github.com/fredrekelthen/wangye1
- 提交 Issue 或 Pull Request

---

**🎉 感谢使用智能教育平台！**

*持续更新中...*
