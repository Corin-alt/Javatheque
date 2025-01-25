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
                        def dockerfileChanged = sh(script: 'git diff --name-only HEAD^ HEAD | grep "Dockerfile"', returnStatus: true) == 0

                        if (dockerfileChanged) {
                            sh '''
                            mkdir -p /root/.docker
                            docker version
                            docker build --build-arg ADMIN_PASSWORD=${GLASSFISH_ADMIN_PASSWORD} \
                                    -t ${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}:${DOCKER_TAG} .
                            echo $GITHUB_TOKEN | docker login ${DOCKER_REGISTRY} -u ${GITHUB_OWNER} --password-stdin
                            docker push ${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}:${DOCKER_TAG}
                            docker logout ${DOCKER_REGISTRY}
                            '''
                        } else {
                            echo 'Dockerfile unchanged, build skip'
                        }
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
                sshagent(credentials: ['deploy-key']) {
                    sh '''
                    ssh $DEPLOY_PPROD_SERVER "touch /path/to/fromjenkins.txt"
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
                echo 'Deploy to Production...'
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