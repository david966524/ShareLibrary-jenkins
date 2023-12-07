import java.net.URLEncoder
import groovy.transform.Field
import java.util.Calendar

@Field String gitlabApiTokenCertID = "417f5ac0-005f-4cbe-9505-497ae20c5ac4"
@Field String tg_TOKEN = "telegramToken"
@Field String chat_Id = "telegramChatid"

def getInfraFile(){

    withCredentials([string(credentialsId: gitlabApiTokenCertID, variable: 'GITLAB_API_TOKEN')]) {
        sh """
            wget --header "PRIVATE-TOKEN: ${GITLAB_API_TOKEN}" http://192.168.1.222/david/infra/-/raw/master/dockerfile/backend/Dockerfile -O Dockerfile
        """
    }
}


def SendTgMsg(msg){
    withCredentials([string(credentialsId: tg_TOKEN, variable: 'tgToken'),string(credentialsId: chat_Id, variable: 'chatId')]) {
        sh """
            curl --location 'https://api.telegram.org/bot${tgToken}/sendMessage' \
            --form text='${msg}' \
            --form chat_id='${chatId}' \
        """
    }
}


def PrintMsg(){
    print("groovy output")
}

def pull(BRANCH,credentialsId,gitRepo){
    checkout scmGit(branches: [[name: BRANCH ]], extensions: [], userRemoteConfigs: [[credentialsId: credentialsId, url: gitRepo]])
}

def withRegistry(registryUrl,credentialsId,imagename,tag){
    docker.withRegistry(registryUrl, credentialsId) {
        println(imagename+":"+tag)
        def customImage = docker.build(imagename+":"+tag)

        /* Push the container to the custom Registry */
        customImage.push()
    }
}

//判断 tag 是否存在， 存在就可以发 ，不存在 报错返回
def checkimagetag(registryUrl,credentialsId,imagename,tag){
    def registryHost="harbor.davidops.info"
    def namespace="private"
    def missingImgs = []
     withDockerRegistry(credentialsId: credentialsId, url: registryUrl) {
        println("${imagename}:${tag}")
        cmd = "docker manifest inspect ${imagename}:${tag}"
        found = sh([returnStatus: true, script: cmd])
            if (found != 0) {
                println("tag 不存在")
                missingImgs.add(imagename) //tag不存在 把image名字 save到list 中
            }else{
                println("tag 存在")
            }
    }

    missingImgs 
    //missingImgs =0 说明 仓库存在tag 不推送 
    //missingImgs !=0 说明 仓库不存在tag 继续推送 
}

//获取变更文件列表，返回HashSet，注意添加的影响文件路径不含仓库目录名
@NonCPS
List<String> getChangedFilesList(){
    def changedFiles = []
    for ( changeLogSet in currentBuild.changeSets ){
        for (entry in changeLogSet.getItems()){
            changedFiles.addAll(entry.affectedPaths)
        }
    }
    return changedFiles
}

// 获取提交ID
@NonCPS
String getGitcommitID(){
    gitCommitID = " "
    for ( changeLogSet in currentBuild.changeSets){
        for (entry in changeLogSet.getItems()){
            gitCommitID = entry.commitId
        }
    }
    return gitCommitID
}

// 获取提交人
@NonCPS
String GetAuthorName(){
    gitAuthorName = " "
    for ( changeLogSet in currentBuild.changeSets){
        for (entry in changeLogSet.getItems()){
            gitAuthorName = entry.author.fullName
        }
    }
    return gitAuthorName
}

// 获取提交信息
@NonCPS
String GetCommitMessage(){
    commitMessage = " "
    for ( changeLogSet in currentBuild.changeSets){
        for (entry in changeLogSet.getItems()){
            commitMessage = entry.msg
        }
    }
    return commitMessage
}