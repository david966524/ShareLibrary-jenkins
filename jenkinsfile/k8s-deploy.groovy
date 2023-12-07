// Telegram configre
def TOKEN = credentials('telegramToken')
def CHAT_ID = credentials('telegramChatid')


// git仓库地址
def gitRepo = "http://192.168.1.222/david/gin-vue-admin.git"
//credentialsId
def credentialsId = "33c2b02a-af2a-4040-b362-a358c7dfd78b"
//镜像仓库地址
def registryUrl = "https://harbor.davidops.info/"
def registryCredentialsId = "e7f8f0fd-67f3-4906-ba7a-1534cf51f66f"
def imageName = "harbor.davidops.info/private/go-server"
def tag = env.BUILD_ID
def buildUser = env.BUILD_USER
// 环境列表
def sites = [
    "test": "测试环境",
    "dev": "开发环境",
    "try": "体验环境",
    "prod": "正式环境",
    "onlyBuild": "只打包",
]

// 环境选项
def siteLabels = sites.values().join(',')
// 环境选项标签
def siteOptions = sites.keySet().join(',')
def siteCount = sites.size()

String gitAuthorName

pipeline {
  agent{
        kubernetes {
            defaultContainer 'jnlp'
            yaml """
apiVersion: v1
kind: Pod
metadata:
labels:
  component: ci
spec:
  # Use service account that can deploy to all namespaces
  # serviceAccountName: kube-ops
  containers:
  - name: docker
    image: docker
    imagePullPolicy: IfNotPresent
    tty: true
    securityContext:
      privileged: true
"""
        }
    }
    options {
        timestamps() //日志会有时间
        skipDefaultCheckout() //删除隐式checkout scm语句
        //disableConcurrentBuilds() //禁止并行
        timeout(time: 1, unit: "HOURS") //流水线超时1小时
    }
    parameters {
        //install extended-choice-parameter
         extendedChoice(
            name: '环境',
            defaultValue: 'onlyBuild',
            descriptionPropertyValue: siteLabels,
            multiSelectDelimiter: ',',
            type: 'PT_RADIO',
            value: siteOptions,
            visibleItemCount: siteCount,
        )
         //install git-parameter
        gitParameter name: 'BRANCH',
                     type: 'PT_BRANCH_TAG',
                     defaultValue: 'test',
                     branchFilter: 'origin/(.*)',
                     useRepository: gitRepo,
                     quickFilterEnabled: true
    }
  stages {
        stage("pull"){
            when {
                expression {
                    return params.环境 && params.BRANCH
                }
            }
            steps{
                //checkout scmGit(branches: [[name: BRANCH ]], extensions: [], userRemoteConfigs: [[credentialsId: '842ea056-6087-470a-9ca0-06bd1e9fa13c', url: gitRepo]])
                script{
                    utils.pull(BRANCH,credentialsId,gitRepo)   //封装jenkins DSL 方法
                    println("部署环境：${params.环境}")
                    gitAuthorName = utils.GetAuthorName()
                    println("提交人: " + gitAuthorName)
                    gitCommitMessage = utils.GetCommitMessage()
                    println("提交信息: " + gitCommitMessage)
                    
                }
            }
        }

        stage("build"){
                when {
                        expression {
                            return params.环境 && params.BRANCH
                        }
                    }
                steps{
                    dir('server'){
                    script{
                        container('docker') {
                            println("Current tag value: ${tag}")
                            utils.getInfraFile()
                            //utils.withRegistry(registryUrl,registryCredentialsId,imageName,tag)
                        }
                    }
                }
            }
        }
        stage("deploy"){
            when {
                expression {
                    return params.环境 && params.BRANCH
                }
            }
            steps{
                script{
                    println "deploy ${params.环境}"
                    println "${imageName}:${tag}"
                }
            }
        }
        stage('Example'){
            steps{
                script{
                    utils.SendTgMsg("sdd")
                }
            }
        }
  }
    post {
        always {
            script {
                println("always")
                sh "curl --location 'https://api.telegram.org/bot6905666656:AAGYC0fizUCy9-E3LS_RhAN8hlWc5gVWjGc/sendMessage' --form text='test' --form chat_id='-4017623842'"
            }
        }
        
        success {
            script {
                currentBuild.description = "\n 构建成功"
                //currentBuild.displayName = env.BUILD_USER 
                utils.SendTgMsg(
                    """
${env.JOB_BASE_NAME}
${currentBuild.result}
${env.BUILD_URL}
${BUILD_USER}
${params.环境}
                    """)

            }
            
        }
        
        failure {
            script {
                currentBuild.description = "\n 构建失败"
                utils.SendTgMsg("failure")
            }
        }
        
        aborted {
            script {
                currentBuild.description = "\n 构建取消"
            }
        }
    }
}