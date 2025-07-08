#!/usr/bin/env groovy

package org.xiaomo

/**
 * Jenkins 配置验证器
 * 提供安全的配置验证和错误恢复机制
 */
class ConfigValidator {
    
    /**
     * 验证 BuildDockerImage 配置
     * @param config 配置对象
     * @param script Jenkins脚本上下文
     * @param env 环境变量对象
     * @return 验证结果和建议
     */
    static def validateBuildDockerImageConfig(config, script, env = null) {
        def validationResults = [
            errors: [],
            warnings: [],
            suggestions: []
        ]
        
        try {
            script.echo "开始配置验证..."
            
            // 1. 验证基本语法
            validateBasicSyntax(config, validationResults)
            
            // 2. 验证环境变量依赖
            validateEnvironmentDependencies(config, env, validationResults)
            
            // 3. 验证参数完整性
            validateParameterCompleteness(config, validationResults)
            
            // 4. 提供优化建议
            provideOptimizationSuggestions(config, validationResults)
            
            // 输出验证结果
            reportValidationResults(script, validationResults)
            
        } catch (Exception e) {
            script.echo "⚠️ 配置验证过程中发生错误: ${e.getMessage()}"
            script.echo "继续执行，但建议检查配置..."
        }
        
        return validationResults
    }
    
    /**
     * 验证基本语法错误
     */
    private static def validateBasicSyntax(config, results) {
        config.each { key, value ->
            if (value instanceof String) {
                // 检查 ${} 语法错误
                if (value.contains('${') && !value.startsWith('./') && !value.startsWith('/')) {
                    results.errors << "参数 '${key}' 包含错误的变量语法: ${value}"
                    results.suggestions << "将 '${key} = ${value}' 改为 '${key} = env.VARIABLE_NAME' 或 '${key} = params.parameter_name'"
                }
                
                // 检查常见的裸露变量名
                if (value == 'BUILD_NUMBER') {
                    results.errors << "参数 '${key}' 使用了裸露的 BUILD_NUMBER"
                    results.suggestions << "将 '${key} = BUILD_NUMBER' 改为 '${key} = env.BUILD_NUMBER'"
                }
                
                if (value == 'JOB_NAME') {
                    results.errors << "参数 '${key}' 使用了裸露的 JOB_NAME"
                    results.suggestions << "将 '${key} = JOB_NAME' 改为 '${key} = env.JOB_NAME'"
                }
            }
        }
    }
    
    /**
     * 验证环境变量依赖
     */
    private static def validateEnvironmentDependencies(config, env, results) {
        def requiredEnvVars = ['REGISTRY_HOST']
        def optionalEnvVars = ['JOB_NAME', 'BUILD_NUMBER', 'BUILDER']
        
        // 检查必需的环境变量
        requiredEnvVars.each { envVar ->
            if (!env || !env[envVar]) {
                if (!config.containsKey(envVar.toLowerCase().replace('_', ''))) {
                    results.warnings << "环境变量 ${envVar} 未设置，且配置中未提供对应参数"
                    results.suggestions << "设置环境变量 ${envVar} 或在配置中明确指定对应参数"
                }
            }
        }
        
        // 检查可选的环境变量
        optionalEnvVars.each { envVar ->
            if (!env || !env[envVar]) {
                results.warnings << "可选环境变量 ${envVar} 未设置，将使用默认值"
            }
        }
    }
    
    /**
     * 验证参数完整性
     */
    private static def validateParameterCompleteness(config, results) {
        def requiredParams = ['name']
        def recommendedParams = ['host', 'project', 'tag']
        
        // 检查必需参数
        requiredParams.each { param ->
            if (!config[param]) {
                results.errors << "缺少必需参数: ${param}"
                results.suggestions << "请在配置中添加 ${param} 参数"
            }
        }
        
        // 检查推荐参数
        recommendedParams.each { param ->
            if (!config[param]) {
                results.warnings << "缺少推荐参数: ${param}，将使用默认值或环境变量"
            }
        }
    }
    
    /**
     * 提供优化建议
     */
    private static def provideOptimizationSuggestions(config, results) {
        // 检查缓存配置
        if (config.enableCache == null) {
            results.suggestions << "建议明确设置 enableCache 参数以优化构建性能"
        }
        
        // 检查平台配置
        if (!config.platform) {
            results.suggestions << "建议明确设置 platform 参数，默认为 linux/amd64"
        } else if (config.platform == 'linux/amd64,linux/arm64') {
            results.suggestions << "使用多平台构建，确保已配置 multi-platform builder"
        }
        
        // 检查构建参数
        if (!config.buildArgs || config.buildArgs.isEmpty()) {
            results.suggestions << "考虑使用 buildArgs 参数传递构建时变量"
        }
    }
    
    /**
     * 报告验证结果
     */
    private static def reportValidationResults(script, results) {
        if (results.errors.isEmpty() && results.warnings.isEmpty()) {
            script.echo "✅ 配置验证通过，无发现问题"
        } else {
            if (!results.errors.isEmpty()) {
                script.echo "❌ 发现 ${results.errors.size()} 个错误:"
                results.errors.each { error ->
                    script.echo "   - ${error}"
                }
            }
            
            if (!results.warnings.isEmpty()) {
                script.echo "⚠️ 发现 ${results.warnings.size()} 个警告:"
                results.warnings.each { warning ->
                    script.echo "   - ${warning}"
                }
            }
        }
        
        if (!results.suggestions.isEmpty()) {
            script.echo "💡 优化建议:"
            results.suggestions.each { suggestion ->
                script.echo "   - ${suggestion}"
            }
        }
        
        // 如果有严重错误，抛出异常
        if (!results.errors.isEmpty()) {
            def errorMessage = "配置验证失败，发现 ${results.errors.size()} 个错误。请修复后重试。"
            script.error(errorMessage)
        }
    }
    
    /**
     * 安全的配置获取方法
     * @param config 配置对象
     * @param key 配置键
     * @param env 环境变量对象
     * @param envKey 对应的环境变量键
     * @param defaultValue 默认值
     * @return 配置值
     */
    static def safeGetConfig(config, key, env, envKey, defaultValue = null) {
        // 优先使用配置中的值
        if (config.containsKey(key) && config[key] != null) {
            return config[key]
        }
        
        // 其次使用环境变量
        if (env && env[envKey]) {
            return env[envKey]
        }
        
        // 最后使用默认值
        return defaultValue
    }
    
    /**
     * 生成配置报告
     * @param config 最终配置
     * @param script Jenkins脚本上下文
     */
    static def generateConfigReport(config, script) {
        script.echo "📋 最终配置报告:"
        script.echo "   镜像仓库: ${config.host ?: 'NOT SET'}"
        script.echo "   项目名称: ${config.project ?: 'NOT SET'}"
        script.echo "   应用名称: ${config.name ?: 'NOT SET'}"
        script.echo "   镜像标签: ${config.tag ?: 'NOT SET'}"
        script.echo "   目标平台: ${config.platform ?: 'linux/amd64'}"
        script.echo "   Dockerfile: ${config.path ?: './Dockerfile'}"
        script.echo "   启用缓存: ${config.enableCache ?: true}"
        script.echo "   构建参数: ${config.buildArgs ?: '[]'}"
        script.echo "   进度模式: ${config.progress ?: 'auto'}"
    }
}