pipeline {
    agent {
        docker {
            image 'maven:3.9.9-eclipse-temurin-17'
            args '-u root' 
        }
    }

    triggers {
        githubPush()
    }

    environment {
        APP_NAME = 'javatheque'
        DOCKER_IMAGE = 'javatheque-env'
        DOCKER_TAG = 'latest'
        DOCKER_REGISTRY = 'ghcr.io'
        GITHUB_OWNER = 'corin-alt'

        DEPLOY_PPROD_SERVER = credentials('deploy-pprod-server') 
        DEPLOY_PROD_SERVER = credentials('deploy-prod-server') 
        APP_CODE_PATH = '/apps/java/src'
        APP_DEPLOY_PATH = '/apps/java/deploy'
    }

    stages {
        stage('Maven Build') {
            steps {
                echo 'Maven Build...'
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Unit Tests') {
            steps {
                echo 'Unit Tests...'
                sh 'mvn clean test -Dtest=**/*UnitTest'
            }
        }


        stage('Build Docker Image') {
            agent {
                docker {
                    image 'ubuntu:latest'
                    args '-u root'
                }
            }
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    anyOf {
                        expression { env.GIT_BRANCH == 'origin/dev' }
                        expression { env.GIT_BRANCH == 'origin/main' }
                    }
                }
            }
            steps {
                echo 'Installing Docker and building image...'
                sh '''
                    # Installation de Docker
                    apt-get update
                    apt-get install -y apt-transport-https ca-certificates curl software-properties-common
                    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
                    add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
                    apt-get update
                    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

                    # Vérification de Docker
                    docker --version
                '''
            }
        }

        stage('Deploy to Pre-production') {
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { env.GIT_BRANCH == 'origin/dev' }
                }
            }
            steps {
                echo 'Deploy to Pre-production...'
            }
        }

        stage('Deploy to Production') {
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { env.GIT_BRANCH == 'origin/main' }
                }
            }
            steps {
                echo 'Deploy to Production...'
            }
        }
    }

    post {
        always {
            node('built-in') {              
                cleanWs()
            }
        }
        failure {
            node('built-in') {
                echo 'Pipeline failed!'
            }
        }
        success {
            node('built-in') {
                echo 'Pipeline completed successfully!'
            }
        }
    }
}
