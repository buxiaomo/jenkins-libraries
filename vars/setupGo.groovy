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
    def command = []

    try {
        // 判断运行环境架构
        def arch = script.sh(script: "uname -m", returnStdout: true).trim()
        if (arch == "x86_64") {
            command << "wget https://go.dev/dl/go${version}.linux-amd64.tar.gz -O go${version}.tar.gz"
        } else if (arch == "aarch64") {
            command << "wget https://go.dev/dl/go${version}.linux-arm64.tar.gz -O go${version}.tar.gz"
        } else {
            script.echo "❌ 不支持的架构: ${arch}"
            return this
        }
        command << "tar -C /usr/local -xzf go${version}.tar.gz"
        command << "rm -f go${version}.tar.gz"
        command << "echo 'export GOPATH=/usr/local/go' >> /etc/profile.d/go.sh"
        command << "echo 'export PATH=/usr/local/go/bin:\$PATH' >> /etc/profile.d/go.sh"
        command << "echo \"✅ Golang ${version} 环境设置成功\""
        def status = script.sh(label: 'Setup Go', script: command.join(" && "), returnStatus: true)
        if (status != 0) {
            script.error("设置 Golang ${version} 环境失败，退出码：${status}")
        }
        return this
    } catch (Exception e) {
        script.echo "❌ 设置 Golang ${version} 环境失败: ${e.getMessage()}"
        return this
    }
}

return this

