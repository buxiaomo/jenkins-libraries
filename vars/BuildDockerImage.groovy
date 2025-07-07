#!/usr/bin/env groovy

/*
// docker buildx --builder ${env.BUILDER} build --progress=plain --platform=${params.platform} -t ${env.REGISTRY_HOST}/${env.JOB_NAME}/dubhe-data-process:${BUILD_NUMBER} -t ${env.REGISTRY_HOST}/${env.JOB_NAME}/dubhe-data-process:latest --push .
BuildDockerImage {
    host = '192.168.1.1:5000'
    project = 'projectName'
    name = 'appname'
    tag = '1.0.0'
    platform = 'linux/amd64,linux/arm64'
    path = './Dockerfile'
}
*/

def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def host = config.get('host', env.REGISTRY_HOST)
    def project = config.get('project', env.JOB_NAME)
    def name = config.get('name', '')
    def tag = config.get('tag', BUILD_NUMBER)
    def platform = config.get('platform', 'linux/amd64')
    def path = config.get('path', './Dockerfile')

    sh "docker buildx --builder ${env.BUILDER} build --platform=${platform} -t ${host}/${project}/${name}:${tag} -t ${host}/${project}/${name}:latest --cache-to type=registry,ref=${host}/${project}/${name}:buildcache,mode=max --cache-from type=registry,ref=${host}/${project}/${name}:buildcache --push . -f ${path}"
}

return this
