#!/usr/bin/env groovy

def call(script, body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def host = config.host
    def username = config.username
    def password = config.password

    // 构建Docker命令
    def cmd = "echo ${password} | docker login ${host} -u ${username} --password-stdin"
    script.sh cmd
}

return this
