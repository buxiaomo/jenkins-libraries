#!/usr/bin/env groovy

/**
 * BuildDockerImage - Jenkins共享库函数，用于构建和推送Docker镜像
 * 
 * 使用示例:
 * BuildDockerImage(this) {
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
 * BuildDockerImage(this) {
 *     host = env.REGISTRY_HOST            // 使用环境变量（无引号）
 *     project = env.JOB_NAME              // 使用环境变量（无引号）
 *     name = 'admin'                      // 字符串常量（有引号）
 *     tag = env.BUILD_NUMBER              // 使用环境变量（无引号）
 *     platform = params.platform         // 使用参数（无引号）
 *     path = './Dockerfile'               // 字符串常量（有引号）
 * }
 */

def call(script, body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def host = config.get('host', env.REGISTRY_HOST)
    def project = config.get('project', env.JOB_NAME)
    def name = config.name
    def tag = config.get('tag', env.BUILD_NUMBER)
    def platform = config.platform
    def path = config.path
    def buildArgs = config.buildArgs
    def progress = config.get('progress', 'auto')

    // 构建Docker命令
    def command = []

    // 判断是否为多平台构建
    def isMultiPlatform = (platform == "linux/amd64,linux/arm64")
    def builderName = isMultiPlatform ? "multi-platform" : "default"

    if (builderName == "default") {
        def enableCache = false
    } else {
        def enableCache = config.enableCache
    }

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
    command << "--push"
    command << "-f ${path}"
    command << "."

    def cmd = command.join(" ")

    script.sh cmd
}

return this
