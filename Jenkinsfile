pipeline {
    agent any

    environment {
        APP_NAME = 'javatheque'
        DOCKER_IMAGE = 'javatheque-env'
        DOCKER_TAG = 'latest'
        DOCKER_REGISTRY = 'ghcr.io'
        GITHUB_OWNER = 'corin-alt'

        DEPLOY_PP_SERVER = credentials('deploy-pprod-serv') 
        APP_CODE_PATH = '/apps/java/src'
        APP_DEPLOY_PATH = '/apps/java/deploy'
        
        GLASSFISH_HOME = '/opt/glassfish7'
    }
    
    tools {
        maven 'Maven'
        dockerTool 'Docker'
    }
    
    stages {
        
        stage('Checkout & Build') {
            steps {
                script {
                    checkout scm
                    def currentBranch = sh(script: 'git name-rev --name-only HEAD', returnStdout: true).trim()
                    echo "Current branch: ${currentBranch}"
                    env.CURRENT_BRANCH = currentBranch
                }
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('Unit testing') {
            steps {
                sh 'mvn test -Dtest=**/*UnitTest'
            }
        }
        
        stage('Build and Push Docker Image') {
            steps {
                script {
                    echo "Building Docker image for branch: ${env.CURRENT_BRANCH}"
                    def imageFullName = "${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}"
                    
                    docker.build("${imageFullName}:${DOCKER_TAG}")
                    
                    withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
                        sh """
                            docker login ghcr.io -u ${GITHUB_OWNER} -p "${GITHUB_TOKEN}"
                            docker push ${imageFullName}:${DOCKER_TAG}
                            docker logout ghcr.io
                        """
                    }
                }
            }
        }
        
        stage('Deploy to Pre-Production server') {
            steps {
                sshagent(['deploy-key']) {
                    sh """
                        ssh ${DEPLOY_PP_SERVER} 'mkdir -p ${APP_CODE_PATH} ${APP_DEPLOY_PATH}'
                        rsync -av --delete ./ ${DEPLOY_PP_SERVER}:${APP_CODE_PATH}/
                        scp target/${APP_NAME}.war ${DEPLOY_PP_SERVER}:${APP_DEPLOY_PATH}/
                        
                        ssh ${DEPLOY_PP_SERVER} "cat > ${APP_CODE_PATH}/.env << EOL
                        DOCKER_REGISTRY=${DOCKER_REGISTRY}
                        GITHUB_OWNER=${GITHUB_OWNER}
                        DOCKER_IMAGE=${DOCKER_IMAGE}
                        DOCKER_TAG=${DOCKER_TAG}
                        APP_DEPLOY_PATH=${APP_DEPLOY_PATH}
                        EOL"
                    """
                    
                    withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
                        sh """
                            ssh ${DEPLOY_PP_SERVER} "
                                cd ${APP_CODE_PATH}
                                docker login ghcr.io -u ${GITHUB_OWNER} -p '${GITHUB_TOKEN}'
                                docker pull ${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}:${DOCKER_TAG}
                                docker-compose down
                                docker-compose up -d
                                docker logout ghcr.io
                            "
                        """
                    }
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo 'Pipeline successfully executed!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}