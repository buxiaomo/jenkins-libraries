# Jenkins 共享库

一个专为 Jenkins CI/CD 流水线设计的共享库，提供 Docker 镜像构建和推送功能。

## 项目结构

```
jenkins-libraries/
├── README.md                    # 项目文档
├── src/
│   └── org/
│       └── xiaomo/
│           └── Common.groovy    # 通用工具类（预留）
└── vars/
    └── BuildDockerImage.groovy  # Docker构建函数
```

## 核心功能

### BuildDockerImage

用于构建和推送 Docker 镜像的 Jenkins 共享库函数，支持多平台构建和智能缓存优化。

#### 主要特性

- ✅ **多平台构建支持** - 支持 linux/amd64、linux/arm64 等多种架构
- ✅ **智能 Builder 选择** - 根据平台自动选择合适的 buildx builder
- ✅ **缓存优化** - 多平台构建时自动启用 registry 缓存
- ✅ **灵活配置** - 支持自定义构建参数、标签和路径
- ✅ **环境变量集成** - 无缝集成 Jenkins 环境变量和参数

## 快速开始

### 1. 安装配置

在 Jenkins 中添加此共享库：

1. 进入 Jenkins 管理 → 系统配置 → Global Pipeline Libraries
2. 添加新库，配置 Git 仓库地址
3. 在 Jenkinsfile 中引用库

### 2. 基本使用

```groovy
@Library('your-shared-library') _

pipeline {
    agent any
    stages {
        stage('Build Docker Image') {
            steps {
                script {
                    BuildDockerImage(this) {
                        name = 'my-app'  // 必需参数
                    }
                }
            }
        }
    }
}
```

### 3. 完整配置示例

```groovy
BuildDockerImage(this) {
    host = 'registry.example.com'           // 镜像仓库地址
    project = 'my-project'                  // 项目名称
    name = 'my-app'                         // 应用名称（必需）
    tag = '1.0.0'                          // 镜像标签
    platform = 'linux/amd64,linux/arm64'   // 目标平台
    path = './docker/Dockerfile'           // Dockerfile路径
    buildArgs = [                          // 构建参数
        'NODE_VERSION=18',
        'APP_ENV=production'
    ]
    progress = 'plain'                     // 构建进度显示
}
```

### 4. 使用环境变量

```groovy
BuildDockerImage(this) {
    host = env.REGISTRY_HOST                // 环境变量
    project = env.JOB_NAME                  // 环境变量
    name = 'admin'                          // 字符串常量
    tag = env.BUILD_NUMBER                  // 环境变量
    platform = params.platform             // Pipeline 参数
}
```

## 参数说明

| 参数 | 类型 | 必需 | 默认值 | 说明 |
|------|------|------|--------|---------|
| `host` | String | 否 | `env.REGISTRY_HOST` | 镜像仓库地址 |
| `project` | String | 否 | `env.JOB_NAME` | 项目名称 |
| `name` | String | **是** | - | 应用名称 |
| `tag` | String | 否 | `env.BUILD_NUMBER` | 镜像标签 |
| `platform` | String | 否 | - | 目标平台 |
| `path` | String | 否 | - | Dockerfile 路径 |
| `buildArgs` | List | 否 | `[]` | Docker 构建参数 |
| `progress` | String | 否 | `auto` | 构建进度显示模式 |

## 核心机制

### 智能 Builder 选择

函数会根据 `platform` 参数自动选择合适的 Docker buildx builder：

- **多平台构建**：当 `platform = "linux/amd64,linux/arm64"` 时
  - 使用 `multi-platform` builder
  - 自动启用 registry 缓存优化
  - 支持跨架构构建

- **单平台构建**：其他情况
  - 使用 `default` builder
  - 禁用缓存以提高构建速度
  - 适合快速迭代开发

### 缓存策略

```groovy
// 多平台构建时的缓存配置
def cacheRef = "${host}/${project}/${name}:buildcache"
command << "--cache-to type=registry,ref=${cacheRef},mode=max"
command << "--cache-from type=registry,ref=${cacheRef}"
```

### 镜像标签

