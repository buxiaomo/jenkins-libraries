#!/usr/bin/env groovy

/**
 * 正确的Jenkins Pipeline示例
 * 演示如何正确使用BuildDockerImage和ValidatePipelineSyntax函数
 */

pipeline {
    agent any
    
    parameters {
        choice(
            name: 'platform',
            choices: ['linux/amd64', 'linux/amd64,linux/arm64'],
            description: '目标平台架构'
        )
        booleanParam(
            name: 'enableCache',
            defaultValue: true,
            description: '是否启用Docker构建缓存'
        )
        string(
            name: 'customTag',
            defaultValue: '',
            description: '自定义标签（可选）'
        )
    }
    
    environment {
        REGISTRY_HOST = 'registry.example.com'
        BUILDER = 'multi-platform'
    }
    
    stages {
        stage('Validate Pipeline Syntax') {
            steps {
                script {
                    // 使用语法验证工具检查Pipeline配置
                    ValidatePipelineSyntax {
                        checkEnvironmentVariables = true
                        checkParameters = true
                        suggestFixes = true
                    }
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    // ✅ 正确的BuildDockerImage使用方式
                    BuildDockerImage {
                        host = env.REGISTRY_HOST        // 环境变量（不加引号）
                        project = env.JOB_NAME          // 环境变量（不加引号）
                        name = 'my-application'         // 字符串常量（加引号）
                        tag = params.customTag ?: env.BUILD_NUMBER  // 参数或环境变量
                        platform = params.platform     // 参数（不加引号）
                        path = './Dockerfile'           // 字符串常量（加引号）
                        enableCache = params.enableCache // 参数（布尔值）
                        buildArgs = [                   // 数组（字符串项加引号）
                            'NODE_ENV=production',
                            'API_VERSION=v1'
                        ]
                        labels = [                      // 数组（字符串项加引号）
                            'maintainer=devops@example.com',
                            'version=' + env.BUILD_NUMBER
                        ]
                    }
                }
            }
        }
        
        stage('Verify Build') {
            steps {
                script {
                    echo "Docker镜像构建完成"
                    echo "镜像标签: ${env.REGISTRY_HOST}/${env.JOB_NAME}/my-application:${params.customTag ?: env.BUILD_NUMBER}"
                    echo "构建平台: ${params.platform}"
                }
            }
        }
    }
    
    post {
        always {
            echo '清理工作空间...'
            cleanWs()
        }
        success {
            echo '✅ Pipeline执行成功！'
        }
        failure {
            echo '❌ Pipeline执行失败，请检查日志。'
        }
    }
}

/* 
=== 常见错误示例（请避免） ===

❌ 错误写法:
BuildDockerImage {
    host = ${env.REGISTRY_HOST}     // 错误：使用${}
    project = ${env.JOB_NAME}       // 错误：使用${}
    name = admin                    // 错误：字符串未加引号
    tag = BUILD_NUMBER              // 错误：应使用env.BUILD_NUMBER
    tag = ${BUILD_NUMBER}           // 错误：使用${}
    platform = ${params.platform}   // 错误：使用${}
}

✅ 正确写法:
BuildDockerImage {
    host = env.REGISTRY_HOST        // 正确：直接使用env
    project = env.JOB_NAME          // 正确：直接使用env
    name = 'admin'                  // 正确：字符串加引号
    tag = env.BUILD_NUMBER          // 正确：直接使用env
    platform = params.platform     // 正确：直接使用params
}

=== 语法规则总结 ===
1. 环境变量: env.VARIABLE_NAME（不加引号）
2. 参数: params.parameter_name（不加引号）
3. 字符串常量: 'value' 或 "value"（加引号）
4. 布尔值: true 或 false（不加引号）
5. 数组: ['item1', 'item2']（字符串项加引号）
6. 避免在闭包参数中使用${}语法
*/