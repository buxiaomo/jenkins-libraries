#!/usr/bin/env groovy

package org.xiaomo

/**
 * Jenkins 错误恢复机制
 * 提供多层次的错误处理和恢复策略
 */
class ErrorRecovery {
    
    /**
     * 安全执行代码块，提供错误恢复机制
     * @param script Jenkins脚本上下文
     * @param operation 操作名称
     * @param mainAction 主要操作
     * @param fallbackAction 备用操作（可选）
     * @param retryCount 重试次数（默认3次）
     * @return 执行结果
     */
    static def safeExecute(script, String operation, Closure mainAction, Closure fallbackAction = null, int retryCount = 3) {
        def lastException = null
        
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                script.echo "🔄 执行 ${operation} (尝试 ${attempt}/${retryCount})"
                return mainAction.call()
            } catch (Exception e) {
                lastException = e
                script.echo "❌ ${operation} 失败 (尝试 ${attempt}/${retryCount}): ${e.getMessage()}"
                
                if (attempt < retryCount) {
                    script.echo "⏳ 等待 ${attempt * 2} 秒后重试..."
                    script.sleep(attempt * 2)
                } else if (fallbackAction) {
                    script.echo "🔧 尝试备用方案..."
                    try {
                        return fallbackAction.call()
                    } catch (Exception fallbackException) {
                        script.echo "❌ 备用方案也失败: ${fallbackException.getMessage()}"
                        throw new Exception("${operation} 失败，主要错误: ${e.getMessage()}，备用方案错误: ${fallbackException.getMessage()}")
                    }
                }
            }
        }
        
        throw new Exception("${operation} 在 ${retryCount} 次尝试后仍然失败: ${lastException.getMessage()}")
    }
    
    /**
     * 安全获取环境变量，提供多种备用策略
     * @param script Jenkins脚本上下文
     * @param env 环境变量对象
     * @param varName 变量名
     * @param defaultValue 默认值
     * @param required 是否必需
     * @return 环境变量值
     */
    static def safeGetEnvVar(script, env, String varName, String defaultValue = null, boolean required = false) {
        def value = null
        
        try {
            // 方法1: 直接从env对象获取
            if (env && env[varName]) {
                value = env[varName]
                script.echo "✅ 从 env.${varName} 获取到值"
                return value
            }
        } catch (Exception e) {
            script.echo "⚠️ 无法从 env.${varName} 获取值: ${e.getMessage()}"
        }
        
        try {
            // 方法2: 使用Jenkins内置方法
            value = script.env.getProperty(varName)
            if (value) {
                script.echo "✅ 从 script.env.${varName} 获取到值"
                return value
            }
        } catch (Exception e) {
            script.echo "⚠️ 无法从 script.env.${varName} 获取值: ${e.getMessage()}"
        }
        
        try {
            // 方法3: 使用系统属性
            value = System.getProperty(varName)
            if (value) {
                script.echo "✅ 从系统属性 ${varName} 获取到值"
                return value
            }
        } catch (Exception e) {
            script.echo "⚠️ 无法从系统属性 ${varName} 获取值: ${e.getMessage()}"
        }
        
        try {
            // 方法4: 使用shell命令获取
            def result = script.sh(script: "echo \$${varName}", returnStdout: true).trim()
            if (result && result != "\$${varName}") {
                script.echo "✅ 从shell环境变量 ${varName} 获取到值"
                return result
            }
        } catch (Exception e) {
            script.echo "⚠️ 无法从shell环境变量 ${varName} 获取值: ${e.getMessage()}"
        }
        
        // 使用默认值
        if (defaultValue != null) {
            script.echo "📝 使用默认值: ${varName} = ${defaultValue}"
            return defaultValue
        }
        
        // 如果是必需的变量但没有找到
        if (required) {
            def errorMsg = "必需的环境变量 ${varName} 未设置，且没有提供默认值"
            script.echo "❌ ${errorMsg}"
            throw new IllegalArgumentException(errorMsg)
        }
        
        script.echo "⚠️ 环境变量 ${varName} 未设置，返回 null"
        return null
    }
    
    /**
     * 安全执行Docker命令，提供错误恢复
     * @param script Jenkins脚本上下文
     * @param command Docker命令
     * @param operation 操作描述
     * @return 执行结果
     */
    static def safeDockerExecution(script, String command, String operation) {
        return safeExecute(script, operation, {
            script.sh(command)
        }, {
            // 备用方案：尝试清理Docker环境后重试
            script.echo "🧹 清理Docker环境后重试..."
            try {
                script.sh('docker system prune -f || true')
                script.sh('docker builder prune -f || true')
            } catch (Exception cleanupException) {
                script.echo "⚠️ Docker清理失败: ${cleanupException.getMessage()}"
            }
            script.sh(command)
        })
    }
    
    /**
     * 智能错误分析和建议
     * @param script Jenkins脚本上下文
     * @param exception 异常对象
     * @param context 上下文信息
     */
    static def analyzeAndSuggest(script, Exception exception, Map context = [:]) {
        def errorMessage = exception.getMessage().toLowerCase()
        def suggestions = []
        
        script.echo "🔍 错误分析: ${exception.getMessage()}"
        
        // NullPointerException 分析
        if (exception instanceof NullPointerException || errorMessage.contains('null')) {
            suggestions << "检查环境变量是否正确设置"
            suggestions << "验证Jenkins Pipeline上下文是否完整"
            suggestions << "确认所有必需的参数都已提供"
            
            if (errorMessage.contains('registry_host')) {
                suggestions << "设置 REGISTRY_HOST 环境变量或在配置中指定 host 参数"
            }
            if (errorMessage.contains('job_name')) {
                suggestions << "设置 JOB_NAME 环境变量或在配置中指定 project 参数"
            }
            if (errorMessage.contains('build_number')) {
                suggestions << "设置 BUILD_NUMBER 环境变量或在配置中指定 tag 参数"
            }
        }
        
        // 编译错误分析
        if (errorMessage.contains('compilation') || errorMessage.contains('syntax')) {
            suggestions << "检查Groovy语法是否正确"
            suggestions << "验证变量引用格式（使用 env.VARIABLE 而不是 VARIABLE）"
            suggestions << "确认字符串拼接语法正确"
        }
        
        // Docker错误分析
        if (errorMessage.contains('docker')) {
            suggestions << "检查Docker服务是否运行"
            suggestions << "验证Docker镜像仓库连接"
            suggestions << "确认Dockerfile路径正确"
            suggestions << "检查磁盘空间是否充足"
        }
        
        // 权限错误分析
        if (errorMessage.contains('permission') || errorMessage.contains('access')) {
            suggestions << "检查文件和目录权限"
            suggestions << "验证Jenkins用户权限"
            suggestions << "确认Docker组权限配置"
        }
        
        // 网络错误分析
        if (errorMessage.contains('network') || errorMessage.contains('connection') || errorMessage.contains('timeout')) {
            suggestions << "检查网络连接"
            suggestions << "验证防火墙设置"
            suggestions << "确认代理配置"
            suggestions << "检查DNS解析"
        }
        
        // 输出建议
        if (!suggestions.isEmpty()) {
            script.echo "💡 错误修复建议:"
            suggestions.each { suggestion ->
                script.echo "   - ${suggestion}"
            }
        }
        
        // 输出上下文信息
        if (!context.isEmpty()) {
            script.echo "📋 错误上下文:"
            context.each { key, value ->
                script.echo "   ${key}: ${value}"
            }
        }
        
        // 提供调试命令
        script.echo "🔧 调试命令:"
        script.echo "   - 检查环境变量: env | grep -E '(REGISTRY|JOB|BUILD)'"
        script.echo "   - 检查Docker状态: docker info"
        script.echo "   - 检查磁盘空间: df -h"
        script.echo "   - 检查Jenkins日志: 查看完整的构建日志"
    }
    
    /**
     * 创建错误恢复上下文
     * @param script Jenkins脚本上下文
     * @param config 配置对象
     * @param env 环境变量对象
     * @return 错误恢复上下文
     */
    static def createRecoveryContext(script, config, env) {
        def context = [:]
        
        try {
            context.jenkinsVersion = script.env.JENKINS_VERSION ?: 'unknown'
            context.nodeLabel = script.env.NODE_LABELS ?: 'unknown'
            context.workspace = script.env.WORKSPACE ?: 'unknown'
            context.buildNumber = script.env.BUILD_NUMBER ?: 'unknown'
            context.jobName = script.env.JOB_NAME ?: 'unknown'
            context.configKeys = config?.keySet()?.join(', ') ?: 'none'
            context.envAvailable = env != null
        } catch (Exception e) {
            script.echo "⚠️ 创建恢复上下文时发生错误: ${e.getMessage()}"
        }
        
        return context
    }
}