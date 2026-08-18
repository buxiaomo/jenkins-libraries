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
    def command = []
    command << "curl -fsSL https://deb.nodesource.com/setup_20.x | bash -"
    command << "apt-get install -y nodejs"
    command << "echo \"✅ Node.js ${version} 环境设置成功\""
    script.sh(label: 'Setup Node.js', script: command.join(" && "), returnStatus: true)
}

return this

