#!/usr/bin/env groovy
import org.xiaomo.Common

/**
 * BuildDockerImage - Jenkins共享库函数，用于构建和推送Docker镜像
 * 
 * 使用示例:
 * HelmDeploy(this) {
 *     name = 'appname'                    // 应用名称（必需）
 *     namespace = 'ns'                    // 命名空间
 *     set = ['key=value']                 // 构建参数
 * }
 */

def call(script, body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def name = config.name
    def namespace = config.namespace
    def path = config.path
    def set = config.set

    def command = []
    command << "helm upgrade -i ${name} "
    command << "${path}"
    command << "--namespace ${namespace}"
    command << "--create-namespace"
    // 构建参数
    set.each { arg ->
        command << "--set ${arg}"
    }
    command << "--wait"
    command << "--timeout 1h0s"

    def cmd = command.join(" ")

    script.sh cmd
}

return this
