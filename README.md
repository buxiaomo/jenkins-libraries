# Jenkins 共享库

这是一个Jenkins共享库项目，提供了用于CI/CD流水线的可重用函数。

## 项目结构

```
jenkins-libraries/
├── README.md                    # 项目文档
├── src/
│   └── org/
│       └── xiaomo/
│           └── Common.groovy    # 通用工具类
└── vars/
    └── BuildDockerImage.groovy  # Docker构建函数
```

## 功能

### Common 工具类

位于 `src/org/xiaomo/Common.groovy`，提供可在多个共享库函数中复用的通用方法：

- `validateAndGet()`: 参数验证和获取
- `buildDockerCommand()`: Docker命令构建（支持智能builder选择）
- `validateFileExists()`: 文件存在性验证
- `validateEnvVar()`: 环境变量验证
- `safeShellExecution()`: 安全的Shell命令执行

### BuildDockerImage

用于构建和推送Docker镜像的Jenkins共享库函数，支持多平台构建、缓存优化和灵活配置。

#### 特性

- ✅ 多平台构建支持（linux/amd64, linux/arm64等）
- ✅ 智能缓存机制，提升构建速度
- ✅ 参数验证和错误处理
- ✅ 自定义构建参数支持
- ✅ 灵活的标签管理
- ✅ 详细的构建日志

#### 使用方法

```groovy
// 基本使用
BuildDockerImage {
    name = 'my-app'  // 必需参数
}

// 完整配置示例
BuildDockerImage {
    host = 'registry.example.com'           // 镜像仓库地址
    project = 'my-project'                  // 项目名称
    name = 'my-app'                         // 应用名称（必需）
    tag = '1.0.0'                          // 镜像标签
    platform = 'linux/amd64,linux/arm64'   // 目标平台
    path = './docker/Dockerfile'           // Dockerfile路径
    enableCache = true                      // 启用缓存（默认true）
    buildArgs = [                          // 构建参数
        'NODE_VERSION=18',
        'APP_ENV=production'
    ]
    progress = 'plain'                     // 构建进度显示模式
}

// 在Jenkins Pipeline中使用环境变量和参数的正确语法
BuildDockerImage {
    host = env.REGISTRY_HOST                // 环境变量（无引号）
    project = env.JOB_NAME                  // 环境变量（无引号）
    name = 'admin'                          // 字符串常量（有引号）
    tag = env.BUILD_NUMBER                  // 环境变量（无引号）
    platform = params.platform             // 参数（无引号）
    path = './Dockerfile'                   // 字符串常量（有引号）
}
```

#### 参数说明

| 参数 | 类型 | 必需 | 默认值 | 说明 |
|------|------|------|--------|---------|
| `host` | String | 否 | `env.REGISTRY_HOST` | 镜像仓库地址 |
| `project` | String | 否 | `env.JOB_NAME` | 项目名称 |
| `name` | String | **是** | - | 应用名称 |
| `tag` | String | 否 | `BUILD_NUMBER` 或 `latest` | 镜像标签 |
| `platform` | String | 否 | `linux/amd64` | 目标平台，当值为 `linux/amd64,linux/arm64` 时自动使用 `multi-platform` builder，否则使用 `default` builder |
| `path` | String | 否 | `./Dockerfile` | Dockerfile文件路径 |
| `enableCache` | Boolean | 否 | `true` | 是否启用构建缓存 |
| `buildArgs` | List | 否 | `[]` | Docker构建参数列表 |
| `progress` | String | 否 | `auto` | 构建进度显示模式（auto/plain/tty） |

#### 环境变量要求

- `BUILDER`: Docker buildx builder名称（必需）
- `REGISTRY_HOST`: 默认镜像仓库地址（可选）
- `JOB_NAME`: 默认项目名称（可选）
- `BUILD_NUMBER`: 默认构建标签（可选）

#### 最佳实践

1. **缓存优化**: 保持 `enableCache = true` 以提升构建速度
2. **多平台构建**: 当 `platform = 'linux/amd64,linux/arm64'` 时自动启用多平台构建模式
3. **标签管理**: 使用语义化版本号作为标签
4. **安全性**: 避免在构建参数中传递敏感信息
5. **Dockerfile位置**: 将Dockerfile放在项目根目录或专门的docker目录中

#### 代码架构

采用模块化设计，将通用功能提取到 `Common` 类中：

- **参数验证**: 使用 `Common.validateAndGet()` 进行统一的参数验证
- **环境检查**: 通过 `Common.validateEnvVar()` 验证环境变量
- **文件验证**: 使用 `Common.validateFileExists()` 确认文件存在
- **命令构建**: `Common.buildDockerCommand()` 负责构建复杂的Docker命令，并根据platform自动选择合适的builder
- **安全执行**: `Common.safeShellExecution()` 提供统一的错误处理和日志记录

