#!/usr/bin/env groovy

import org.xiaomo.Common

/**
 * ValidatePipelineSyntax - Jenkins共享库函数，用于验证和修复常见的Pipeline语法错误
 * 
 * 使用示例:
 * ValidatePipelineSyntax {
 *     checkEnvironmentVariables = true
 *     checkParameters = true
 *     suggestFixes = true
 * }
 */

def call(body = [:]) {
    def config = [:]
    if (body instanceof Closure) {
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()
    } else {
        config = body
    }

    def checkEnvVars = config.get('checkEnvironmentVariables', true)
    def checkParams = config.get('checkParameters', true)
    def suggestFixes = config.get('suggestFixes', true)

    echo "=== Pipeline Syntax Validation ==="
    
    if (checkEnvVars) {
        validateEnvironmentVariables()
    }
    
    if (checkParams) {
        validateParameters()
    }
    
    if (suggestFixes) {
        provideSyntaxGuidance()
    }
    
    echo "=== Validation Complete ==="
}

def validateEnvironmentVariables() {
    echo "Checking common environment variables..."
    
    def commonEnvVars = ['REGISTRY_HOST', 'JOB_NAME', 'BUILD_NUMBER', 'BUILDER']
    def missingVars = []
    
    commonEnvVars.each { varName ->
        if (!env[varName]) {
            missingVars << varName
        } else {
            echo "✓ ${varName} = ${env[varName]}"
        }
    }
    
    if (missingVars) {
        echo "⚠️ Missing environment variables: ${missingVars.join(', ')}"
        echo "These may cause BuildDockerImage to fail if not provided explicitly."
    }
}

def validateParameters() {
    echo "Checking pipeline parameters..."
    
    if (params) {
        params.each { key, value ->
            echo "✓ Parameter: ${key} = ${value}"
        }
    } else {
        echo "ℹ️ No pipeline parameters defined"
    }
}

def provideSyntaxGuidance() {
    echo """
=== Common Syntax Issues and Fixes ===

❌ WRONG: host = \${env.REGISTRY_HOST}
✅ RIGHT: host = env.REGISTRY_HOST

❌ WRONG: tag = \${BUILD_NUMBER}
✅ RIGHT: tag = env.BUILD_NUMBER

❌ WRONG: tag = BUILD_NUMBER
✅ RIGHT: tag = env.BUILD_NUMBER

❌ WRONG: name = admin
✅ RIGHT: name = 'admin'

❌ WRONG: platform = \${params.platform}
✅ RIGHT: platform = params.platform

=== Syntax Rules ===
1. Environment variables: env.VARIABLE_NAME (no quotes)
2. Parameters: params.parameter_name (no quotes)
3. String constants: 'value' or \"value\" (with quotes)
4. Boolean values: true or false (no quotes)
5. Arrays: ['item1', 'item2'] (with quotes for strings)

=== Example Correct Usage ===
BuildDockerImage {
    host = env.REGISTRY_HOST        // Environment variable
    project = env.JOB_NAME          // Environment variable
    name = 'my-app'                 // String constant
    tag = env.BUILD_NUMBER          // Environment variable
    platform = params.platform     // Parameter
    path = './Dockerfile'           // String constant
    enableCache = true              // Boolean
    buildArgs = ['ARG1=value1']     // Array of strings
}
"""
}

return this