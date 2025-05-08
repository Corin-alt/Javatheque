pipeline {
    agent {
        docker {
            image 'maven:3.9-eclipse-temurin-17'
            args '-v /var/run/docker.sock:/var/run/docker.sock'
        }
    }

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
        jdk 'JDK17'
    }
    
    stages {
        stage('Setup Environment') {
            steps {
                script {
                    sh '''
                        # Installation de Chrome et ChromeDriver
                        wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add -
                        echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google.list
                        apt-get update
                        apt-get install -y google-chrome-stable
                        
                        CHROME_VERSION=$(google-chrome --version | awk '{ print $3 }' | cut -d'.' -f1)
                        wget -N "https://chromedriver.storage.googleapis.com/LATEST_RELEASE_${CHROME_VERSION}"
                        wget -N "https://chromedriver.storage.googleapis.com/$(cat LATEST_RELEASE_${CHROME_VERSION})/chromedriver_linux64.zip"
                        unzip chromedriver_linux64.zip
                        mv chromedriver /usr/local/bin/
                        chmod +x /usr/local/bin/chromedriver
                        
                        # Installation de GlassFish 7
                        wget https://download.eclipse.org/ee4j/glassfish/glassfish-7.0.0.zip
                        unzip glassfish-7.0.0.zip -d /opt/
                        chmod -R +x ${GLASSFISH_HOME}/bin
                    '''
                }
            }
        }

        stage('Checkout & Build') {
            steps {
                script {
                    checkout scm
                    def changes = changeset 'Dockerfile'
                    env.DOCKERFILE_CHANGED = changes.toString()
                }
        
                sh '''
                    ${GLASSFISH_HOME}/bin/asadmin start-domain domain1
                    # Configuration de la ressource MongoDB dans GlassFish
                    ${GLASSFISH_HOME}/bin/asadmin create-custom-resource \
                        --restype=java.lang.String \
                        --factoryclass=org.glassfish.resources.custom.factory.PrimitivesAndStringFactory \
                        --property value=${DB_URL} \
                        mongodb/url
                    ${GLASSFISH_HOME}/bin/asadmin create-custom-resource \
                        --restype=java.lang.String \
                        --factoryclass=org.glassfish.resources.custom.factory.PrimitivesAndStringFactory \
                        --property value=${DB_USER} \
                        mongodb/user
                    ${GLASSFISH_HOME}/bin/asadmin create-custom-resource \
                        --restype=java.lang.String \
                        --factoryclass=org.glassfish.resources.custom.factory.PrimitivesAndStringFactory \
                        --property value=${DB_PASSWORD} \
                        mongodb/password
                '''
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
                sh 'mvn clean test -Dbrowser=chrome -DbaseUrl=http://localhost:8080/javatheque -Dheadless=true'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Integration Testing') {
            when {
                expression {
                    return currentBuild.resultIsBetterOrEqualTo('SUCCESS')
                }
            }
            steps {
                sh """
                    mvn verify -Dwebdriver.chrome.driver=/usr/local/bin/chromedriver \
                        -Dselenium.chrome.options="${CHROME_OPTIONS}" \
                        -Djakarta.persistence.jdbc.url=${DB_URL} \
                        -Djakarta.persistence.jdbc.user=${DB_USER} \
                        -Djakarta.persistence.jdbc.password=${DB_PASSWORD} \
                        -Pfailsafe
                """
            }
            post {
                always {
                    junit '**/target/failsafe-reports/*.xml'
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
                sshagent(['deploy-pprod-key']) {
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
            sh '${GLASSFISH_HOME}/bin/asadmin stop-domain domain1 || true'
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