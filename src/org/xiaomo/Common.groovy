#!/usr/bin/env groovy

package org.xiaomo

/**
 * Jenkins 共享库通用工具类
 * 提供可在多个共享库函数中复用的通用方法
 */
class Common {
    /**
     * 设置 docker 环境
     * @param script Jenkins脚本上下文
     * @param version Docker版本
     */
    static boolean setupDocker(script, String version, String mirror = 'Aliyun') {
        try {
            script.sh("curl -fsSL https://get.docker.com | bash -s docker --mirror ${mirror} --version ${version} --no-autostart")
            script.echo "✅ Docker 环境设置成功"
            return true
        } catch (Exception e) {
            script.echo "❌ 设置 Docker 环境失败: ${e.getMessage()}"
            return false
        }
    }

    /**
     * 设置 go 环境
     * @param script Jenkins脚本上下文
     * @param version golang版本
     */
    static boolean setupGo(script, String version) {
        try {
            // 判断运行环境架构
            def arch = script.sh(script: "uname -m", returnStdout: true).trim()
            if (arch == "x86_64") {
                script.sh("wget https://go.dev/dl/go${version}.linux-amd64.tar.gz -O go${version}.tar.gz")
            } else if (arch == "aarch64") {
                script.sh("wget https://go.dev/dl/go${version}.linux-arm64.tar.gz -O go${version}.tar.gz")
            } else {
                script.echo "❌ 不支持的架构: ${arch}"
                return false
            }
            script.sh("tar -C /usr/local -xzf go${version}.tar.gz")
            script.sh("rm -f go${version}.tar.gz")
            script.sh("export PATH=/usr/local/go/bin:$PATH")
            script.echo "✅ Golang ${version} 环境设置成功"
            return true
        } catch (Exception e) {
            script.echo "❌ 设置 Golang ${version} 环境失败: ${e.getMessage()}"
            return false
        }
    }

    /**
     * 判断文件夹是否存在
     * @param script Jenkins脚本上下文
     * @param dirPath 文件夹路径
     * @return boolean 文件夹是否存在
     */
    static boolean directoryExists(script, String dirPath) {
        try {
            // 使用Jenkins的fileExists方法检查路径是否存在
            if (!script.fileExists(dirPath)) {
                return false
            }
            
            // 进一步验证是否为目录
            def result = script.sh(
                script: "test -d '${dirPath}' && echo 'true' || echo 'false'",
                returnStdout: true
            ).trim()
            
            return result == 'true'
        } catch (Exception e) {
            script.echo "检查文件夹存在性时发生错误: ${e.getMessage()}"
            return false
        }
    }
    
    /**
     * 判断文件是否存在
     * @param script Jenkins脚本上下文
     * @param filePath 文件路径
     * @return boolean 文件是否存在
     */
    static boolean fileExists(script, String filePath) {
        try {
            return script.fileExists(filePath)
        } catch (Exception e) {
            script.echo "检查文件存在性时发生错误: ${e.getMessage()}"
            return false
        }
    }
    
    /**
     * 创建文件夹（如果不存在）
     * @param script Jenkins脚本上下文
     * @param dirPath 文件夹路径
     * @return boolean 创建是否成功
     */
    static boolean createDirectoryIfNotExists(script, String dirPath) {
        try {
            if (!directoryExists(script, dirPath)) {
                script.sh "mkdir -p '${dirPath}'"
                script.echo "创建文件夹: ${dirPath}"
                return true
            }
            script.echo "文件夹已存在: ${dirPath}"
            return true
        } catch (Exception e) {
            script.echo "创建文件夹失败: ${e.getMessage()}"
            return false
        }
    }
    
    /**
     * 验证必需的文件夹是否存在，不存在则抛出错误
     * @param script Jenkins脚本上下文
     * @param dirPath 文件夹路径
     * @param description 文件夹描述
     */
    static void validateDirectoryExists(script, String dirPath, String description = '文件夹') {
        if (!directoryExists(script, dirPath)) {
            def errorMsg = "${description} '${dirPath}' 不存在"
            script.echo "❌ ${errorMsg}"
            script.error(errorMsg)
        }
        script.echo "✅ ${description} '${dirPath}' 存在"
    }
}