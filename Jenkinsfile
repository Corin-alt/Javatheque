pipeline {
    agent none

    triggers {
        githubPush()
    }

    environment {
        APP_NAME = 'javatheque'
        DOCKER_IMAGE = 'javatheque-env'
        DOCKER_TAG = 'latest'
        DOCKER_REGISTRY = 'ghcr.io'
        GITHUB_OWNER = 'corin-alt'
        GITHUB_TOKEN = credentials('github-token')
        DEPLOY_PPROD_SERVER = credentials('deploy-pprod-server')
        DEPLOY_PROD_SERVER = credentials('deploy-prod-server')
        APP_CODE_PATH = '/apps/java/src'
        APP_DEPLOY_PATH = '/apps/java/deploy'
    }

    stages {
        stage('Maven Build') {
            agent {
                docker {
                    image 'maven:3.9.9-eclipse-temurin-17'
                    args '-v $HOME/.m2:/root/.m2 -u root'
                }
            }
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Unit Tests') {
            agent {
                docker {
                    image 'maven:3.9.9-eclipse-temurin-17'
                    args '-v $HOME/.m2:/root/.m2 -u root'
                }
            }
            steps {
                sh 'mvn clean test -Dtest=**/*UnitTest'
            }
        }

        stage('Build Docker Image') {
            agent {
                docker {
                    image 'docker:dind'
                    args '--privileged -v /var/run/docker.sock:/var/run/docker.sock'
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
                script {
                    sh 'docker --version'
                }
            }
        }

        stage('Deploy to Pre-production') {
            agent {
                docker {
                    image 'alpine:latest'
                    args '-u root'
                }
            }
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
            agent {
                docker {
                    image 'alpine:latest'
                    args '-u root'
                }
            }
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
            node {
                cleanWs()
            }
        }
        failure {
            node {
                echo 'Pipeline failed!'
            }
        }
        success {
            node {
                echo 'Pipeline completed successfully!'
            }
        }
    }
}
