pipeline {
    agent any

    environment {
        APP_NAME = 'javatheque'
        DOCKER_IMAGE = 'javatheque-env'
        DOCKER_TAG = 'latest'
        DOCKER_REGISTRY = 'ghcr.io'
        GITHUB_OWNER = 'corin-alt'

        DEPLOY_SERVER = credentials('deploy-pprod-serv') 
        APP_CODE_PATH = '/apps/java/src'
        APP_DEPLOY_PATH = '/apps/java/deploy'
        
        DOCKERFILE_CHANGED = 'false'
        
        GLASSFISH_HOME = '/opt/glassfish7'
        CHROME_OPTIONS = '--headless --no-sandbox --disable-dev-shm-usage'
        DB_URL = credentials('db_url')
        DB_USER = credentials('db_user')
        DB_PASSWORD = credentials('db_password')
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
                    def changes = changeset 'Dockerfile'
                    env.DOCKERFILE_CHANGED = changes.toString()
                }
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('Unit testing') {
            when {
                expression {
                    return currentBuild.resultIsBetterOrEqualTo('SUCCESS')
                }
            }
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Build and Push Docker Image') {
            when {
                allOf {
                    branch 'main'
                    expression {
                        return currentBuild.resultIsBetterOrEqualTo('SUCCESS')
                    }
                    expression {
                        return env.DOCKERFILE_CHANGED == 'true'
                    }
                }
            }
            steps {
                script {
                    def imageFullName = "${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}"
                    
                    docker.build("${imageFullName}:${DOCKER_TAG}")
                    
                    withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
                        sh """
                            echo $GITHUB_TOKEN | docker login ghcr.io -u ${GITHUB_OWNER} --password-stdin
                            docker push ${imageFullName}:${DOCKER_TAG}
                            docker logout ghcr.io
                        """
                    }
                }
            }
        }
        
        stage('Deploy to Pre-Production server') {
            when {
                allOf {
                    branch 'main'
                    expression {
                        def unitTestingSuccess = currentBuild.resultIsBetterOrEqualTo('SUCCESS')
                        def dockerBuildSuccess = env.DOCKERFILE_CHANGED == 'false' || 
                            (env.DOCKERFILE_CHANGED == 'true' && currentBuild.resultIsBetterOrEqualTo('SUCCESS'))
                        return unitTestingSuccess && dockerBuildSuccess
                    }
                }
            }
            steps {
                sshagent(['deploy-key']) {
                    sh """
                        ssh ${DEPLOY_SERVER} 'mkdir -p ${APP_CODE_PATH} ${APP_DEPLOY_PATH}'
                        rsync -av --delete ./ ${DEPLOY_SERVER}:${APP_CODE_PATH}/
                        scp target/${APP_NAME}.war ${DEPLOY_SERVER}:${APP_DEPLOY_PATH}/
                        
                        ssh ${DEPLOY_SERVER} "cat > ${APP_CODE_PATH}/.env << EOL
                        DOCKER_REGISTRY=${DOCKER_REGISTRY}
                        GITHUB_OWNER=${GITHUB_OWNER}
                        DOCKER_IMAGE=${DOCKER_IMAGE}
                        DOCKER_TAG=${DOCKER_TAG}
                        APP_DEPLOY_PATH=${APP_DEPLOY_PATH}
                        EOL"
                    """
                    
                    withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
                        sh """
                            ssh ${DEPLOY_SERVER} '
                                cd ${APP_CODE_PATH}
                                echo ${GITHUB_TOKEN} | docker login ghcr.io -u ${GITHUB_OWNER} --password-stdin
                                docker pull ${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}:${DOCKER_TAG}
                                docker-compose down
                                docker-compose up -d
                                docker logout ghcr.io
                            '
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