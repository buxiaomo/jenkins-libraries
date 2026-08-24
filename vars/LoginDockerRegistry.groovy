#!/usr/bin/env groovy

def call(script, body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def credentials = config.credentials
    def host = config.host

    script.withCredentials([
        script.usernamePassword(
            credentialsId: credentials,
            usernameVariable: 'USERNAME',
            passwordVariable: 'PASSWORD',
        ),
    ]) {
        script.sh """
            printf '%s' "\$PASSWORD" | docker login "${config.host}" -u "\$USERNAME" --password-stdin
        """
    }
}

return this
