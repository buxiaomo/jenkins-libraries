#!/usr/bin/env groovy

package org.xiaomo

/**
 * Jenkins 共享库通用工具类
 * 提供可在多个共享库函数中复用的通用方法
 */
class Common {
    
    /**
     * 验证并获取配置参数
     * @param config 配置对象
     * @param key 参数键名
     * @param defaultValue 默认值
     * @param description 参数描述
     * @param required 是否为必需参数
     * @return 参数值
     */
    static def validateAndGet(config, key, defaultValue, description, required = false) {
        def value = config.get(key, defaultValue)
        
        if (required && (!value || value.toString().trim().isEmpty())) {
            error("必需参数 '${key}' (${description}) 不能为空")
        }
        
        if (!value) {
            error("参数 '${key}' (${description}) 未设置且无默认值")
        }
        
        return value
    }
    
    /**
     * 构建Docker命令
     * @param host 镜像仓库地址
     * @param project 项目名称
     * @param name 应用名称
     * @param tag 镜像标签
     * @param platform 目标平台
     * @param path Dockerfile路径
     * @param enableCache 是否启用缓存
     * @param buildArgs 构建参数列表
     * @param progress 进度显示模式
     * @return 完整的Docker构建命令
     */
    static def buildDockerCommand(host, project, name, tag, platform, path, enableCache, buildArgs, progress) {
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
        
        return command.join(' \\
    ')
    }
    
    /**
     * 验证文件是否存在
     * @param filePath 文件路径
     * @param description 文件描述
     * @return true如果文件存在
     */
    static def validateFileExists(script, filePath, description = '文件') {
        if (!script.fileExists(filePath)) {
            script.error("${description}不存在: ${filePath}")
        }
        return true
    }
    
    /**
     * 验证环境变量是否设置
     * @param script Jenkins脚本上下文
     * @param envVar 环境变量名
     * @param description 环境变量描述
     * @return true如果环境变量已设置
     */
    static def validateEnvVar(script, envVar, description = '环境变量') {
        if (!script.env[envVar]) {
            script.error("${description} ${envVar} 未设置")
        }
        return true
    }
    
    /**
     * 安全地执行shell命令并提供详细日志
     * @param script Jenkins脚本上下文
     * @param command 要执行的命令
     * @param description 命令描述
     */
    static def safeShellExecution(script, command, description = '执行命令') {
        try {
            script.echo "开始${description}..."
            script.sh command
            script.echo "${description}成功完成"
        } catch (Exception e) {
            script.error("${description}失败: ${e.getMessage()}")
        }
    }
}