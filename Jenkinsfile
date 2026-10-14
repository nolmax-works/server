pipeline {
    agent any

    tools {
        jdk 'JDK21'
    }

    stages {
        stage('Build and test') {
            steps {
                script {
                    if (isUnix()) {
                        sh './gradlew build'
                    } else {
                        bat 'gradlew.bat build'
                    }
                }
            }
        }

        stage('SonarQube analysis') {
            steps {
                withSonarQubeEnv('SonarQube Cloud') {
                    script {
                        if (isUnix()) {
                            sh './gradlew sonar'
                        } else {
                            bat 'gradlew.bat sonar'
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
