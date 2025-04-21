pipeline {
    // Use any available agent/node for executing the pipeline
    agent any

    options {
        // Keep only the last 2 builds' history
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '2')
    }

    environment {
        // Docker image configuration settings
        APP_NAME = 'javatheque'
        DOCKER_IMAGE = 'javatheque-env'
        DOCKER_TAG = 'latest'
        DOCKER_REGISTRY = 'ghcr.io'  // GitHub Container Registry
        GITHUB_OWNER = 'corin-alt'

        // Remote deployment server configuration
        DEPLOY_SERVER = credentials('deploy-pprod-serv')    
        APP_CODE_PATH = '/apps/java/src'  // Path for application source code
        APP_DEPLOY_PATH = '/apps/java/deploy'  // Path for deployment artifacts
        
        // Flag to track if Dockerfile has been modified
        DOCKERFILE_CHANGED = 'false'
    }
    
    // Required tools for the pipeline
    tools {
        maven 'Maven'  // Maven for Java build
        docker 'Docker'  // Docker for containerization
    }
    
    stages {
        // Stage 1: Checkout code and build the application
        stage('Checkout & Build') {
            steps {
                script {
                    // Checkout code from SCM (Source Control Management)
                    checkout scm
                    // Check if Dockerfile has been modified
                    def changes = changeset 'Dockerfile'
                    env.DOCKERFILE_CHANGED = changes.toString()
                }
                // Build the Java application, skipping tests at this stage
                sh 'mvn clean package -DskipTests'
            }
        }
        
        // Stage 2: Run unit tests
        stage('Unit testing') {
            when {
                expression {
                    // Only proceed if previous stage was successful
                    return currentBuild.resultIsBetterOrEqualTo('SUCCESS')
                }
            }
            steps {
                // Execute Maven tests
                sh 'mvn test'
            }
            post {
                always {
                    // Publish JUnit test results
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        
        // Stage 3: Build and push Docker image
        stage('Build and Push Docker Image') {
            when {
                allOf {
                    branch 'main'  // Only on main branch
                    expression {
                        // Only if previous stages were successful
                        return currentBuild.resultIsBetterOrEqualTo('SUCCESS')
                    }
                    expression {
                        // Only if Dockerfile was modified
                        return env.DOCKERFILE_CHANGED == 'true'
                    }
                }
            }
            steps {
                script {
                    // Construct full Docker image name
                    def imageFullName = "${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}"
                    
                    // Build Docker image
                    docker.build("${imageFullName}:${DOCKER_TAG}")
                    
                    // Login to GitHub Container Registry and push image
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
        
        // Stage 4: Deploy the application
        stage('Deploy') {
            when {
                allOf {
                    branch 'main'  // Only deploy from main branch
                    expression {
                        // Check if unit tests passed
                        def unitTestingSuccess = currentBuild.resultIsBetterOrEqualTo('SUCCESS')
                        
                        // Check Docker build status
                        def dockerBuildSuccess = env.DOCKERFILE_CHANGED == 'false' || 
                            (env.DOCKERFILE_CHANGED == 'true' && currentBuild.resultIsBetterOrEqualTo('SUCCESS'))
                        
                        return unitTestingSuccess && dockerBuildSuccess
                    }
                }
            }
            steps {
                // Use SSH agent for secure deployment
                sshagent(['deploy-pprod-key']) {
                    // Create necessary directories and sync code
                    sh """
                        ssh ${DEPLOY_SERVER} 'mkdir -p ${APP_CODE_PATH} ${APP_DEPLOY_PATH}'
                        rsync -av --delete ./ ${DEPLOY_SERVER}:${APP_CODE_PATH}/
                        scp target/${APP_NAME}.war ${DEPLOY_SERVER}:${APP_DEPLOY_PATH}/
                    """
                    
                    // Deploy using Docker
                    withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
                        sh """
                            ssh ${DEPLOY_SERVER} '
                                # Login to GitHub Container Registry
                                echo ${GITHUB_TOKEN} | docker login ghcr.io -u ${GITHUB_OWNER} --password-stdin
                                
                                # Pull latest Docker image
                                docker pull ${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}:${DOCKER_TAG}
                                
                                # Stop and remove existing container
                                docker stop javatheque || true
                                docker rm javatheque || true
                                
                                # Run new container with appropriate port mappings and volume mounts
                                docker run -d --name javatheque \
                                    -p 8080:8080 -p 4848:4848 \
                                    -v ${APP_DEPLOY_PATH}:/opt/glassfish7/glassfish/domains/domain1/autodeploy \
                                    ${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}:${DOCKER_TAG}
                                
                                # Logout from registry
                                docker logout ghcr.io
                            '
                        """
                    }
                }
            }
        }
    }
    
    // Post-build actions
    post {
        always {
            // Clean workspace after build
            cleanWs()
        }
        success {
            // Notification on successful build
            echo 'Pipeline successfully executed!'
        }
        failure {
            // Notification on failed build
            echo 'The pipeline failed!'
        }
    }
}