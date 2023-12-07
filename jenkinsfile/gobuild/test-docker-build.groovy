
// git仓库地址
def gitRepo = "http://192.168.1.222/david/gin-vue-admin.git"
//credentialsId
def credentialsId = "33c2b02a-af2a-4040-b362-a358c7dfd78b"
//镜像仓库地址
def registryUrl = "https://harbor.davidops.info/"
def registryCredentialsId = "e7f8f0fd-67f3-4906-ba7a-1534cf51f66f"
def imageName = "harbor.davidops.info/private/go-server"
def tag = env.BUILD_ID
// 环境列表
def sites = [
    "test": "测试环境",
    "dev": "开发环境",
    "try": "体验环境",
    "prod": "正式环境",
]

// 环境选项
def siteLabels = sites.values().join(',')
// 环境选项标签
def siteOptions = sites.keySet().join(',')
def siteCount = sites.size()

String gitAuthorName

pipeline {
   agent any
    options {
        timestamps() //日志会有时间
        skipDefaultCheckout() //删除隐式checkout scm语句
        disableConcurrentBuilds() //禁止并行
        timeout(time: 1, unit: "HOURS") //流水线超时1小时
    }
    parameters {
         //install extended-choice-parameter
         extendedChoice(
            name: '环境',
            defaultValue: 'test',
            descriptionPropertyValue: siteLabels,
            multiSelectDelimiter: ',',
            type: 'PT_RADIO',
            value: siteOptions,
            visibleItemCount: siteCount,
        )

        string(name: 'ImageTag', defaultValue: 'latest', description: '镜像tag')

        //install git-parameter
        gitParameter name: 'BRANCH',
                     type: 'PT_BRANCH_TAG',
                     defaultValue: 'test',
                     branchFilter: 'origin/(.*)',
                     useRepository: gitRepo,
                     quickFilterEnabled: true
        //获取镜像tag   Image Tag Parameter
        imageTag(
            name: 'DOCKER_IMAGE', description: '',
            image: 'private/go-server', filter: '', defaultTag: '',
            registry: registryUrl, credentialId: registryCredentialsId, tagOrder: 'NATURAL')
    }

    stages {
         // stage("test"){
         //     steps{
         //         //git branch: '$BRANCH', credentialsId: '842ea056-6087-470a-9ca0-06bd1e9fa13c', url: 'https://github.com/david966524/ShareLibrary-jenkins.git'
         //         git branch: '$BRANCH', url: gitRepo
         //         script{
         //             println("test")
         //             print(siteOptions)
         //             println("${params.环境}")
         //             utils.PrintMsg()
         //             hello.Helloutils()
         //         }
         //     }
         // }
         stage('Test') { // 测试获取的tag
                steps {
                    echo "$DOCKER_IMAGE" // will print selected image name with tag (eg. jenkins/jenkins:lts-jdk11)
                    echo "$DOCKER_IMAGE_TAG" // will print selected tag value (eg. lts-jdk11)
                    echo "$DOCKER_IMAGE_IMAGE" // will print selected image name value (eg. jenkins/jenkins)
                }
         }
    
        stage("check"){
            when{ expression { env.ImageTag != null && env.ImageTag != "latest" } }
            steps{
                println(env.ImageTag)
                script{
                    tag = env.ImageTag
                }
            }
        }

        stage("pull"){
            steps{
                //checkout scmGit(branches: [[name: BRANCH ]], extensions: [], userRemoteConfigs: [[credentialsId: '842ea056-6087-470a-9ca0-06bd1e9fa13c', url: gitRepo]])
                script{
                    utils.pull(BRANCH,credentialsId,gitRepo)   //封装jenkins DSL 方法
                    println("部署环境：${params.环境}")
                    gitAuthorName = utils.GetAuthorName()
                    println("提交人: " + gitAuthorName)
                    String gitCommitMessage = utils.GetCommitMessage()
                    println("提交信息: " + gitCommitMessage)

                }
            }
        }
        stage("show file"){
            steps{
                dir('server'){
                    sh 'ls -al'
                }
            }
        }

        // stage("build"){
        //     // agent docker 也可以定义在stage 中
        //     // agent {
        //     //     docker {
        //     //         image 'maven:3.6.3-jdk-8'
        //     //         args '-v $HOME/.m2:/root/.m2'
        //     //     }
        //     // }
        //     steps{
        //         script{

        //             sh "mvn --version"
        //         }
        //     }
        // }

        stage("build go"){
            input {
                message "选择分支  ${BRANCH} \n Tag ${tag} \n 确定要构建吗？"
                ok "yes"
                submitter "admin,david "
            }
            steps {
                dir('server'){
                    script{
                        // docker.build("test-image","server/Dockerfile")
                            // docker.withRegistry('https://harbor.davidops.info/', 'e7f8f0fd-67f3-4906-ba7a-1534cf51f66f') {

                            //     def customImage = docker.build("harbor.davidops.info/private/go-server:")

                            //     /* Push the container to the custom Registry */
                            //     customImage.push()
                            // }
                            //  https://www.jenkins.io/zh/doc/book/pipeline/docker/  使用方式
                            // 在这里再次确认 tag 的值
                            println("Current tag value: ${tag}")
                            utils.withRegistry(registryUrl,registryCredentialsId,imageName,tag)
                    }
                }
            }
        }
}

    post {
        // always {
        //     script {
        //         println("always")
        //     }
        // }

        success {
            script {
                currentBuild.description = "\n 构建成功"
            //currentBuild.displayName = gitAuthorName
            }
        }

        failure {
            script {
                currentBuild.description = "\n 构建失败"
            }
        }

        aborted {
            script {
                currentBuild.description = "\n 构建取消"
            }
        }
    }
}