自动生成两个标签：
- 指定标签：`${host}/${project}/${name}:${tag}`
- 最新标签：`${host}/${project}/${name}:latest`

## 使用场景

### 场景 1：单架构快速构建

```groovy
// 适用于开发环境快速迭代
BuildDockerImage(this) {
    name = 'my-app'
    platform = 'linux/amd64'
    // 使用 default builder，无缓存，构建速度快
}
```

### 场景 2：多架构生产构建

```groovy
// 适用于生产环境多架构部署
BuildDockerImage(this) {
    name = 'my-app'
    platform = 'linux/amd64,linux/arm64'
    tag = env.BUILD_NUMBER
    // 使用 multi-platform builder，启用缓存优化
}
```

### 场景 3：自定义构建参数

```groovy
BuildDockerImage(this) {
    name = 'my-app'
    buildArgs = [
        'NODE_VERSION=18.17.0',
        'APP_ENV=production',
        'BUILD_DATE=' + new Date().format('yyyy-MM-dd')
    ]
}
```

## 环境要求

### Docker Buildx

确保 Jenkins 环境中已安装并配置 Docker Buildx：

```bash
# 检查 buildx 是否可用
docker buildx version

# 创建多平台 builder（如需要）
docker buildx create --name multi-platform --use
```

### 环境变量

建议在 Jenkins 中配置以下环境变量：

- `REGISTRY_HOST`：镜像仓库地址
- `JOB_NAME`：项目名称（Jenkins 自动设置）
- `BUILD_NUMBER`：构建编号（Jenkins 自动设置）

## 最佳实践

### 1. 构建策略

- **开发环境**：使用单平台构建，关闭缓存，提高构建速度
- **生产环境**：使用多平台构建，启用缓存，确保兼容性

### 2. 标签管理

```groovy
// 使用语义化版本
tag = "v${env.BUILD_NUMBER}"

// 使用 Git 提交哈希
tag = env.GIT_COMMIT?.take(8) ?: 'latest'

// 使用分支名称
tag = env.BRANCH_NAME?.replaceAll('/', '-') ?: 'main'
```

### 3. 安全考虑

- 避免在 `buildArgs` 中传递敏感信息
- 使用 Jenkins 凭据管理敏感数据
- 定期清理构建缓存

### 4. 性能优化

- 优化 Dockerfile 层级结构
- 使用 `.dockerignore` 减少构建上下文
- 合理使用多阶段构建

## 故障排除

### 常见问题

**问题 1：Builder 不存在**
```
ERROR: failed to find the builder "multi-platform"
```

**解决方案：**
```bash
docker buildx create --name multi-platform --use
```

**问题 2：平台不支持**
```
ERROR: multiple platforms feature is currently not supported
```

**解决方案：**
- 检查 Docker 版本是否支持 buildx
- 确认 builder 是否正确配置

**问题 3：缓存访问失败**
```
ERROR: failed to solve: failed to push cache
```

**解决方案：**
- 检查镜像仓库访问权限
- 确认网络连接正常

### 调试技巧

1. **查看生成的命令**：函数会输出完整的 Docker 命令
2. **检查 builder 状态**：`docker buildx ls`
3. **验证平台支持**：`docker buildx inspect`
4. **测试仓库连接**：`docker login <registry>`

## 扩展开发

### 添加新功能

1. 在 `vars/` 目录创建新的 `.groovy` 文件
2. 参考 `BuildDockerImage.groovy` 的结构
3. 使用统一的参数处理和错误处理模式

### 自定义配置

可以根据需要修改默认行为：

```groovy
// 自定义 builder 选择逻辑
def isMultiPlatform = (platform == "linux/amd64,linux/arm64")
def builderName = isMultiPlatform ? "my-custom-builder" : "default"

// 自定义缓存策略
if (enableCache) {
    def cacheRef = "${host}/${project}/${name}:buildcache"
    // 添加自定义缓存配置
}
```

## 版本历史

### v1.0.0
- 初始版本
- 支持基本的 Docker 镜像构建和推送
- 智能 builder 选择机制
- 多平台构建支持
- Registry 缓存优化

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这个共享库。

## 许可证

本项目采用 MIT 许可证。