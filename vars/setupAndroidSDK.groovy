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
    def compileSdk = config.compileSdk
    def ndkVersion = config.ndkVersion

    def command = []


    try {
        command << "mkdir -p ${ANDROID_HOME}/cmdline-tools"
        command << "wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O cmdline-tools.zip"
        command << "unzip -q cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools"
        command << "ln -sf ${ANDROID_HOME}/cmdline-tools/latest/bin/* /usr/local/bin/"
        command << "mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest"
        command << "rm cmdline-tools.zip"
        command << "yes | ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager --licenses"
        command << "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager \"platform-tools\" \"platforms;android-${compileSdk}\" \"build-tools;${compileSdk}.0.0\" \"ndk;${ndkVersion}\""
        command << "echo \"✅ Android SDK ${compileSdk} 环境设置成功\""
        def status = script.sh(label: 'Setup Android SDK', script: command.join(" && "), returnStatus: true)
        if (status != 0) {
            script.error("设置 Android SDK ${compileSdk} 环境失败，退出码：${status}")
        }
        script.env.ANDROID_HOME = ANDROID_HOME
        return this
    } catch (Exception e) {
        script.echo "❌ 设置 Android SDK ${compileSdk} 环境失败: ${e.getMessage()}"
        return this
    }
}

return this

