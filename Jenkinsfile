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
        
        APP_CODE_PATH = '/apps/javatheque/sourcecode'
        APP_DEPLOY_PATH = '/apps/javatheque/deploy'

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
                    apt-get update && apt-get install -y openssh-client
                '''
                sshagent(credentials: ['jenkins-ssh-private-key']) {
                    sh '''
                        mkdir -p ~/.ssh
                        chmod 700 ~/.ssh
                        TARGET_IP=$(echo $DEPLOY_PPROD_SERVER | cut -d'@' -f2)
                        ssh-keyscan -H $TARGET_IP >> ~/.ssh/known_hosts
                        chmod 644 ~/.ssh/known_hosts

                        echo "${SUDO_PASSWORD}" | ssh ${DEPLOY_PPROD_SERVER} "sudo -S mkdir -p /apps && \
                            sudo -S chown -R \$(whoami):\$(whoami) /apps && \
                            mkdir -p \"${APP_CODE_PATH}\" && \
                            mkdir -p \"${APP_DEPLOY_PATH}\" && \
                            chmod 755 \"${APP_CODE_PATH}\" && \
                            chmod 755 \"${APP_DEPLOY_PATH}\"
                        "
                        
                        rsync -av --delete ./ ${DEPLOY_PPROD_SERVER}:${APP_CODE_PATH}/
                        scp target/${APP_NAME}.war ${DEPLOY_PPROD_SERVER}:${APP_DEPLOY_PATH}/
                        
                        ssh ${DEPLOY_PPROD_SERVER} "cat > \"${APP_CODE_PATH}\"/.env << EOL
                        DOCKER_REGISTRY=\"${DOCKER_REGISTRY}\"
                        GITHUB_OWNER=\"${GITHUB_OWNER}\"
                        DOCKER_IMAGE=\"${DOCKER_IMAGE}\"
                        DOCKER_TAG=\"${DOCKER_TAG}\"
                        APP_DEPLOY_PATH=\"${APP_DEPLOY_PATH}\"
                        EOL"
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
                    apt-get update && apt-get install -y openssh-client
                '''
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