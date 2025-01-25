pipeline {
    agent {
        docker {
            image 'maven:3.9.9-eclipse-temurin-17'
            args '-u root -v /var/run/docker.sock:/var/run/docker.sock -v /usr/bin/docker:/usr/bin/docker'
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
        stage('Environnement Setup') {
            steps {
                echo 'Environnement configuration...'
                sh 'docker --version'
                sh '''
                    apt-get update
                    apt-get install -y docker.io
                '''
            }
        }

        stage('Maven Build') {
            when { 
                expression { currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                echo 'Maven Build...'
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Unit Tests') {
            when {
                expression { currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                echo 'Unit Tests...'
                sh 'mvn clean test -Dtest=**/*UnitTest'
            }
        }

        stage('Build Docker Image') {
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
                echo 'Build Docker Image...'

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