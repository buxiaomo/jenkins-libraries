#!/usr/bin/env groovy

def call(script, body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def version = config.version
    def command = []

    try {
        def arch = script.sh(script: "uname -m", returnStdout: true).trim()
        if (arch == "x86_64") {
            command << "wget https://go.dev/dl/go${version}.linux-amd64.tar.gz -O go${version}.tar.gz"
        } else if (arch == "aarch64") {
            command << "wget https://go.dev/dl/go${version}.linux-arm64.tar.gz -O go${version}.tar.gz"
        } else {
            script.error("❌ 不支持的架构: ${arch}")
        }

        command << "rm -rf /usr/local/go"
        command << "tar -C /usr/local -xzf go${version}.tar.gz"
        command << "rm -f go${version}.tar.gz"
        command << "ln -sf /usr/local/go/bin/go /usr/local/bin/go"
        command << "ln -sf /usr/local/go/bin/gofmt /usr/local/bin/gofmt"
        command << "echo \"✅ Golang ${version} 环境设置成功\""
        def status = script.sh(label: 'Setup Go', script: command.join(" && "), returnStatus: true)
        if (status != 0) {
            script.error("设置 Golang ${version} 环境失败，退出码：${status}")
        }
        script.env.GOROOT = "/usr/local/go"
        return this
    } catch (Exception e) {
        script.error("❌ 设置 Golang ${version} 环境失败: ${e.getMessage()}")
    }
}

return this