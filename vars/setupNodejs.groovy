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

    def os = script.sh(
        script: ". /etc/os-release && echo \$ID",
        returnStdout: true
    ).trim()

    switch (os) {
        case "ubuntu":
        case "debian":
            command << "apt-get update"
            command << "apt-get install -y curl"
            command << "curl -fsSL https://deb.nodesource.com/setup_${version}.x | bash -"
            command << "apt-get install -y nodejs"
            break
        case "centos":
        case "rhel":
        case "rocky":
            command << "yum install -y curl"
            command << "curl -fsSL https://rpm.nodesource.com/setup_${version}.x | bash -"
            break

        default:
            script.error("不支持的操作系统: ${os}")
    }


    command << "echo \"✅ Node.js ${version} 环境设置成功\""
    script.sh(label: 'Setup Node.js', script: command.join(" && "), returnStatus: true)
}

return this

