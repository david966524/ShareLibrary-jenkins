
// git仓库地址
def gitRepo = "https://github.com/david966524/ShareLibrary-jenkins.git"

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
//credentialsId
def credentialsId = "842ea056-6087-470a-9ca0-06bd1e9fa13c"

String gitAuthorName 

pipeline {
    agent {
        docker {
            image 'maven:3.6.3-jdk-8'
            args '-v $HOME/.m2:/root/.m2'
        }
    }
    
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
        //install git-parameter
        gitParameter name: 'BRANCH',type: 'PT_BRANCH_TAG',defaultValue: 'release/test', branchFilter: 'origin/(.*)', useRepository: gitRepo,quickFilterEnabled: true
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
    
        stage("build"){
            steps{
                script{
               
                    sh "mvn --version"
                }
            }
        }
        
        stage("build go"){
            input {
                message "确定要构建吗？"
                ok "yes"
                submitter "admin,david"
            }
            steps {
                script{
                    mygo = tool "mygo"
                    sh "${mygo}/bin/go version"
                }
            }
        }
    }
    
       
    post {
        always {
            script {
                println("always")
            }
        }
        
        success {
            script {
                currentBuild.description = "\n 构建成功"
                currentBuild.displayName = gitAuthorName
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