pipeline {
    agent {
        docker {
            image 'ubuntu:latest'
            args '-u root'
        }
    }

    triggers {
        githubPush()
    }

    tools {
        maven 'Maven'
        dockerTool 'Docker'
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
                sh '''
                    apt-get update
                '''
            }
        }

        stage('Maven Build') {
            when { 
                expression { currentBuild.previousBuild?.result == 'SUCCESS' }
            }
            steps {
                echo 'Maven Build...'
            }
        }

        stage('Unit Tests') {
            when {
                expression { currentBuild.previousBuild?.result == 'SUCCESS' }
            }
            steps {
                echo 'Unit Tests...'
            }
        }

        stage('Build Docker Image') {
            when {
                allOf {
                    expression { currentBuild.previousBuild?.result == 'SUCCESS' }
                    anyOf {
                        branch 'main'
                        branch 'dev'
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
                    expression { currentBuild.previousBuild?.result == 'SUCCESS' }
                    branch 'dev'
                }
            }
            steps {
                echo 'Deploy to Pre-production...'
            }
        }

        stage('Deploy to Production') {
            when {
                allOf {
                    expression { currentBuild.previousBuild?.result == 'SUCCESS' }
                    branch 'main'
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
