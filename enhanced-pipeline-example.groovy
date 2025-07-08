#!/usr/bin/env groovy

/**
 * 增强版 Jenkins Pipeline 示例
 * 展示新的错误恢复和配置验证功能
 */

@Library('jenkins-libraries') _

pipeline {
    agent any
    
    parameters {
        choice(
            name: 'PLATFORM',
            choices: ['linux/amd64', 'linux/arm64', 'linux/amd64,linux/arm64'],
            description: '目标平台'
        )
        booleanParam(
            name: 'ENABLE_CACHE',
            defaultValue: true,
            description: '启用Docker构建缓存'
        )
        string(
            name: 'CUSTOM_TAG',
            defaultValue: '',
            description: '自定义镜像标签（可选）'
        )
    }
    
    environment {
        // 必需的环境变量
        REGISTRY_HOST = 'registry.example.com'
        
        // 可选的环境变量（如果未设置，将使用默认值）
        DOCKER_BUILDER = 'multiplatform-builder'
        BUILD_ARGS = 'VERSION=1.0.0,ENVIRONMENT=production'
    }
    
    stages {
        stage('环境检查') {
            steps {
                script {
                    echo "🔍 检查构建环境..."
                    echo "Jenkins版本: ${env.JENKINS_VERSION}"
                    echo "节点标签: ${env.NODE_LABELS}"
                    echo "工作空间: ${env.WORKSPACE}"
                    echo "构建编号: ${env.BUILD_NUMBER}"
                    echo "任务名称: ${env.JOB_NAME}"
                }
            }
        }
        
        stage('Pipeline语法验证') {
            steps {
                script {
                    echo "📝 验证Pipeline语法..."
                    
                    // 使用语法验证工具
                    ValidatePipelineSyntax {
                        // 验证配置
                        checkSyntax = true
                        validateEnvVars = true
                        checkDockerfile = true
                    }
                }
            }
        }
        
        stage('Docker镜像构建 - 基础配置') {
            steps {
                script {
                    echo "🐳 构建Docker镜像（基础配置）..."
                    
                    // 基础配置示例 - 依赖环境变量
                    BuildDockerImage {
                        name = 'my-app'
                        platform = params.PLATFORM
                        enableCache = params.ENABLE_CACHE
                        
                        // 如果提供了自定义标签，使用它；否则使用BUILD_NUMBER
                        if (params.CUSTOM_TAG) {
                            tag = params.CUSTOM_TAG
                        }
                        
                        // 构建参数
                        buildArgs = [
                            'VERSION=' + env.BUILD_NUMBER,
                            'BUILD_DATE=' + new Date().format('yyyy-MM-dd'),
                            'GIT_COMMIT=' + (env.GIT_COMMIT ?: 'unknown')
                        ]
                    }
                }
            }
        }
        
        stage('Docker镜像构建 - 完整配置') {
            steps {
                script {
                    echo "🐳 构建Docker镜像（完整配置）..."
                    
                    // 完整配置示例 - 明确指定所有参数
                    BuildDockerImage {
                        host = env.REGISTRY_HOST
                        project = env.JOB_NAME.split('/')[0] // 从JOB_NAME提取项目名
                        name = 'my-enhanced-app'
                        tag = "v${env.BUILD_NUMBER}-${params.PLATFORM.replace('/', '-')}"
                        platform = params.PLATFORM
                        path = './docker/Dockerfile'
                        enableCache = params.ENABLE_CACHE
                        progress = 'plain'
                        
                        // 高级构建参数
                        buildArgs = [
                            'APP_VERSION=' + env.BUILD_NUMBER,
                            'BUILD_TIMESTAMP=' + System.currentTimeMillis(),
                            'PLATFORM=' + params.PLATFORM,
                            'JENKINS_URL=' + env.JENKINS_URL,
                            'BUILD_URL=' + env.BUILD_URL
                        ]
                    }
                }
            }
        }
        
        stage('多平台构建示例') {
            when {
                expression { params.PLATFORM.contains(',') }
            }
            steps {
                script {
                    echo "🌐 执行多平台构建..."
                    
                    // 多平台构建配置
                    BuildDockerImage {
                        name = 'my-multiplatform-app'
                        platform = params.PLATFORM
                        enableCache = true
                        
                        // 多平台构建的特殊配置
                        buildArgs = [
                            'TARGETPLATFORM=${TARGETPLATFORM}',
                            'BUILDPLATFORM=${BUILDPLATFORM}',
                            'VERSION=' + env.BUILD_NUMBER
                        ]
                    }
                }
            }
        }
        
        stage('错误恢复演示') {
            steps {
                script {
                    echo "🔧 演示错误恢复机制..."
                    
                    // 故意使用可能失败的配置来演示错误恢复
                    try {
                        BuildDockerImage {
                            name = 'test-recovery-app'
                            // 故意不设置某些参数来触发错误恢复
                            platform = params.PLATFORM
                            enableCache = false // 禁用缓存可能导致构建时间更长
                        }
                    } catch (Exception e) {
                        echo "⚠️ 捕获到预期的错误，错误恢复机制已激活"
                        echo "错误信息: ${e.getMessage()}"
                        
                        // 在实际使用中，这里可以实现额外的恢复逻辑
                        echo "💡 建议检查环境变量配置和Dockerfile路径"
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                echo "🧹 清理构建环境..."
                
                // 清理Docker构建缓存（可选）
                try {
                    sh 'docker builder prune -f --filter until=24h || true'
                    sh 'docker system prune -f --filter until=24h || true'
                } catch (Exception e) {
                    echo "⚠️ Docker清理失败: ${e.getMessage()}"
                }
            }
        }
        
        success {
            echo "✅ Pipeline执行成功！"
            echo "📊 构建统计:"
            echo "   - 构建编号: ${env.BUILD_NUMBER}"
            echo "   - 构建时长: ${currentBuild.durationString}"
            echo "   - 目标平台: ${params.PLATFORM}"
            echo "   - 缓存状态: ${params.ENABLE_CACHE ? '启用' : '禁用'}"
        }
        
        failure {
            echo "❌ Pipeline执行失败！"
            echo "🔍 故障排除建议:"
            echo "   1. 检查环境变量配置"
            echo "   2. 验证Dockerfile路径和语法"
            echo "   3. 确认Docker服务状态"
            echo "   4. 检查网络连接和镜像仓库访问"
            echo "   5. 查看完整的构建日志"
            
            // 收集调试信息
            script {
                try {
                    sh 'env | grep -E "(REGISTRY|JOB|BUILD|DOCKER)" || true'
                    sh 'docker info || true'
                    sh 'df -h || true'
                } catch (Exception e) {
                    echo "⚠️ 无法收集调试信息: ${e.getMessage()}"
                }
            }
        }
        
        unstable {
            echo "⚠️ Pipeline执行不稳定"
            echo "建议检查构建警告和配置"
        }
    }
}

/**
 * 常见错误示例和修复方法
 */

/*
// ❌ 错误示例 1: 使用裸露的环境变量
BuildDockerImage {
    name = 'my-app'
    tag = BUILD_NUMBER  // 错误：应该使用 env.BUILD_NUMBER
    host = REGISTRY_HOST  // 错误：应该使用 env.REGISTRY_HOST
}

// ✅ 正确示例 1: 使用正确的环境变量语法
BuildDockerImage {
    name = 'my-app'
    tag = env.BUILD_NUMBER  // 正确
    host = env.REGISTRY_HOST  // 正确
}

// ❌ 错误示例 2: 使用 ${} 语法
BuildDockerImage {
    name = 'my-app'
    tag = '${BUILD_NUMBER}'  // 错误：在Groovy中不需要 ${}
}

// ✅ 正确示例 2: 直接使用变量
BuildDockerImage {
    name = 'my-app'
    tag = env.BUILD_NUMBER  // 正确
}

// ❌ 错误示例 3: 缺少必需参数
BuildDockerImage {
    // 错误：缺少 name 参数
    platform = 'linux/amd64'
}

// ✅ 正确示例 3: 提供所有必需参数
BuildDockerImage {
    name = 'my-app'  // 必需
    platform = 'linux/amd64'
}
*/

/**
 * 环境变量配置建议
 */

/*
在Jenkins中设置以下环境变量：

必需的环境变量：
- REGISTRY_HOST: 镜像仓库地址（如：registry.example.com）

可选的环境变量：
- JOB_NAME: 任务名称（Jenkins自动设置）
- BUILD_NUMBER: 构建编号（Jenkins自动设置）
- DOCKER_BUILDER: Docker构建器名称
- BUILD_ARGS: 构建参数

在Pipeline中设置：
environment {
    REGISTRY_HOST = 'your-registry.com'
    DOCKER_BUILDER = 'multiplatform-builder'
}

或在Jenkins全局配置中设置。
*/