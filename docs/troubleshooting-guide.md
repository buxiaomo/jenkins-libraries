# Jenkins 共享库故障排除指南

## 常见错误及解决方案

### 1. NullPointerException: Cannot get property 'REGISTRY_HOST' on null object

**错误描述：**
```
java.lang.NullPointerException: Cannot get property 'REGISTRY_HOST' on null object
```

**原因分析：**
- Jenkins Pipeline 中 `env` 对象为 null
- 环境变量未正确设置
- 在错误的上下文中访问环境变量

**解决方案：**

1. **确保在正确的 Pipeline 上下文中使用**
```groovy
pipeline {
    agent any
    
    environment {
        REGISTRY_HOST = 'your-registry.com'
        BUILDER = 'multi-platform'
    }
    
    stages {
        stage('Build') {
            steps {
                script {
                    BuildDockerImage {
                        host = env.REGISTRY_HOST  // 正确：在 pipeline 上下文中
                        // ... 其他配置
                    }
                }
            }
        }
    }
}
```

2. **使用显式配置避免依赖环境变量**
```groovy
BuildDockerImage {
    host = 'your-registry.com'        // 显式指定，不依赖环境变量
    project = 'your-project'
    name = 'your-app'
    tag = '1.0.0'
    // ... 其他配置
}
```

3. **检查环境变量是否正确设置**
```groovy
stage('Validate Environment') {
    steps {
        script {
            ValidatePipelineSyntax {
                checkEnvironmentVariables = true
                checkParameters = true
                suggestFixes = true
            }
        }
    }
}
```

### 2. Expected a step 编译错误

**错误描述：**
```
org.codehaus.groovy.control.MultipleCompilationErrorsException: startup failed:
Expected a step
```

**常见错误语法：**
```groovy
// ❌ 错误写法
BuildDockerImage {
    host = ${env.REGISTRY_HOST}     // 错误：使用 ${}
    tag = BUILD_NUMBER              // 错误：缺少 env 前缀
    name = admin                    // 错误：字符串未加引号
}
```

**正确语法：**
```groovy
// ✅ 正确写法
BuildDockerImage {
    host = env.REGISTRY_HOST        // 正确：直接使用 env
    tag = env.BUILD_NUMBER          // 正确：使用 env 前缀
    name = 'admin'                  // 正确：字符串加引号
}
```

### 3. 文件不存在错误

**错误描述：**
```
Dockerfile不存在: ./Dockerfile
```

**解决方案：**

1. **确保 Dockerfile 存在于正确路径**
```groovy
BuildDockerImage {
    path = './docker/Dockerfile'    // 指定正确的 Dockerfile 路径
    // ... 其他配置
}
```

2. **使用相对于工作目录的路径**
```groovy
stage('Build') {
    steps {
        dir('your-service-directory') {  // 切换到正确的目录
            BuildDockerImage {
                path = './Dockerfile'
                // ... 其他配置
            }
        }
    }
}
```

### 4. Docker 构建失败

**常见原因及解决方案：**

1. **Builder 不存在**
```bash
# 创建 multi-platform builder
docker buildx create --name multi-platform --use
```

2. **权限问题**
```groovy
// 确保 Jenkins 用户有 Docker 权限
stage('Setup') {
    steps {
        sh 'docker info'  // 验证 Docker 访问权限
    }
}
```

3. **网络问题**
```groovy
BuildDockerImage {
    enableCache = false  // 禁用缓存避免网络问题
    // ... 其他配置
}
```

## 调试技巧

### 1. 启用详细日志
```groovy
BuildDockerImage {
    progress = 'plain'  // 显示详细构建日志
    // ... 其他配置
}
```

### 2. 使用语法验证工具
```groovy
stage('Validate') {
    steps {
        script {
            ValidatePipelineSyntax {
                checkEnvironmentVariables = true
                checkParameters = true
                suggestFixes = true
            }
        }
    }
}
```

### 3. 分步调试
```groovy
stage('Debug Info') {
    steps {
        script {
            echo "Registry Host: ${env.REGISTRY_HOST ?: 'NOT SET'}"
            echo "Job Name: ${env.JOB_NAME ?: 'NOT SET'}"
            echo "Build Number: ${env.BUILD_NUMBER ?: 'NOT SET'}"
            echo "Current Directory: ${pwd()}"
            sh 'ls -la'  // 列出当前目录文件
        }
    }
}
```

### 4. 环境变量检查
```groovy
stage('Environment Check') {
    steps {
        script {
            if (!env.REGISTRY_HOST) {
                error('REGISTRY_HOST 环境变量未设置')
            }
            if (!fileExists('./Dockerfile')) {
                error('Dockerfile 不存在于当前目录')
            }
        }
    }
}
```

## 最佳实践

### 1. 错误处理
```groovy
stage('Build Docker Image') {
    steps {
        script {
            try {
                BuildDockerImage {
                    // ... 配置
                }
            } catch (Exception e) {
                echo "构建失败: ${e.getMessage()}"
                currentBuild.result = 'FAILURE'
                throw e
            }
        }
    }
}
```

### 2. 参数验证
```groovy
pipeline {
    parameters {
        string(
            name: 'REGISTRY_HOST',
            defaultValue: 'registry.example.com',
            description: '镜像仓库地址'
        )
        choice(
            name: 'platform',
            choices: ['linux/amd64', 'linux/amd64,linux/arm64'],
            description: '目标平台'
        )
    }
    // ...
}
```

### 3. 条件构建
```groovy
stage('Build Docker Image') {
    when {
        anyOf {
            branch 'main'
            branch 'develop'
            changeRequest()
        }
    }
    steps {
        script {
            BuildDockerImage {
                // ... 配置
            }
        }
    }
}
```

## 获取帮助

如果遇到其他问题：

1. 检查 Jenkins 构建日志
2. 使用 `ValidatePipelineSyntax` 进行预检查
3. 参考完整示例：`examples/correct-pipeline-example.groovy`
4. 确保环境变量正确设置
5. 验证 Dockerfile 和相关文件存在

## 常用命令

```bash
# 检查 Docker 状态
docker info

# 列出 buildx builders
docker buildx ls

# 创建新的 builder
docker buildx create --name multi-platform --use

# 检查镜像
docker images

# 清理构建缓存
docker buildx prune
```