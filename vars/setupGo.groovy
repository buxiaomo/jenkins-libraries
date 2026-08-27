#!/usr/bin/env groovy

def call(script, body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def version = config.version
    def command = """
if [ ! -f /package/go${version}.linux-amd64.tar.gz ]; then
    wget -q https://go.dev/dl/go${version}.linux-amd64.tar.gz -O /package/go${version}.linux-amd64.tar.gz
fi
rm -rf /usr/local/go
tar -xf /package/go${version}.linux-amd64.tar.gz -C /usr/local
ln -sf /usr/local/go/bin/* /usr/local/bin/
go version
"""
    try {
        def status = script.sh(label: 'Setup Go', script: command, returnStatus: true)
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