#!/usr/bin/env groovy

/**
* 设置 docker 环境
*/

def call(script, body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def version = config.version
    def command = """
if [ ! -f /package/node-v${version}-linux-x64.tar.xz ]; then
    wget -q https://nodejs.org/dist/v${version}/node-v${version}-linux-x64.tar.xz -O /package/node-v${version}-linux-x64.tar.xz
fi
tar -xf /package/node-v${version}-linux-x64.tar.xz -C /usr/local --strip-components=1
node -v
"""
    try {
        def status = script.sh(label: 'Setup Node.js', script: command, returnStatus: true)
        if (status != 0) {
            script.error("设置 Node.js ${version} 环境失败，退出码：${status}")
        }
        return this
    } catch (Exception e) {
        script.error("❌ 设置 Node.js ${version} 环境失败: ${e.getMessage()}")
    }
}

return this