#### 错误处理

函数包含完善的错误处理机制：

- 参数验证：检查必需参数是否提供
- 环境检查：验证Docker buildx环境是否正确配置
- 文件验证：确认Dockerfile文件存在
- 构建监控：捕获并报告构建过程中的错误

## 安装和配置

1. 将此库添加到Jenkins的全局库配置中
2. 在Jenkinsfile中引用：

```groovy
@Library('your-shared-library') _

pipeline {
    agent any
    stages {
        stage('Build Docker Image') {
            steps {
                BuildDockerImage {
                    name = 'my-application'
                    tag = env.BUILD_NUMBER
                }
            }
        }
    }
}
```

## 语法验证工具

### ValidatePipelineSyntax 函数

为了帮助开发者快速识别和修复常见的语法错误，我们提供了专门的验证工具：

```groovy
// 在Pipeline中使用语法验证
ValidatePipelineSyntax {
    checkEnvironmentVariables = true
    checkParameters = true
    suggestFixes = true
}
```

该工具会：
- 检查常用环境变量是否存在
- 验证Pipeline参数配置
- 提供语法修复建议
- 显示正确的使用示例

### 自动语法检查

`BuildDockerImage`函数现在包含自动语法验证，会在执行前检查配置并提供详细的错误信息和修复建议。

## 故障排除

### 常见语法错误

**错误**: `Expected a step @ line X, column Y`

**原因**: 在BuildDockerImage闭包中使用了错误的语法

**解决方案**:
```groovy
// ❌ 错误写法 - 环境变量使用了引号或${}语法
BuildDockerImage {
    host = '${env.REGISTRY_HOST}'     // 错误：字符串中的变量
    tag = ${BUILD_NUMBER}             // 错误：缺少引号
    name = admin                      // 错误：字符串需要引号
}

// ✅ 正确写法
BuildDockerImage {
    host = env.REGISTRY_HOST          // 正确：直接引用环境变量
    tag = env.BUILD_NUMBER            // 正确：直接引用环境变量
    name = 'my-app'                   // 正确：字符串常量用引号
}
```

### 语法规则

1. **环境变量**: 使用 `env.VARIABLE_NAME`（无引号）
2. **参数**: 使用 `params.parameter_name`（无引号）
3. **字符串常量**: 使用单引号或双引号包围
4. **布尔值**: 直接使用 `true` 或 `false`
5. **数组**: 使用方括号 `['item1', 'item2']`

### 错误检测和修复

`BuildDockerImage`和`ValidatePipelineSyntax`函数会自动检测以下常见错误：

1. **${} 语法错误**: 自动检测并提示正确用法
2. **BUILD_NUMBER 错误**: 检测裸露的`BUILD_NUMBER`并建议使用`env.BUILD_NUMBER`
3. **字符串引号缺失**: 检测未加引号的字符串常量
4. **环境变量和参数验证**: 检查必需的环境变量是否存在

### 调试技巧

1. 使用`ValidatePipelineSyntax`函数进行预检查
2. 检查所有字符串是否正确加引号
3. 确认环境变量使用`env.`前缀
4. 确认参数使用`params.`前缀
5. 避免使用`${}`语法在闭包参数中
6. 注意`BUILD_NUMBER`应写作`env.BUILD_NUMBER`
7. 使用Jenkins的Pipeline语法生成器验证语法

### 完整示例

查看 <mcfile name="correct-pipeline-example.groovy" path="/Users/peng.liu/workspace/src/jenkins-libraries/examples/correct-pipeline-example.groovy"></mcfile> 文件，了解完整的正确使用示例，包括：

- 正确的参数和环境变量语法
- ValidatePipelineSyntax的使用
- BuildDockerImage的最佳实践
- 常见错误对比和修复方法

## 扩展开发

### 添加新的共享库函数

1. 在 `vars/` 目录下创建新的 `.groovy` 文件
2. 导入 `Common` 类：`import org.xiaomo.Common`
3. 使用 `Common` 类中的通用方法进行参数验证和错误处理
4. 遵循现有的代码风格和文档规范

### 扩展Common类

在 `src/org/xiaomo/Common.groovy` 中添加新的静态方法，确保：
- 方法是静态的（`static`）
- 包含完整的JavaDoc注释
- 遵循统一的错误处理模式
- 可在多个共享库函数中复用

## 贡献

欢迎提交Issue和Pull Request来改进这个共享库。

## 许可证

本项目采用MIT许可证。