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
       
        GLASSFISH_ADMIN_PASSWORD = credentials('glassfish-admin-password')

        GITHUB_OWNER = 'corin-alt'
        GITHUB_TOKEN = credentials('github-token')
        
        DEPLOY_PPROD_SERVER = credentials('deploy-pprod-server')
        DEPLOY_PROD_SERVER = credentials('deploy-prod-server')
        
        APP_PATH = 'javatheque'
        APP_DEPLOY_PATH= './target'

        SUDO_PASSWORD = credentials('sudo-password')
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
                archiveArtifacts artifacts: 'target/*.war', fingerprint: true
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
                    args '--privileged -v /var/run/docker.sock:/var/run/docker.sock -u root'
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
                withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
                    script {
                        sh '''
                        mkdir -p /root/.docker
                        docker build --build-arg ADMIN_PASSWORD=${GLASSFISH_ADMIN_PASSWORD} \
                                -t ${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}:${DOCKER_TAG} .
                        echo $GITHUB_TOKEN | docker login ${DOCKER_REGISTRY} -u ${GITHUB_OWNER} --password-stdin
                        docker push ${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}:${DOCKER_TAG}
                        docker logout ${DOCKER_REGISTRY}
                        '''
                    }
                }
            }
        }

        stage('Deploy to Pre-production') {
            agent {
                docker {
                    image 'ubuntu:latest'
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
                sh '''
                    apt-get update && apt-get install -y openssh-client rsync
                    mkdir -p target
                    chmod -R 777 target
                '''
                copyArtifacts filter: 'target/*.war', fingerprintArtifacts: true, projectName: '${JOB_NAME}', selector: specific('${BUILD_NUMBER}')
                sh 'chmod -R 777 target'
                sshagent(credentials: ['jenkins-ssh-private-key']) {
                    sh '''
                    mkdir -p ~/.ssh
                    chmod 700 ~/.ssh
                    TARGET_IP=$(echo $DEPLOY_PPROD_SERVER | cut -d'@' -f2)

                    ssh-keyscan -H $TARGET_IP >> ~/.ssh/known_hosts
                    chmod 644 ~/.ssh/known_hosts

                    rsync -av --delete ./ ${DEPLOY_PPROD_SERVER}:${APP_PATH}/
                    scp target/${APP_NAME}.war ${DEPLOY_PPROD_SERVER}:${APP_PATH}/target

                    ssh ${DEPLOY_PPROD_SERVER} "printf 'DOCKER_REGISTRY=%s\nGITHUB_OWNER=%s\nDOCKER_IMAGE=%s\nDOCKER_TAG=%s\nAPP_DEPLOY_PATH=%s' '${DOCKER_REGISTRY}' '${GITHUB_OWNER}' '${DOCKER_IMAGE}' '${DOCKER_TAG}' '${APP_DEPLOY_PATH}' > ${APP_PATH}/.env"

                    ssh ${DEPLOY_PPROD_SERVER} "cd ${APP_PATH} && \
                        echo ${GITHUB_TOKEN} | docker login ghcr.io -u ${GITHUB_OWNER} --password-stdin || exit 1 && \
                        docker pull ${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}:${DOCKER_TAG} || true && \
                        docker compose down || echo 'Warning: docker compose down failed but continuing' && \
                        docker compose up -d || exit 1 && \
                        docker logout ghcr.io"
                    '''
                }
            }
        }

        stage('Deploy to Production') {
            agent {
                docker {
                    image 'ubuntu:latest'
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
                sh '''
                    apt-get update && apt-get install -y openssh-client rsync
                    mkdir -p target
                    chmod -R 777 target
                '''
                copyArtifacts filter: 'target/*.war', fingerprintArtifacts: true, projectName: '${JOB_NAME}', selector: specific('${BUILD_NUMBER}')
                sh 'chmod -R 777 target'
                sshagent(credentials: ['jenkins-ssh-private-key']) {
                    sh '''
                    mkdir -p ~/.ssh
                    chmod 700 ~/.ssh
                    TARGET_IP=$(echo $DEPLOY_PPROD_SERVER | cut -d'@' -f2)

                    ssh-keyscan -H $TARGET_IP >> ~/.ssh/known_hosts
                    chmod 644 ~/.ssh/known_hosts

                    rsync -av --delete ./ ${DEPLOY_PPROD_SERVER}:${APP_PATH}/
                    scp target/${APP_NAME}.war ${DEPLOY_PPROD_SERVER}:${APP_PATH}/target

                    ssh ${DEPLOY_PPROD_SERVER} "printf 'DOCKER_REGISTRY=%s\nGITHUB_OWNER=%s\nDOCKER_IMAGE=%s\nDOCKER_TAG=%s\nAPP_DEPLOY_PATH=%s' '${DOCKER_REGISTRY}' '${GITHUB_OWNER}' '${DOCKER_IMAGE}' '${DOCKER_TAG}' '${APP_DEPLOY_PATH}' > ${APP_PATH}/.env"

                    ssh ${DEPLOY_PPROD_SERVER} "cd ${APP_PATH} && \
                        echo ${GITHUB_TOKEN} | docker login ghcr.io -u ${GITHUB_OWNER} --password-stdin || exit 1 && \
                        docker pull ${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}:${DOCKER_TAG} || true && \
                        docker compose down || echo 'Warning: docker compose down failed but continuing' && \
                        docker compose up -d || exit 1 && \
                        docker logout ghcr.io"
                    '''
                }
            }
        }
    }

    post {
        always {
            script {
                node('built-in') {
                    cleanWs()
                }
            }
        }
        failure {
            script {
                node('built-in') {
                    echo 'Pipeline failed!'
                }
            }
        }
        success {
            script {
                node('built-in') {
                    echo 'Pipeline completed successfully!'
                }
            }
        }
    }
}