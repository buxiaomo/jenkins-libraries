#!/usr/bin/env groovy

import org.xiaomo.Common

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
 */

def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    // 参数验证和默认值设置
    def host = Common.validateAndGet(config, 'host', env.REGISTRY_HOST, '镜像仓库地址')
    def project = Common.validateAndGet(config, 'project', env.JOB_NAME, '项目名称')
    def name = Common.validateAndGet(config, 'name', null, '应用名称', true)
    def tag = config.get('tag', BUILD_NUMBER ?: 'latest')
    def platform = config.get('platform', 'linux/amd64')
    def path = config.get('path', './Dockerfile')
    def enableCache = config.get('enableCache', true)
    def buildArgs = config.get('buildArgs', [])
    def progress = config.get('progress', 'auto')

    // 验证必需的环境变量
    Common.validateEnvVar(this, 'BUILDER', '环境变量')

    // 验证Dockerfile是否存在
    Common.validateFileExists(this, path, 'Dockerfile')

    // 构建Docker命令
    def dockerCommand = Common.buildDockerCommand(host, project, name, tag, platform, path, enableCache, buildArgs, progress)
    
    echo "开始构建Docker镜像: ${host}/${project}/${name}:${tag}"
    echo "构建平台: ${platform}"
    echo "Dockerfile路径: ${path}"
    
    // 执行Docker构建命令
    Common.safeShellExecution(this, dockerCommand, "Docker镜像构建")
}

return this
