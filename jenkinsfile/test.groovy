//git仓库地址
def gitRepo = "http://192.168.1.222/david/gin-vue-admin.git"

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
def credentialsId = "33c2b02a-af2a-4040-b362-a358c7dfd78b"

String gitAuthorName 

pipeline {
    agent any
    
    options {
        timestamps() //日志会有时间
        skipDefaultCheckout() //删除隐式checkout scm语句
        disableConcurrentBuilds() //禁止并行
        timeout(time: 1, unit: "HOURS") //流水线超时1小时
    }
    tools {
        go 'mygo'
        //mvn 'mymvn'
    }
    parameters {
        //install extended-choice-parameter
         extendedChoice(
            name: '环境',
            //defaultValue: '',
            descriptionPropertyValue: siteLabels,
            multiSelectDelimiter: ',',
            type: 'PT_RADIO',
            value: siteOptions,
            visibleItemCount: siteCount,
        )
        //install git-parameter
        gitParameter name: 'BRANCH',
                     type: 'PT_BRANCH_TAG',
                    // defaultValue: '', 
                     branchFilter: 'origin/(.*)', 
                     useRepository: gitRepo, 
                     quickFilterEnabled: true
    }
    
    stages {
        // stage("test"){
        //     steps{
        //         git branch: BRANCH, credentialsId: '33c2b02a-af2a-4040-b362-a358c7dfd78b', url: 'http://192.168.1.222/david/gin-vue-admin.git'
        //         // git branch: '$BRANCH', url: gitRepo
        //         // script{
        //         //     println("test")
        //         //     print(siteOptions)
        //         //     println("${params.环境}")
        //         //     utils.PrintMsg()
        //         //     hello.Helloutils()
        //         // }
        //     }
        // }
        stage("check"){
            when{ expression { params.环境 != null && params.环境 == "test" } }
            steps{
                 script{
                    println("----${params.环境}----")
                    println("test")
                 }
            }
        }
        stage("pull"){
            steps{
                //checkout scmGit(branches: [[name: BRANCH ]], extensions: [], userRemoteConfigs: [[credentialsId: credentialsId, url: gitRepo]])
                
                script{
                    utils.PrintMsg()
                    utils.pull(BRANCH,credentialsId,gitRepo)   //封装jenkins DSL 方法
                    println("部署环境：${params.环境}")
                    gitAuthorName = utils.GetAuthorName()
                    println("提交人: " + gitAuthorName)
                    String gitCommitMessage = utils.GetCommitMessage()
                    println("提交信息: " + gitCommitMessage)
                    
                }
            }
        }
    
        // stage("build"){
        //     steps{
        //         script{
        //             mvn = tool "mymvn"
        //             println(mvn)
        //             sh "${mvn}/bin/mvn --version"
        //         }
        //     }
        // }
        
        stage("build go"){
            input {
                message " 选择分支  ${BRANCH} \n 确定要构建吗？ "
                ok "yes"
                submitter "admin,david"
            }
            steps {
                script{
                    //mygo = tool "mygo"
                    //sh "${mygo}/bin/go version"
                    sh "go version"
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

