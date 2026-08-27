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
if [ ! -f /package/docker-${version}.tgz ]; then
    wget -q https://download.docker.com/linux/static/stable/x86_64/docker-${version}.tgz -O /package/docker-${version}.tgz
fi
tar -xf /package/docker-${version}.tgz -C /usr/bin --strip-components=1
docker -v
"""
    def status = script.sh(label: 'Setup Docker', script: command, returnStatus: true)
    if (status != 0) {
        script.error("设置 Docker ${version} 环境失败，退出码：${status}")
    }
}

return this

