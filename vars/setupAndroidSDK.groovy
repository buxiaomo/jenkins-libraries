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
    def commandlineToolsVersion = "15859902"
    def commandlineToolsSHA256 = "4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583"
    def compileSdk = config.compileSdk
    def ndkVersion = config.ndkVersion

    def command = []


    try {
        cmd = """
mkdir -p ${ANDROID_HOME}/cmdline-tools
if [ -f /opt/commandlinetools-linux-${commandlineToolsVersion}_latest.zip ]; then
    sha256sum /opt/commandlinetools-linux-${commandlineToolsVersion}_latest.zip | grep ${commandlineToolsSHA256}
    if [ $? -ne 0 ]; then
        echo "❌ commandlinetools-linux-${commandlineToolsVersion}_latest.zip 文件校验失败，重新下载"
        wget -q https://dl.google.com/android/repository/commandlinetools-linux-${commandlineToolsVersion}_latest.zip -O /opt/commandlinetools-linux-${commandlineToolsVersion}_latest.zip
    fi
else
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-${commandlineToolsVersion}_latest.zip -O /opt/commandlinetools-linux-${commandlineToolsVersion}_latest.zip
unzip -q /opt/commandlinetools-linux-${commandlineToolsVersion}_latest.zip -d ${ANDROID_HOME}/cmdline-tools
ln -sf ${ANDROID_HOME}/cmdline-tools/latest/bin/* /usr/local/bin/
mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest
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
        return this
    }
}

return this

