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

    def ANDROID_HOME = "/opt/android-sdk"
    def version = config.version
    def command = []


    try {
        command << "mkdir -p ${ANDROID_HOME}/cmdline-tools"
        command << "wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O cmdline-tools.zip"
        command << "unzip -q cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools"
        command << "mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest"
        command << "rm cmdline-tools.zip"
        command << "echo 'export ANDROID_HOME=${ANDROID_HOME}' >> /etc/profile.d/android-sdk.sh"
        command << "echo 'export PATH=${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:\$PATH' >> /etc/profile.d/android-sdk.sh"
        command << "yes | /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager --licenses"
        command << "/opt/android-sdk/cmdline-tools/latest/bin/sdkmanager \"platform-tools\" \"platforms;android-36\" \"build-tools;36.0.0\" \"ndk;27.1.12297006\""
        command << "echo \"✅ Android SDK ${version} 环境设置成功\""
        def status = script.sh(label: 'Setup Android SDK', script: command.join(" && "), returnStatus: true)
        if (status != 0) {
            script.error("设置 Android SDK ${version} 环境失败，退出码：${status}")
        }
        return this
    } catch (Exception e) {
        script.echo "❌ 设置 Android SDK ${version} 环境失败: ${e.getMessage()}"
        return this
    }
}

return this

