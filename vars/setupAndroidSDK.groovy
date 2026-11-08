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
    def commandlineToolsVersion = config.commandlineToolsVersion
    def commandlineToolsSHA = config.commandlineToolsSHA
    def compileSdk = config.compileSdk
    def ndkVersion = config.ndkVersion

    try {
        cmd = """
set -eu
mkdir -p ${ANDROID_HOME}/cmdline-tools/latest
if [ ! -f /package/commandlinetools-linux-${commandlineToolsVersion}_latest.zip ]; then
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-${commandlineToolsVersion}_latest.zip -O /package/commandlinetools-linux-${commandlineToolsVersion}_latest.zip
fi
unpack_dir=\$(mktemp -d)
unzip -q /package/commandlinetools-linux-${commandlineToolsVersion}_latest.zip -d "\$unpack_dir"
cp -R "\$unpack_dir/cmdline-tools/." "${ANDROID_HOME}/cmdline-tools/latest/"
rm -rf "\$unpack_dir"
ln -sf "${ANDROID_HOME}"/cmdline-tools/latest/bin/* /usr/local/bin/
yes | ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager --licenses
${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager \"platform-tools\" \"platforms;android-${compileSdk}\" \"build-tools;${compileSdk}.0.0\" \"ndk;${ndkVersion}\"
"""
        def status = script.sh(label: 'Setup Android SDK', script: cmd, returnStatus: true)
        if (status != 0) {
            script.error("设置 Android SDK ${compileSdk} 环境失败，退出码：${status}")
        }
        script.env.ANDROID_HOME = ANDROID_HOME
        return this
    } catch (Exception e) {
        script.echo "❌ 设置 Android SDK ${compileSdk} 环境失败: ${e.getMessage()}"
        throw e
    }
}

return this

