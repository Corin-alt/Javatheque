pipeline {
    agent {
        docker {
            image 'maven:3.9.9-eclipse-temurin-17'
            args '-v $HOME/.m2:/root/.m2 -u root'
            reuseNode true
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
        GITHUB_TOKEN = credentials('github-token')
        DEPLOY_PPROD_SERVER = credentials('deploy-pprod-server') 
        DEPLOY_PROD_SERVER = credentials('deploy-prod-server') 
        APP_CODE_PATH = '/apps/java/src'
        APP_DEPLOY_PATH = '/apps/java/deploy'
    }

    stages {
        stage('Maven Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Unit Tests') {
            steps {
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
                script {
                    docker.withRegistry("https://${DOCKER_REGISTRY}", 'github-token') {
                        def customImage = docker.build("${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}:${DOCKER_TAG}")
                        customImage.push()
                    }
                }
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
            cleanWs()
        }
        failure {
            echo 'Pipeline failed!'
        }
        success {
            echo 'Pipeline completed successfully!'
        }
    }
}