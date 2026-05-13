# 电磁场课程学习平台 - 后端项目

基于 Spring Boot + MongoDB 的后端服务，为前端提供用户管理、课程管理、学习资料管理等功能。

## 📋 项目概述

### 技术栈
- **Spring Boot 3.5.13** - Java Web 框架
- **Spring Data MongoDB** - MongoDB 数据访问
- **MongoDB** - 文档型数据库
- **Lombok** - 简化 Java 代码
- **Jakarta Validation** - 参数校验

### 项目结构
```
backend/
├── src/main/java/com/edu/backend/
│   ├── config/          # 配置类
│   │   ├── CorsConfig.java           # 跨域配置
│   │   └── DataInitializer.java      # 数据初始化
│   ├── controller/      # 控制器层
│   │   ├── UserController.java        # 用户控制器
│   │   ├── MaterialController.java     # 资料控制器
│   │   └── CourseStructureController.java  # 课程控制器
│   ├── model/           # 数据模型
│   │   ├── dto/        # 数据传输对象
│   │   ├── entity/     # 实体类
│   │   └── enums/      # 枚举类
│   ├── repository/      # 数据访问层
│   ├── service/         # 业务逻辑层
│   │   └── impl/
│   └── utils/          # 工具类
├── src/main/resources/
│   └── application.yml # 配置文件
└── pom.xml            # Maven 依赖配置
```

## 🚀 快速开始

### 环境要求
- JDK 17 或更高版本
- MongoDB 4.0 或更高版本
- Maven 3.6 或更高版本

### 安装步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd backend
   ```

2. **配置数据库**
   编辑 `src/main/resources/application.yml`：
   ```yaml
   spring:
     data:
       mongodb:
         uri: mongodb://localhost:27017/edu_platform
   ```

3. **启动 MongoDB**
   ```bash
   # Windows
   mongod

   # Linux/Mac
   sudo mongod
   ```

4. **编译项目**
   ```bash
   mvn clean install
   ```

5. **运行项目**
   ```bash
   mvn spring-boot:run
   ```

6. **验证启动**
   访问 `http://localhost:8080/api/course/structure`，应返回课程结构数据。

## 📡 API 文档

### 1. 用户模块

#### 1.1 用户注册
```
POST /api/user/register
Content-Type: application/json

Request Body:
{
  "userAccount": "testuser",
  "userPassword": "123456",
  "checkPassword": "123456",
  "planetCode": "123456"
}

Response:
{
  "code": 0,
  "data": 1,
  "description": "注册成功"
}
```

#### 1.2 用户登录
```
POST /api/user/login
Content-Type: application/json

Request Body:
{
  "userAccount": "testuser",
  "userPassword": "123456"
}

Response:
{
  "code": 0,
  "data": {
    "id": "...",
    "userAccount": "testuser",
    "role": "USER",
    ...
  },
  "description": "登录成功"
}
```

#### 1.3 获取当前用户
```
GET /api/user/current

Response:
{
  "code": 0,
  "data": {
    "id": "...",
    "userAccount": "testuser",
    ...
  },
  "description": "success"
}
```

#### 1.4 获取用户列表（管理员）
```
GET /api/user/list?pageNum=1&pageSize=10

Response:
{
  "code": 0,
  "data": {
    "content": [...],
    "totalPages": 1,
    "totalElements": 10
  }
}
```

#### 1.5 删除用户（管理员）
```
POST /api/user/delete
Content-Type: application/json

Request Body:
{
  "id": "user_id"
}

Response:
{
  "code": 0,
  "data": true,
  "description": "删除成功"
}
```

### 2. 课程模块

#### 2.1 获取课程结构
```
GET /api/course/structure

Response:
{
  "code": 0,
  "data": {
    "id": "...",
    "courseName": "电磁场课程",
    "description": "...",
    "chapters": [
      {
        "id": "chapter-1",
        "title": "第1章 矢量分析",
        "sections": [...]
      }
    ]
  }
}
```

### 3. 资料模块

#### 3.1 上传资料
```
POST /api/material/upload
Content-Type: application/json

Request Body:
{
  "fileName": "电磁场基础.pdf",
  "fileType": "pdf",
  "filePath": "/uploads/electromagnetic_basics.pdf",
  "fileSize": 2048576,
  "chapterTitle": "第1章 矢量分析",
  "lessonTitle": "1.1 矢量代数",
  "description": "电磁场基础知识文档",
  "md5": "abc123...",
  "isPublic": true
}

Response:
{
  "code": 0,
  "data": {
    "id": "...",
    "fileName": "电磁场基础.pdf",
    ...
  },
  "description": "上传成功"
}
```

