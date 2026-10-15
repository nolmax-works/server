pipeline {
    agent any

    tools {
        jdk 'JDK21'
    }

    triggers {
        pollSCM('H/1 * * * *')
    }

    stages {
        stage('Build and test') {
            steps {
                script {
                    if (isUnix()) {
                        sh './gradlew build --no-daemon'
                    } else {
                        bat 'gradlew.bat build --no-daemon'
                    }
                }
            }
        }

        stage('SonarQube analysis') {
            steps {
                withSonarQubeEnv('SonarQube Cloud') {
                    script {
                        if (isUnix()) {
                            sh './gradlew sonar --no-daemon'
                        } else {
                            bat 'gradlew.bat sonar --no-daemon'
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true, allowEmptyArchive: true
        }
    }
}
