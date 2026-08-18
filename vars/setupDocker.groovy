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
    def mirror = config.mirror
    def command = []
    if mirror == null || mirror.trim() == "" {
        command << "curl -fsSL https://get.docker.com | bash -s docker --version ${version} --no-autostart"
    } else{
        command << "curl -fsSL https://get.docker.com | bash -s docker --mirror ${mirror} --version ${version} --no-autostart"
    }
    command << "echo \"✅ Docker ${version} 环境设置成功\""
    script.sh(label: 'Setup Docker', script: command.join(" && "), returnStatus: true)
}

return this

