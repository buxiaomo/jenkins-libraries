#!/usr/bin/env groovy

/**
* 设置 android sdk 环境
* @param script Jenkins脚本上下文
* @param version android sdk版本
*/

def call(script, body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def MAESTRO_HOME = "/opt/maestro"
    def version = config.version
    def command = []


    try {
        cmd = """
mkdir -p ${MAESTRO_HOME}
if [ ! -f /package/maestro-${version}.zip ]; then
    wget -q https://github.com/mobile-dev-inc/Maestro/releases/download/cli-${version}/maestro.zip -O /package/maestro-${version}.zip
fi
unzip -q /package/maestro-${version}.zip -d ${MAESTRO_HOME}/
ln -sf ${MAESTRO_HOME}/bin/* /usr/local/bin/
"""
        def status = script.sh(label: 'Setup Maestro', script: cmd, returnStatus: true)
        if (status != 0) {
            script.error("设置 Maestro 环境失败，退出码：${status}")
        }
        script.env.MAESTRO_HOME = MAESTRO_HOME
        return this
    } catch (Exception e) {
        script.echo "❌ 设置 Maestro 环境失败: ${e.getMessage()}"
        return this
    }
}

return this