#### 3.2 获取资料列表
```
GET /api/material/list?pageNum=1&pageSize=10

Response:
{
  "code": 0,
  "data": {
    "content": [...],
    "totalPages": 1,
    "totalElements": 10
  }
}
```

#### 3.3 根据章节获取资料
```
GET /api/material/by-chapter?chapterTitle=第1章 矢量分析&lessonTitle=1.1 矢量代数

Response:
{
  "code": 0,
  "data": [
    {
      "id": "...",
      "fileName": "电磁场基础.pdf",
      ...
    }
  ]
}
```

#### 3.4 删除资料
```
POST /api/material/delete?id=material_id

Response:
{
  "code": 0,
  "data": true,
  "description": "删除成功"
}
```

## 📊 数据库设计

### Collection 结构

#### users（用户表）
```json
{
  "_id": ObjectId("..."),
  "userAccount": "testuser",
  "userPassword": "encrypted_password",
  "planetCode": "123456",
  "role": "USER",
  "createTime": ISODate("2024-01-01T00:00:00Z"),
  "updateTime": ISODate("2024-01-01T00:00:00Z"),
  "userStatus": 0,
  "userName": "测试用户",
  "userAvatar": "http://...",
  "email": "test@example.com",
  "phone": "13800138000"
}
```

#### course_structures（课程结构表）
```json
{
  "_id": ObjectId("..."),
  "courseName": "电磁场课程",
  "description": "...",
  "coverUrl": "",
  "status": "PUBLISHED",
  "chapters": [
    {
      "id": "chapter-1",
      "title": "第1章 矢量分析",
      "color": "#00B894",
      "sortOrder": 1,
      "sections": [
        {
          "id": "1-1",
          "title": "1.1 矢量代数",
          "sortOrder": 1,
          "subsections": [...]
        }
      ]
    }
  ],
  "createTime": ISODate("2024-01-01T00:00:00Z"),
  "updateTime": ISODate("2024-01-01T00:00:00Z")
}
```

#### materials（资料表）
```json
{
  "_id": ObjectId("..."),
  "fileName": "电磁场基础.pdf",
  "fileType": "pdf",
  "materialType": "PDF",
  "filePath": "/uploads/electromagnetic_basics.pdf",
  "fileSize": 2048576,
  "chapterTitle": "第1章 矢量分析",
  "lessonTitle": "1.1 矢量代数",
  "path": "第1章 矢量分析/1.1 矢量代数",
  "uploaderId": "user_id",
  "uploaderName": "测试用户",
  "createTime": ISODate("2024-01-01T00:00:00Z"),
  "downloadCount": 10,
  "status": 0,
  "description": "电磁场基础知识文档",
  "md5": "abc123...",
  "isPublic": true
}
```

## 🔒 安全说明

### 密码加密
- 使用 MD5 + 盐值加密存储密码
- 建议升级为 BCrypt 加密方式

### 跨域配置
- 已配置 CORS，允许所有源访问
- 支持携带 Cookie

### 权限控制
- 用户管理接口需要管理员权限
- 资料删除需要是上传者或管理员

## 🐛 常见问题

### 1. MongoDB 连接失败
**问题**：`Failed to connect to MongoDB`

**解决方案**：
- 确保 MongoDB 服务已启动
- 检查 `application.yml` 中的连接配置
- 验证 MongoDB 端口（默认 27017）是否可访问

### 2. 跨域问题
**问题**：前端请求被 CORS 拦截

**解决方案**：
- 检查 `CorsConfig.java` 配置
- 确保前端请求的 Origin 在允许列表中

### 3. Session 失效
**问题**：用户登录后 Session 快速过期

**解决方案**：
- 调整 Session 过期时间
- 考虑使用 JWT 替代 Session

## 📝 开发计划

- [ ] 实现 JWT 认证
- [ ] 添加文件上传接口
- [ ] 实现权限控制（Spring Security）
- [ ] 添加接口文档（Swagger）
- [ ] 实现学习进度跟踪
- [ ] 添加讨论区功能
- [ ] 实现成绩管理

## 👥 联系方式

如有问题或建议，请联系项目维护者。

## 📄 许可证

本项目采用 MIT 许可证。
