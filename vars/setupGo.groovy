#!/usr/bin/env groovy

/**
* 设置 go 环境
* @param script Jenkins脚本上下文
* @param version golang版本
*/

def call(script, body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def version = config.version

    try {
        // 判断运行环境架构
        def arch = script.sh(script: "uname -m", returnStdout: true).trim()
        if (arch == "x86_64") {
            script.sh("wget https://go.dev/dl/go${version}.linux-amd64.tar.gz -O go${version}.tar.gz")
        } else if (arch == "aarch64") {
            script.sh("wget https://go.dev/dl/go${version}.linux-arm64.tar.gz -O go${version}.tar.gz")
        } else {
            script.echo "❌ 不支持的架构: ${arch}"
            return this
        }
        script.sh("tar -C /usr/local -xzf go${version}.tar.gz")
        script.sh("rm -f go${version}.tar.gz")
        script.sh("export PATH=/usr/local/go/bin:$PATH")
        script.echo "✅ Golang ${version} 环境设置成功"
        return this
    } catch (Exception e) {
        script.echo "❌ 设置 Golang ${version} 环境失败: ${e.getMessage()}"
        return this
    }
}

return this

