void setBuildStatus(String message, String state) {
  step([
      $class: "GitHubCommitStatusSetter",
      reposSource: [$class: "ManuallyEnteredRepositorySource", url: "${env.GIT_URL}"],
      contextSource: [$class: "ManuallyEnteredCommitContextSource", context: "ci/jenkins/build-status"],
      errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
      statusResultSource: [ $class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
  ]);
}

pipeline {
    agent any

    stages {
        stage('Maven build') {
            steps {
                withMaven(maven: 'Maven', options: [findbugsPublisher(disabled: true)]) {
                    sh "mvn clean install -e -P let_reporting"
                }
            }        
        }
        
        stage('Reporting') {
            steps {
                pmd canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/target/pmd.xml', thresholdLimit: 'high', unHealthy: ''
                findbugs canComputeNew: false, defaultEncoding: '', excludePattern: '', healthy: '', includePattern: '', isRankActivated: true, pattern: '**/target/findbugsXml.xml', unHealthy: ''
                jacoco classPattern: '**/build/classes/*/main/', execPattern: '**/target/jacoco.exec', sourcePattern: '**/src/main/java'
            }        
        }
        
        stage('Tag') {
            steps {
                echo 'Build is tagged here.'
            }
        }
        
        stage('Push to Nexus') {
            steps {
                echo 'Build is pushed to Nexus here.'
            }
        }
        
    }

    post {
        failure {
            setBuildStatus("Build failed", "FAILURE");
            emailext body: "The build of the LET Application (${env.JOB_NAME}) failed! See ${env.BUILD_URL}", recipientProviders: [[$class: 'CulpritsRecipientProvider']], subject: 'LET Application Build Failure'
        }
        success {
            setBuildStatus("Build complete", "SUCCESS");
        }
    }
    options {
        timeout(time: 10, unit: 'MINUTES')
        buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '7'))
    }
    triggers {
        pollSCM('H/5 * * * *')
    }  
       
}    