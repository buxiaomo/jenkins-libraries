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
    def cmd = """
if [ ! -f /package/node-v${version}-linux-x64.tar.xz ]; then
    wget -q https://nodejs.org/dist/${version}/node-v${version}-linux-x64.tar.xz -O /package/node-v${version}-linux-x64.tar.xz
fi
tar -xf /package/node-v${version}-linux-x64.tar.xz -C /usr/local --strip-components=1
node -v
"""
    script.sh(label: 'Setup Node.js', script: cmd, returnStatus: true)
}

return this

