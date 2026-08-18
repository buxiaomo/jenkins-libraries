#!/usr/bin/env groovy

/**
* 设置 android sdk 环境
* @param script Jenkins脚本上下文
* @param version android sdk版本
*/

// # Set Android SDK environment

// ENV PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools"

// # Install Android command-line tools, Node.js, accept licenses and create app directory
// RUN  && \
//      && \
//      && \
//      && \
//      && \
//      && \
//      && \
//      && \
//      && \
//      && \
//      && \
//     mkdir -p /app && \
//     chown -R 1001:1001 /app ${ANDROID_HOME}

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
        command << "yes | sdkmanager --licenses > /dev/null 2>&1"
        command << "sdkmanager \"platform-tools\" \"platforms;android-36\" \"build-tools;36.0.0\" \"ndk;27.1.12297006\""
        command << "echo \"✅ Android SDK ${version} 环境设置成功\""
        script.sh(label: 'Setup Android SDK', script: command.join(" && "), returnStatus: true)
        return this
    } catch (Exception e) {
        script.echo "❌ 设置 Android SDK ${version} 环境失败: ${e.getMessage()}"
        return this
    }
}

return this

