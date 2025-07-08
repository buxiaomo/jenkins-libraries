#!/usr/bin/env groovy

import org.xiaomo.Common
import org.xiaomo.ConfigValidator
import org.xiaomo.ErrorRecovery

/**
 * BuildDockerImage - Jenkins共享库函数，用于构建和推送Docker镜像
 * 
 * 使用示例:
 * BuildDockerImage {
 *     host = '192.168.1.1:5000'          // 镜像仓库地址
 *     project = 'projectName'             // 项目名称
 *     name = 'appname'                    // 应用名称（必需）
 *     tag = '1.0.0'                       // 镜像标签
 *     platform = 'linux/amd64,linux/arm64' // 目标平台
 *     path = './Dockerfile'               // Dockerfile路径
 *     enableCache = true                  // 是否启用缓存
 *     buildArgs = ['ARG1=value1']         // 构建参数
 *     progress = 'plain'                  // 构建进度显示模式
 * }
 *
 * 在Jenkins Pipeline中使用环境变量的正确语法:
 * BuildDockerImage {
 *     host = env.REGISTRY_HOST            // 使用环境变量（无引号）
 *     project = env.JOB_NAME              // 使用环境变量（无引号）
 *     name = 'admin'                      // 字符串常量（有引号）
 *     tag = env.BUILD_NUMBER              // 使用环境变量（无引号）
 *     platform = params.platform         // 使用参数（无引号）
 *     path = './Dockerfile'               // 字符串常量（有引号）
 * }
 */

def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    
    def recoveryContext = null
    
    try {
        // 创建错误恢复上下文
        recoveryContext = ErrorRecovery.createRecoveryContext(this, config, env)
        
        // 使用错误恢复机制进行配置验证
        def validationResults = ErrorRecovery.safeExecute(this, "配置验证", {
            return ConfigValidator.validateBuildDockerImageConfig(config, this, env)
        })
        
        // 安全地获取配置参数
        def host = ErrorRecovery.safeGetEnvVar(this, env, 'REGISTRY_HOST', config.get('host'), false)
        def project = ErrorRecovery.safeGetEnvVar(this, env, 'JOB_NAME', config.get('project'), false)
        def name = config.get('name', null)
        def tag = ErrorRecovery.safeGetEnvVar(this, env, 'BUILD_NUMBER', config.get('tag', 'latest'), false)
        def platform = config.get('platform', 'linux/amd64')
        def path = config.get('path', './Dockerfile')
        def enableCache = config.get('enableCache', true)
        def buildArgs = config.get('buildArgs', [])
        def progress = config.get('progress', 'auto')
        
        // 验证必需参数
        if (!host) {
            error("镜像仓库地址未设置。请设置 REGISTRY_HOST 环境变量或在配置中指定 host 参数。")
        }
        if (!project) {
            error("项目名称未设置。请设置 JOB_NAME 环境变量或在配置中指定 project 参数。")
        }
        if (!name) {
            error("应用名称未设置。请在配置中指定 name 参数。")
        }
        
        // 验证Dockerfile是否存在
        ErrorRecovery.safeExecute(this, "Dockerfile验证", {
            Common.validateFileExists(this, path, 'Dockerfile')
        })
        
        // 生成配置报告
        def finalConfig = [
            host: host,
            project: project,
            name: name,
            tag: tag,
            platform: platform,
            path: path,
            enableCache: enableCache,
            buildArgs: buildArgs,
            progress: progress
        ]
        ConfigValidator.generateConfigReport(finalConfig, this)

        // 构建Docker命令
        def command = []
        
        // 判断是否为多平台构建
        def isMultiPlatform = (platform == "linux/amd64,linux/arm64")
        def builderName = isMultiPlatform ? "multi-platform" : "default"
        
        // 基础命令
        command << "docker buildx --builder ${builderName} build"
        command << "--progress=${progress}"
        command << "--platform=${platform}"
        
        // 构建参数
        buildArgs.each { arg ->
            command << "--build-arg ${arg}"
        }
        
        // 镜像标签
        command << "-t ${host}/${project}/${name}:${tag}"
        command << "-t ${host}/${project}/${name}:latest"
        
        // 缓存配置
        if (enableCache) {
            def cacheRef = "${host}/${project}/${name}:buildcache"
            command << "--cache-to type=registry,ref=${cacheRef},mode=max"
            command << "--cache-from type=registry,ref=${cacheRef}"
        }
        
        // 推送和文件路径
        command << "--push ."
        command << "-f ${path}"
        
        def cmd = command.join(" ")
         
         echo "🐳 开始构建Docker镜像..."
         echo "📋 构建命令: ${cmd}"
        
        // 使用错误恢复机制执行Docker构建
        sh cmd
        // ErrorRecovery.safeDockerExecution(this, dockerCommand, "Docker镜像构建")
        
    } catch (Exception e) {
        // 使用智能错误分析
        ErrorRecovery.analyzeAndSuggest(this, e, recoveryContext)
        
        // 重新抛出异常
        throw e
    }
}

return this
